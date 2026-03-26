package com.financetracker.service;

import com.financetracker.dto.BudgetDtos;
import com.financetracker.dto.GoalDtos;
import com.financetracker.dto.RecurringDtos;
import com.financetracker.dto.ReportDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final com.financetracker.repository.TransactionRepository transactionRepository;
    private final com.financetracker.repository.AccountRepository accountRepository;
    private final MapperService mapperService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final RecurringTransactionService recurringTransactionService;
    private final AccessControlService accessControlService;

    @Transactional(readOnly = true)
    public ReportDtos.DashboardResponse getDashboard() {
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        BigDecimal[] monthlySummary = summary(accountIds, start, end);
        BigDecimal totalBalance = getCurrentBalance(accountIds);
        List<BudgetDtos.BudgetResponse> budgets = budgetService.getBudgets(now.getMonthValue(), now.getYear());
        List<GoalDtos.GoalResponse> goals = goalService.getGoals();
        List<RecurringDtos.RecurringResponse> recurring = recurringTransactionService.getUpcomingRecurringForAccounts(accountIds, 5);
        List<com.financetracker.dto.TransactionDtos.TransactionResponse> recent = accountIds.isEmpty()
                ? List.of()
                : transactionRepository.findRecentByAccountIds(accountIds, PageRequest.of(0, 5)).getContent().stream().map(mapperService::toTransactionResponse).toList();

        return new ReportDtos.DashboardResponse(
                new ReportDtos.SummaryCards(monthlySummary[0], monthlySummary[1], totalBalance),
                getCategorySpend(start, end),
                getIncomeVsExpense(start.minusMonths(5), end),
                recent,
                recurring,
                goals,
                budgets
        );
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.CategorySpendPoint> getCategorySpend(LocalDate startDate, LocalDate endDate) {
        List<FinanceTransaction> transactions = getTransactions(startDate, endDate);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (FinanceTransaction transaction : transactions) {
            if (transaction.getType() == TransactionType.EXPENSE && transaction.getCategory() != null) {
                totals.merge(transaction.getCategory().getName(), transaction.getAmount(), BigDecimal::add);
            }
        }
        return totals.entrySet().stream()
                .sorted((left, right) -> right.getValue().compareTo(left.getValue()))
                .map(entry -> new ReportDtos.CategorySpendPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.TrendPoint> getIncomeVsExpense(LocalDate startDate, LocalDate endDate) {
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        Map<YearMonth, BigDecimal[]> grouped = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        while (!cursor.isAfter(end)) {
            grouped.put(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cursor = cursor.plusMonths(1);
        }
        for (FinanceTransaction transaction : getTransactions(startDate, endDate)) {
            YearMonth ym = YearMonth.from(transaction.getTransactionDate());
            BigDecimal[] totals = grouped.computeIfAbsent(ym, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (transaction.getType() == TransactionType.INCOME) {
                totals[0] = totals[0].add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                totals[1] = totals[1].add(transaction.getAmount());
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        return grouped.entrySet().stream()
                .map(entry -> new ReportDtos.TrendPoint(entry.getKey().atDay(1).format(formatter), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.AccountBalancePoint> getAccountBalanceTrend() {
        return getAccessibleAccounts().stream()
                .map(account -> new ReportDtos.AccountBalancePoint(account.getName(), account.getCurrentBalance()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportDtos.NetWorthResponse getNetWorthReport() {
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        BigDecimal currentNetWorth = getCurrentBalance(accountIds);
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.minusMonths(5).atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();
        Map<YearMonth, BigDecimal> monthlyNet = new LinkedHashMap<>();
        for (int index = 5; index >= 0; index--) {
            monthlyNet.put(currentMonth.minusMonths(index), BigDecimal.ZERO);
        }
        for (FinanceTransaction transaction : getTransactions(start, end)) {
            YearMonth month = YearMonth.from(transaction.getTransactionDate());
            BigDecimal delta = transaction.getType() == TransactionType.INCOME
                    ? transaction.getAmount()
                    : transaction.getType() == TransactionType.EXPENSE ? transaction.getAmount().negate() : BigDecimal.ZERO;
            monthlyNet.merge(month, delta, BigDecimal::add);
        }

        List<YearMonth> months = new ArrayList<>(monthlyNet.keySet());
        Map<YearMonth, BigDecimal> snapshot = new LinkedHashMap<>();
        BigDecimal running = currentNetWorth;
        snapshot.put(months.get(months.size() - 1), running);
        for (int index = months.size() - 2; index >= 0; index--) {
            YearMonth nextMonth = months.get(index + 1);
            YearMonth month = months.get(index);
            running = running.subtract(monthlyNet.getOrDefault(nextMonth, BigDecimal.ZERO));
            snapshot.put(month, running);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        List<ReportDtos.NetWorthPoint> history = months.stream()
                .map(month -> new ReportDtos.NetWorthPoint(month.atDay(1).format(formatter), snapshot.getOrDefault(month, BigDecimal.ZERO)))
                .toList();
        return new ReportDtos.NetWorthResponse(currentNetWorth, history);
    }

    public ReportDtos.FilteredReportResponse getFilteredReport(LocalDate startDate, LocalDate endDate) {
        return new ReportDtos.FilteredReportResponse(
                startDate,
                endDate,
                getCategorySpend(startDate, endDate),
                getIncomeVsExpense(startDate, endDate),
                getAccountBalanceTrend(),
                getNetWorthReport()
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(LocalDate startDate, LocalDate endDate) {
        List<FinanceTransaction> transactions = getTransactions(startDate, endDate);
        StringBuilder builder = new StringBuilder("date,type,account,category,merchant,amount,note\n");
        for (FinanceTransaction tx : transactions) {
            builder.append(tx.getTransactionDate()).append(',')
                    .append(tx.getType()).append(',')
                    .append(tx.getAccount().getName()).append(',')
                    .append(tx.getCategory() == null ? "" : tx.getCategory().getName()).append(',')
                    .append(tx.getMerchant() == null ? "" : tx.getMerchant()).append(',')
                    .append(tx.getAmount()).append(',')
                    .append(tx.getNote() == null ? "" : tx.getNote().replace(',', ';'))
                    .append("\n");
        }
        return builder.toString();
    }

    private BigDecimal[] summary(Set<UUID> accountIds, LocalDate start, LocalDate end) {
        List<FinanceTransaction> transactions = accountIds.isEmpty() ? List.of() : transactionRepository.findByAccountIdInAndTransactionDateBetween(accountIds, start, end);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinanceTransaction tx : transactions) {
            if (tx.getType() == TransactionType.INCOME) {
                income = income.add(tx.getAmount());
            } else if (tx.getType() == TransactionType.EXPENSE) {
                expense = expense.add(tx.getAmount());
            }
        }
        return new BigDecimal[]{income, expense};
    }

    private BigDecimal getCurrentBalance(Set<UUID> accountIds) {
        return getAccessibleAccounts().stream()
                .map(account -> account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Account> getAccessibleAccounts() {
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return accountRepository.findByIdInOrderByCreatedAtDesc(accountIds);
    }

    private List<FinanceTransaction> getTransactions(LocalDate startDate, LocalDate endDate) {
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdInAndTransactionDateBetween(accountIds, startDate, endDate);
    }
}

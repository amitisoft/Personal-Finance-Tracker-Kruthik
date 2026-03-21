package com.financetracker.service;

import com.financetracker.dto.BudgetDtos;
import com.financetracker.dto.GoalDtos;
import com.financetracker.dto.RecurringDtos;
import com.financetracker.dto.ReportDtos;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.repository.AccountRepository;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final MapperService mapperService;
    private final UserContextService userContextService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final RecurringTransactionService recurringTransactionService;

    @Transactional(readOnly = true)
    public ReportDtos.DashboardResponse getDashboard() {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        BigDecimal[] monthlySummary = summary(userId, start, end);
        BigDecimal totalBalance = accountRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(account -> account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BudgetDtos.BudgetResponse> budgets = budgetService.getBudgets(now.getMonthValue(), now.getYear());
        List<GoalDtos.GoalResponse> goals = goalService.getGoals();
        List<RecurringDtos.RecurringResponse> recurring = recurringTransactionService.getUpcomingRecurringForUser(userId, 5);
        List<com.financetracker.dto.TransactionDtos.TransactionResponse> recent = transactionRepository
                .findRecentByUserId(userId, PageRequest.of(0, 5))
                .getContent().stream().map(mapperService::toTransactionResponse).toList();

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

    public List<ReportDtos.CategorySpendPoint> getCategorySpend(LocalDate startDate, LocalDate endDate) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return transactionRepository.getCategorySpend(userId, startDate, endDate).stream()
                .map(row -> new ReportDtos.CategorySpendPoint((String) row[0], (BigDecimal) row[1]))
                .toList();
    }

    public List<ReportDtos.TrendPoint> getIncomeVsExpense(LocalDate startDate, LocalDate endDate) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        Map<YearMonth, BigDecimal[]> grouped = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        while (!cursor.isAfter(end)) {
            grouped.put(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cursor = cursor.plusMonths(1);
        }
        for (FinanceTransaction transaction : transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate)) {
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

    public List<ReportDtos.AccountBalancePoint> getAccountBalanceTrend() {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return accountRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(account -> new ReportDtos.AccountBalancePoint(account.getName(), account.getCurrentBalance()))
                .toList();
    }

    public ReportDtos.FilteredReportResponse getFilteredReport(LocalDate startDate, LocalDate endDate) {
        return new ReportDtos.FilteredReportResponse(
                startDate,
                endDate,
                getCategorySpend(startDate, endDate),
                getIncomeVsExpense(startDate, endDate),
                getAccountBalanceTrend()
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(LocalDate startDate, LocalDate endDate) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        List<FinanceTransaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate);
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

    private BigDecimal[] summary(UUID userId, LocalDate start, LocalDate end) {
        List<FinanceTransaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
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
}

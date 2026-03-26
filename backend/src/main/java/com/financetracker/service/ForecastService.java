package com.financetracker.service;

import com.financetracker.dto.ForecastDtos;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.repository.AccountRepository;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccessControlService accessControlService;

    @Transactional(readOnly = true)
    public ForecastDtos.MonthForecastResponse getMonthForecast() {
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        int daysElapsed = today.getDayOfMonth();
        int daysRemaining = Math.max(0, monthEnd.getDayOfMonth() - today.getDayOfMonth());

        BigDecimal currentBalance = getCurrentBalance(accountIds);
        BigDecimal[] currentMonthTotals = summarizeTransactions(getTransactions(accountIds, monthStart, today));
        HistoricalDailyAverages averages = getHistoricalAverages(accountIds, monthStart, today, daysElapsed);
        BigDecimal projectedFutureIncome = scale(averages.averageIncomePerDay.multiply(BigDecimal.valueOf(daysRemaining)));
        BigDecimal projectedFutureExpense = scale(averages.averageExpensePerDay.multiply(BigDecimal.valueOf(daysRemaining)));
        BigDecimal projectedEndBalance = scale(currentBalance.add(projectedFutureIncome).subtract(projectedFutureExpense));
        BigDecimal projectedIncome = scale(currentMonthTotals[0].add(projectedFutureIncome));
        BigDecimal projectedExpense = scale(currentMonthTotals[1].add(projectedFutureExpense));
        BigDecimal projectedNet = scale(projectedIncome.subtract(projectedExpense));
        BigDecimal emergencyBuffer = scale(averages.averageExpensePerDay.multiply(BigDecimal.valueOf(7)));
        BigDecimal safeToSpend = scale(projectedEndBalance.subtract(emergencyBuffer).max(BigDecimal.ZERO));
        String riskLevel = projectedEndBalance.compareTo(BigDecimal.ZERO) < 0 ? "HIGH" : projectedEndBalance.compareTo(emergencyBuffer) < 0 ? "MEDIUM" : "LOW";
        String warning = projectedEndBalance.compareTo(BigDecimal.ZERO) < 0
                ? "Projected balance may turn negative before month end."
                : projectedEndBalance.compareTo(emergencyBuffer) < 0
                    ? "Projected balance is positive but leaves a thin cash buffer."
                    : "Projection indicates a stable month-end balance.";

        return new ForecastDtos.MonthForecastResponse(
                scale(currentBalance),
                projectedEndBalance,
                projectedIncome,
                projectedExpense,
                projectedNet,
                safeToSpend,
                daysRemaining,
                averages.confidence,
                riskLevel,
                warning
        );
    }

    @Transactional(readOnly = true)
    public ForecastDtos.DailyForecastResponse getDailyForecast() {
        ForecastDtos.MonthForecastResponse monthForecast = getMonthForecast();
        Set<UUID> accountIds = accessControlService.getAccessibleAccountIds();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        YearMonth currentMonth = YearMonth.from(today);
        HistoricalDailyAverages averages = getHistoricalAverages(accountIds, monthStart, today, today.getDayOfMonth());
        BigDecimal dailyNet = scale(averages.averageIncomePerDay.subtract(averages.averageExpensePerDay));

        List<ForecastDtos.DailyProjectionPoint> points = new ArrayList<>();
        BigDecimal runningBalance = scale(monthForecast.currentBalance());
        points.add(new ForecastDtos.DailyProjectionPoint(today, runningBalance));
        for (LocalDate date = today.plusDays(1); !date.isAfter(currentMonth.atEndOfMonth()); date = date.plusDays(1)) {
            runningBalance = scale(runningBalance.add(dailyNet));
            points.add(new ForecastDtos.DailyProjectionPoint(date, runningBalance));
        }

        return new ForecastDtos.DailyForecastResponse(
                monthForecast.currentBalance(),
                monthForecast.projectedEndBalance(),
                points
        );
    }

    private HistoricalDailyAverages getHistoricalAverages(Set<UUID> accountIds, LocalDate monthStart, LocalDate today, int daysElapsed) {
        LocalDate historicalStart = monthStart.minusMonths(6);
        LocalDate historicalEnd = monthStart.minusDays(1);
        List<FinanceTransaction> historyTransactions = getTransactions(accountIds, historicalStart, historicalEnd);

        if (!historyTransactions.isEmpty()) {
            BigDecimal[] historySummary = summarizeTransactions(historyTransactions);
            long historyDays = Math.max(1, historicalEnd.toEpochDay() - historicalStart.toEpochDay() + 1);
            return new HistoricalDailyAverages(
                    scale(historySummary[0].divide(BigDecimal.valueOf(historyDays), 6, RoundingMode.HALF_UP)),
                    scale(historySummary[1].divide(BigDecimal.valueOf(historyDays), 6, RoundingMode.HALF_UP)),
                    historyDays >= 90 ? "HIGH" : historyDays >= 45 ? "MEDIUM" : "LOW"
            );
        }

        BigDecimal[] current = summarizeTransactions(getTransactions(accountIds, monthStart, today));
        BigDecimal divisor = BigDecimal.valueOf(Math.max(daysElapsed, 1));
        return new HistoricalDailyAverages(
                scale(current[0].divide(divisor, 6, RoundingMode.HALF_UP)),
                scale(current[1].divide(divisor, 6, RoundingMode.HALF_UP)),
                daysElapsed >= 7 ? "MEDIUM" : "LOW"
        );
    }

    private BigDecimal[] summarizeTransactions(List<FinanceTransaction> transactions) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinanceTransaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                income = income.add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                expense = expense.add(transaction.getAmount());
            }
        }
        return new BigDecimal[]{income, expense};
    }

    private List<FinanceTransaction> getTransactions(Set<UUID> accountIds, LocalDate startDate, LocalDate endDate) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdInAndTransactionDateBetween(accountIds, startDate, endDate);
    }

    private BigDecimal getCurrentBalance(Set<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return accountRepository.findByIdInOrderByCreatedAtDesc(accountIds).stream()
                .map(account -> account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record HistoricalDailyAverages(BigDecimal averageIncomePerDay, BigDecimal averageExpensePerDay, String confidence) {
    }
}

package com.financetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDtos {

    public record DashboardResponse(
            SummaryCards summary,
            List<CategorySpendPoint> categorySpending,
            List<TrendPoint> incomeVsExpense,
            List<TransactionDtos.TransactionResponse> recentTransactions,
            List<RecurringDtos.RecurringResponse> upcomingRecurring,
            List<GoalDtos.GoalResponse> goals,
            List<BudgetDtos.BudgetResponse> budgets
    ) {
    }

    public record SummaryCards(BigDecimal income, BigDecimal expense, BigDecimal balance) {
    }

    public record CategorySpendPoint(String category, BigDecimal amount) {
    }

    public record TrendPoint(String label, BigDecimal income, BigDecimal expense) {
    }

    public record AccountBalancePoint(String account, BigDecimal balance) {
    }

    public record NetWorthPoint(String label, BigDecimal netWorth) {
    }

    public record NetWorthResponse(BigDecimal currentNetWorth, List<NetWorthPoint> history) {
    }

    public record FilteredReportResponse(
            LocalDate startDate,
            LocalDate endDate,
            List<CategorySpendPoint> categorySpend,
            List<TrendPoint> incomeVsExpense,
            List<AccountBalancePoint> accountBalances,
            NetWorthResponse netWorth
    ) {
    }
}

package com.financetracker.service;

import com.financetracker.dto.BudgetDtos;
import com.financetracker.dto.InsightsDtos;
import com.financetracker.dto.ReportDtos;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.repository.AccountRepository;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BudgetService budgetService;
    private final AccessControlService accessControlService;

    @Transactional(readOnly = true)
    public InsightsDtos.HealthScoreResponse getHealthScore() {
        Set<java.util.UUID> accountIds = accessControlService.getAccessibleAccountIds();
        List<MonthSnapshot> months = getMonthlySnapshots(accountIds, 6);
        BigDecimal totalBalance = getCurrentBalance(accountIds);

        int savingsScore = calculateSavingsRateScore(months);
        int stabilityScore = calculateExpenseStabilityScore(months);
        int budgetScore = calculateBudgetAdherenceScore();
        int cashBufferScore = calculateCashBufferScore(totalBalance, months);

        int finalScore = (int) Math.round(
                savingsScore * 0.30
                        + stabilityScore * 0.20
                        + budgetScore * 0.25
                        + cashBufferScore * 0.25
        );

        List<InsightsDtos.ScoreBreakdown> breakdown = List.of(
                new InsightsDtos.ScoreBreakdown("Savings rate", savingsScore, savingsRateDetail(months)),
                new InsightsDtos.ScoreBreakdown("Expense stability", stabilityScore, expenseStabilityDetail(months)),
                new InsightsDtos.ScoreBreakdown("Budget adherence", budgetScore, budgetAdherenceDetail()),
                new InsightsDtos.ScoreBreakdown("Cash buffer", cashBufferScore, cashBufferDetail(totalBalance, months))
        );

        return new InsightsDtos.HealthScoreResponse(finalScore, scoreStatus(finalScore), breakdown, buildSuggestions(savingsScore, stabilityScore, budgetScore, cashBufferScore));
    }

    @Transactional(readOnly = true)
    public InsightsDtos.InsightsResponse getInsights() {
        Set<java.util.UUID> accountIds = accessControlService.getAccessibleAccountIds();
        List<MonthSnapshot> months = getMonthlySnapshots(accountIds, 6);
        InsightsDtos.HealthScoreResponse healthScore = getHealthScore();
        List<ReportDtos.TrendPoint> incomeExpenseTrend = months.stream()
                .map(month -> new ReportDtos.TrendPoint(month.label(), month.income(), month.expense()))
                .toList();
        List<InsightsDtos.SavingsTrendPoint> savingsTrend = months.stream()
                .map(month -> new InsightsDtos.SavingsTrendPoint(month.label(), month.savings(), month.savingsRate()))
                .toList();
        List<InsightsDtos.CategoryTrendPoint> categoryTrends = getCategoryTrends(accountIds, 6);
        List<InsightsDtos.InsightHighlight> highlights = buildHighlights(months, healthScore.score());

        return new InsightsDtos.InsightsResponse(
                healthScore,
                highlights,
                incomeExpenseTrend,
                savingsTrend,
                categoryTrends
        );
    }

    @Transactional(readOnly = true)
    public InsightsDtos.TrendReportResponse getTrendReport() {
        InsightsDtos.InsightsResponse insights = getInsights();
        return new InsightsDtos.TrendReportResponse(
                insights.incomeExpenseTrend(),
                insights.savingsTrend(),
                insights.categoryTrends()
        );
    }

    private List<MonthSnapshot> getMonthlySnapshots(Set<java.util.UUID> accountIds, int monthsBack) {
        LocalDate now = LocalDate.now();
        YearMonth current = YearMonth.from(now);
        LocalDate start = current.minusMonths(monthsBack - 1L).atDay(1);
        LocalDate end = current.atEndOfMonth();
        List<FinanceTransaction> transactions = accountIds.isEmpty() ? List.of() : transactionRepository.findByAccountIdInAndTransactionDateBetween(accountIds, start, end);
        Map<YearMonth, BigDecimal[]> aggregates = new LinkedHashMap<>();

        for (FinanceTransaction transaction : transactions) {
            YearMonth yearMonth = YearMonth.from(transaction.getTransactionDate());
            BigDecimal[] totals = aggregates.computeIfAbsent(yearMonth, ignored -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (transaction.getType() == TransactionType.INCOME) {
                totals[0] = totals[0].add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                totals[1] = totals[1].add(transaction.getAmount());
            }
        }

        List<MonthSnapshot> snapshots = new ArrayList<>();
        for (int index = monthsBack - 1; index >= 0; index--) {
            YearMonth month = current.minusMonths(index);
            BigDecimal[] totals = aggregates.getOrDefault(month, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            snapshots.add(new MonthSnapshot(month.format(MONTH_LABEL), totals[0], totals[1]));
        }
        return snapshots;
    }

    private List<InsightsDtos.CategoryTrendPoint> getCategoryTrends(Set<java.util.UUID> accountIds, int monthsBack) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        YearMonth current = YearMonth.now();
        LocalDate start = current.minusMonths(monthsBack - 1L).atDay(1);
        LocalDate end = current.atEndOfMonth();
        List<FinanceTransaction> transactions = transactionRepository.findByAccountIdInAndTransactionDateBetween(accountIds, start, end);
        Map<String, BigDecimal> totalsByCategory = new LinkedHashMap<>();
        Map<YearMonth, Map<String, BigDecimal>> monthlyCategoryTotals = new LinkedHashMap<>();

        for (FinanceTransaction transaction : transactions) {
            if (transaction.getType() != TransactionType.EXPENSE || transaction.getCategory() == null) {
                continue;
            }
            String category = transaction.getCategory().getName();
            totalsByCategory.merge(category, transaction.getAmount(), BigDecimal::add);
            YearMonth month = YearMonth.from(transaction.getTransactionDate());
            monthlyCategoryTotals.computeIfAbsent(month, ignored -> new LinkedHashMap<>())
                    .merge(category, transaction.getAmount(), BigDecimal::add);
        }

        List<String> topCategories = totalsByCategory.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();

        List<InsightsDtos.CategoryTrendPoint> result = new ArrayList<>();
        for (int index = monthsBack - 1; index >= 0; index--) {
            YearMonth month = current.minusMonths(index);
            Map<String, BigDecimal> categoryTotals = monthlyCategoryTotals.getOrDefault(month, Map.of());
            for (String category : topCategories) {
                result.add(new InsightsDtos.CategoryTrendPoint(
                        month.format(MONTH_LABEL),
                        category,
                        categoryTotals.getOrDefault(category, BigDecimal.ZERO)
                ));
            }
        }
        return result;
    }

    private int calculateSavingsRateScore(List<MonthSnapshot> months) {
        double avgRate = months.stream().mapToDouble(MonthSnapshot::savingsRate).average().orElse(0);
        return clamp((int) Math.round((avgRate / 0.30) * 100));
    }

    private int calculateExpenseStabilityScore(List<MonthSnapshot> months) {
        double avgExpense = months.stream().map(MonthSnapshot::expense).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (avgExpense <= 0) {
            return 65;
        }
        double variance = months.stream()
                .map(MonthSnapshot::expense)
                .mapToDouble(BigDecimal::doubleValue)
                .map(value -> Math.pow(value - avgExpense, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);
        double coefficient = stdDev / avgExpense;
        return clamp((int) Math.round(100 - (coefficient * 160)));
    }

    private int calculateBudgetAdherenceScore() {
        LocalDate now = LocalDate.now();
        List<BudgetDtos.BudgetResponse> budgets = budgetService.getBudgets(now.getMonthValue(), now.getYear());
        if (budgets.isEmpty()) {
            return 60;
        }
        double avgUtilization = budgets.stream().mapToDouble(BudgetDtos.BudgetResponse::utilizationPercentage).average().orElse(100);
        if (avgUtilization <= 80) {
            return 100;
        }
        if (avgUtilization <= 100) {
            return 80;
        }
        if (avgUtilization <= 120) {
            return 50;
        }
        return 20;
    }

    private int calculateCashBufferScore(BigDecimal totalBalance, List<MonthSnapshot> months) {
        double avgExpense = months.stream().map(MonthSnapshot::expense).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (avgExpense <= 0) {
            return 70;
        }
        double monthsCovered = totalBalance.doubleValue() / avgExpense;
        if (monthsCovered >= 6) {
            return 100;
        }
        if (monthsCovered >= 3) {
            return 80;
        }
        if (monthsCovered >= 1) {
            return 55;
        }
        return 25;
    }

    private List<InsightsDtos.InsightHighlight> buildHighlights(List<MonthSnapshot> months, int score) {
        List<InsightsDtos.InsightHighlight> highlights = new ArrayList<>();
        MonthSnapshot latest = months.get(months.size() - 1);
        MonthSnapshot previous = months.size() > 1 ? months.get(months.size() - 2) : null;

        if (previous != null) {
            BigDecimal expenseChange = latest.expense().subtract(previous.expense());
            if (expenseChange.compareTo(BigDecimal.ZERO) > 0) {
                highlights.add(new InsightsDtos.InsightHighlight(
                        "Spending increased",
                        String.format("Expenses in %s are %s higher than %s.", latest.label(), formatAbsolute(expenseChange), previous.label()),
                        "warning"
                ));
            } else if (expenseChange.compareTo(BigDecimal.ZERO) < 0) {
                highlights.add(new InsightsDtos.InsightHighlight(
                        "Spending improved",
                        String.format("Expenses in %s are %s lower than %s.", latest.label(), formatAbsolute(expenseChange.abs()), previous.label()),
                        "positive"
                ));
            }

            BigDecimal savingsChange = latest.savings().subtract(previous.savings());
            if (savingsChange.compareTo(BigDecimal.ZERO) > 0) {
                highlights.add(new InsightsDtos.InsightHighlight(
                        "Savings momentum",
                        String.format("Net savings improved by %s compared with %s.", formatAbsolute(savingsChange), previous.label()),
                        "positive"
                ));
            }
        }

        highlights.add(new InsightsDtos.InsightHighlight(
                "Financial health",
                String.format("Your current financial health score is %d, which is rated %s.", score, scoreStatus(score).toLowerCase(Locale.ENGLISH)),
                score >= 70 ? "positive" : score >= 50 ? "neutral" : "warning"
        ));
        return highlights;
    }

    private String savingsRateDetail(List<MonthSnapshot> months) {
        double avgRate = months.stream().mapToDouble(MonthSnapshot::savingsRate).average().orElse(0) * 100;
        return String.format(Locale.ENGLISH, "Average savings rate over recent months is %.1f%%.", avgRate);
    }

    private String expenseStabilityDetail(List<MonthSnapshot> months) {
        double avgExpense = months.stream().map(MonthSnapshot::expense).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        return String.format(Locale.ENGLISH, "Average monthly expense is %.0f with moderate month-to-month variation.", avgExpense);
    }

    private String budgetAdherenceDetail() {
        LocalDate now = LocalDate.now();
        List<BudgetDtos.BudgetResponse> budgets = budgetService.getBudgets(now.getMonthValue(), now.getYear());
        if (budgets.isEmpty()) {
            return "No active budgets yet, so this metric uses a neutral baseline.";
        }
        double avgUtilization = budgets.stream().mapToDouble(BudgetDtos.BudgetResponse::utilizationPercentage).average().orElse(0);
        return String.format(Locale.ENGLISH, "Average budget utilization this month is %.1f%%.", avgUtilization);
    }

    private String cashBufferDetail(BigDecimal totalBalance, List<MonthSnapshot> months) {
        double avgExpense = months.stream().map(MonthSnapshot::expense).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (avgExpense <= 0) {
            return "Cash buffer is healthy because recent expense history is limited.";
        }
        double monthsCovered = totalBalance.doubleValue() / avgExpense;
        return String.format(Locale.ENGLISH, "Current balances can cover about %.1f months of average expenses.", monthsCovered);
    }

    private List<String> buildSuggestions(int savingsScore, int stabilityScore, int budgetScore, int cashBufferScore) {
        List<String> suggestions = new ArrayList<>();
        if (savingsScore < 60) {
            suggestions.add("Try setting aside a fixed savings amount right after income is credited.");
        }
        if (stabilityScore < 60) {
            suggestions.add("Review variable expenses and cap the categories that fluctuate the most.");
        }
        if (budgetScore < 60) {
            suggestions.add("Tighten overspending categories or lower non-essential monthly expenses.");
        }
        if (cashBufferScore < 60) {
            suggestions.add("Build an emergency buffer to cover at least one to three months of expenses.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Your finances are tracking well. Keep monitoring trends and maintain your current discipline.");
        }
        return suggestions;
    }

    private String scoreStatus(int score) {
        if (score >= 80) {
            return "Excellent";
        }
        if (score >= 65) {
            return "Good";
        }
        if (score >= 45) {
            return "Fair";
        }
        return "Needs attention";
    }

    private BigDecimal getCurrentBalance(Set<java.util.UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return accountRepository.findByIdInOrderByCreatedAtDesc(accountIds).stream()
                .map(account -> account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String formatAbsolute(BigDecimal value) {
        return "INR " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record MonthSnapshot(String label, BigDecimal income, BigDecimal expense) {
        BigDecimal savings() {
            return income.subtract(expense);
        }

        double savingsRate() {
            if (income.compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }
            return savings().divide(income, 4, RoundingMode.HALF_UP).doubleValue();
        }
    }
}

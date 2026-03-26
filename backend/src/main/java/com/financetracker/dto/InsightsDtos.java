package com.financetracker.dto;

import java.math.BigDecimal;
import java.util.List;

public class InsightsDtos {

    public record HealthScoreResponse(
            int score,
            String status,
            List<ScoreBreakdown> breakdown,
            List<String> suggestions
    ) {
    }

    public record ScoreBreakdown(
            String metric,
            int score,
            String detail
    ) {
    }

    public record InsightsResponse(
            HealthScoreResponse healthScore,
            List<InsightHighlight> highlights,
            List<ReportDtos.TrendPoint> incomeExpenseTrend,
            List<SavingsTrendPoint> savingsTrend,
            List<CategoryTrendPoint> categoryTrends
    ) {
    }

    public record InsightHighlight(
            String title,
            String message,
            String tone
    ) {
    }

    public record SavingsTrendPoint(
            String label,
            BigDecimal savings,
            double savingsRate
    ) {
    }

    public record CategoryTrendPoint(
            String label,
            String category,
            BigDecimal amount
    ) {
    }

    public record TrendReportResponse(
            List<ReportDtos.TrendPoint> incomeExpenseTrend,
            List<SavingsTrendPoint> savingsTrend,
            List<CategoryTrendPoint> categoryTrends
    ) {
    }
}

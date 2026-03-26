package com.financetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ForecastDtos {

    public record MonthForecastResponse(
            BigDecimal currentBalance,
            BigDecimal projectedEndBalance,
            BigDecimal projectedIncome,
            BigDecimal projectedExpense,
            BigDecimal projectedNet,
            BigDecimal safeToSpend,
            int daysRemaining,
            String confidence,
            String riskLevel,
            String warning
    ) {
    }

    public record DailyForecastResponse(
            BigDecimal currentBalance,
            BigDecimal projectedEndBalance,
            List<DailyProjectionPoint> projections
    ) {
    }

    public record DailyProjectionPoint(
            LocalDate date,
            BigDecimal projectedBalance
    ) {
    }
}

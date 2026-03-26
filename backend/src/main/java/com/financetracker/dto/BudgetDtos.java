package com.financetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BudgetDtos {

    public record BudgetRequest(
            @NotBlank String categoryId,
            String accountId,
            @NotNull @Min(1) @Max(12) Integer month,
            @NotNull @Min(2000) Integer year,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull @Min(1) @Max(120) Integer alertThresholdPercent
    ) {
    }

    public record BudgetResponse(
            String id,
            String categoryId,
            String categoryName,
            String accountId,
            String accountName,
            Integer month,
            Integer year,
            BigDecimal amount,
            BigDecimal spent,
            BigDecimal remaining,
            Integer alertThresholdPercent,
            double utilizationPercentage
    ) {
    }
}

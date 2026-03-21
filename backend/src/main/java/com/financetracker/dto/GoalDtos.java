package com.financetracker.dto;

import com.financetracker.entity.GoalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class GoalDtos {

    public record GoalRequest(
            @NotBlank String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
            LocalDate targetDate,
            String linkedAccountId,
            String icon,
            String color
    ) {
    }

    public record GoalContributionRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String sourceAccountId
    ) {
    }

    public record GoalResponse(
            String id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate targetDate,
            String linkedAccountId,
            String icon,
            String color,
            GoalStatus status,
            double progressPercentage
    ) {
    }
}

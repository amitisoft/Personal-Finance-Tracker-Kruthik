package com.financetracker.dto;

import com.financetracker.entity.RecurringFrequency;
import com.financetracker.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringDtos {

    public record RecurringRequest(
            @NotBlank String title,
            @NotNull TransactionType type,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String categoryId,
            String accountId,
            @NotNull RecurringFrequency frequency,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            @NotNull Boolean autoCreateTransaction,
            Boolean paused
    ) {
    }

    public record RecurringResponse(
            String id,
            String title,
            TransactionType type,
            BigDecimal amount,
            String categoryId,
            String categoryName,
            String accountId,
            String accountName,
            RecurringFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate nextRunDate,
            Boolean autoCreateTransaction,
            Boolean paused
    ) {
    }
}

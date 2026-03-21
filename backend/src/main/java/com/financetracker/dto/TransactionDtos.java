package com.financetracker.dto;

import com.financetracker.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionDtos {

    public record TransactionRequest(
            @NotNull TransactionType type,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull LocalDate date,
            @NotBlank String accountId,
            String categoryId,
            String transferAccountId,
            String merchant,
            String note,
            String paymentMethod,
            List<String> tags
    ) {
    }

    public record TransactionResponse(
            String id,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String accountId,
            String accountName,
            String categoryId,
            String categoryName,
            String merchant,
            String note,
            String paymentMethod,
            List<String> tags,
            String createdAt
    ) {
    }

    public record TransactionPageResponse(
            List<TransactionResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}

package com.financetracker.dto;

import com.financetracker.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AccountDtos {

    public record AccountRequest(
            @NotBlank String name,
            @NotNull AccountType type,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal openingBalance,
            String institutionName
    ) {
    }

    public record TransferRequest(
            @NotBlank String fromAccountId,
            @NotBlank String toAccountId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            String note
    ) {
    }

    public record AccountResponse(
            String id,
            String name,
            AccountType type,
            BigDecimal openingBalance,
            BigDecimal currentBalance,
            String institutionName,
            String createdAt
    ) {
    }
}

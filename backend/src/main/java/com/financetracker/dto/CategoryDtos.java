package com.financetracker.dto;

import com.financetracker.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryDtos {

    public record CategoryRequest(
            @NotBlank String name,
            @NotNull CategoryType type,
            String color,
            String icon,
            Boolean isArchived
    ) {
    }

    public record CategoryResponse(
            String id,
            String name,
            CategoryType type,
            String color,
            String icon,
            Boolean isArchived
    ) {
    }
}

package com.financetracker.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class RuleDtos {

    public record RuleRequest(
            @NotBlank String name,
            @NotBlank String conditionJson,
            @NotBlank String actionJson,
            Boolean isActive
    ) {
    }

    public record RuleResponse(
            String id,
            String name,
            String conditionJson,
            String actionJson,
            Boolean isActive,
            String createdAt
    ) {
    }

    public record RuleEvaluationResult(
            String categoryId,
            List<String> tags,
            String note,
            List<String> matchedRuleNames
    ) {
    }
}

package com.financetracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.dto.RuleDtos;
import com.financetracker.dto.TransactionDtos;
import com.financetracker.entity.Rule;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.RuleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesEngineService {

    private final RuleRepository ruleRepository;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<RuleDtos.RuleResponse> getRules() {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return ruleRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RuleDtos.RuleResponse create(RuleDtos.RuleRequest request) {
        User user = userContextService.getCurrentUserEntity();
        Rule rule = Rule.builder()
                .user(user)
                .name(request.name())
                .conditionJson(request.conditionJson())
                .actionJson(request.actionJson())
                .isActive(!Boolean.FALSE.equals(request.isActive()))
                .build();
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public RuleDtos.RuleResponse update(String id, RuleDtos.RuleRequest request) {
        Rule rule = getOwnedRule(id);
        rule.setName(request.name());
        rule.setConditionJson(request.conditionJson());
        rule.setActionJson(request.actionJson());
        rule.setIsActive(!Boolean.FALSE.equals(request.isActive()));
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void delete(String id) {
        ruleRepository.delete(getOwnedRule(id));
    }

    @Transactional(readOnly = true)
    public TransactionDtos.TransactionRequest applyRules(TransactionDtos.TransactionRequest request) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        String categoryId = request.categoryId();
        String note = request.note();
        Set<String> tags = new LinkedHashSet<>(request.tags() == null ? List.of() : request.tags());

        for (Rule rule : ruleRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)) {
            try {
                Map<String, Object> condition = objectMapper.readValue(rule.getConditionJson(), new TypeReference<>() {});
                Map<String, Object> action = objectMapper.readValue(rule.getActionJson(), new TypeReference<>() {});
                if (!matches(condition, request)) {
                    continue;
                }
                if ((categoryId == null || categoryId.isBlank()) && action.get("categoryId") instanceof String nextCategoryId && !nextCategoryId.isBlank()) {
                    categoryId = nextCategoryId;
                }
                if (action.get("tag") instanceof String tag && !tag.isBlank()) {
                    tags.add(tag.trim());
                }
                if (action.get("appendNote") instanceof String appendNote && !appendNote.isBlank()) {
                    note = note == null || note.isBlank() ? appendNote : note + " | " + appendNote;
                }
            } catch (Exception ex) {
                log.warn("Failed to evaluate rule {}: {}", rule.getId(), ex.getMessage());
            }
        }

        return new TransactionDtos.TransactionRequest(
                request.type(),
                request.amount(),
                request.date(),
                request.accountId(),
                categoryId,
                request.transferAccountId(),
                request.merchant(),
                note,
                request.paymentMethod(),
                new ArrayList<>(tags)
        );
    }

    private boolean matches(Map<String, Object> condition, TransactionDtos.TransactionRequest request) {
        if (condition.get("type") instanceof String type && request.type() != TransactionType.valueOf(type.toUpperCase(Locale.ENGLISH))) {
            return false;
        }
        if (condition.get("merchantContains") instanceof String merchantContains) {
            String merchant = request.merchant() == null ? "" : request.merchant().toLowerCase(Locale.ENGLISH);
            if (!merchant.contains(merchantContains.toLowerCase(Locale.ENGLISH))) {
                return false;
            }
        }
        if (condition.get("accountId") instanceof String accountId && !accountId.equals(request.accountId())) {
            return false;
        }
        if (condition.get("minAmount") instanceof Number minAmount) {
            if (request.amount().compareTo(BigDecimal.valueOf(minAmount.doubleValue())) < 0) {
                return false;
            }
        }
        return true;
    }

    private Rule getOwnedRule(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return ruleRepository.findByIdAndUserId(UUID.fromString(id), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
    }

    private RuleDtos.RuleResponse toResponse(Rule rule) {
        return new RuleDtos.RuleResponse(
                rule.getId().toString(),
                rule.getName(),
                rule.getConditionJson(),
                rule.getActionJson(),
                rule.getIsActive(),
                rule.getCreatedAt() == null ? null : rule.getCreatedAt().toString()
        );
    }
}

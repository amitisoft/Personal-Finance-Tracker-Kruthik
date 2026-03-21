package com.financetracker.service;

import com.financetracker.dto.BudgetDtos;
import com.financetracker.entity.Budget;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.BudgetRepository;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserContextService userContextService;
    private final CategoryService categoryService;
    private final MapperService mapperService;

    @Transactional(readOnly = true)
    public List<BudgetDtos.BudgetResponse> getBudgets(Integer month, Integer year) {
        User user = userContextService.getCurrentUserEntity();
        return budgetRepository.findByUserIdAndMonthAndYearOrderByCreatedAtDesc(user.getId(), month, year).stream()
                .map(budget -> mapperService.toBudgetResponse(budget, calculateSpent(user.getId(), budget)))
                .toList();
    }

    @Transactional
    public BudgetDtos.BudgetResponse create(BudgetDtos.BudgetRequest request) {
        User user = userContextService.getCurrentUserEntity();
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(user.getId(), UUID.fromString(request.categoryId()), request.month(), request.year())) {
            throw new BadRequestException("Budget already exists for this category and month");
        }
        Budget budget = Budget.builder()
                .user(user)
                .category(categoryService.getOwnedCategory(request.categoryId()))
                .month(request.month())
                .year(request.year())
                .amount(request.amount())
                .alertThresholdPercent(request.alertThresholdPercent())
                .build();
        Budget saved = budgetRepository.save(budget);
        return mapperService.toBudgetResponse(saved, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetDtos.BudgetResponse update(String id, BudgetDtos.BudgetRequest request) {
        Budget budget = getOwnedBudget(id);
        budget.setCategory(categoryService.getOwnedCategory(request.categoryId()));
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setAmount(request.amount());
        budget.setAlertThresholdPercent(request.alertThresholdPercent());
        Budget saved = budgetRepository.save(budget);
        return mapperService.toBudgetResponse(saved, calculateSpent(userContextService.getCurrentUserEntity().getId(), saved));
    }

    @Transactional
    public void delete(String id) {
        budgetRepository.delete(getOwnedBudget(id));
    }

    @Transactional(readOnly = true)
    private Budget getOwnedBudget(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return budgetRepository.findByIdAndUserId(UUID.fromString(id), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private BigDecimal calculateSpent(UUID userId, Budget budget) {
        LocalDate start = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end).stream()
                .filter(tx -> tx.getType() == TransactionType.EXPENSE)
                .filter(tx -> tx.getCategory() != null && tx.getCategory().getId().equals(budget.getCategory().getId()))
                .map(tx -> tx.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

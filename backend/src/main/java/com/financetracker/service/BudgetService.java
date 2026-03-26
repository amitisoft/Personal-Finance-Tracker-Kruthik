package com.financetracker.service;

import com.financetracker.dto.BudgetDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.Budget;
import com.financetracker.entity.Category;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.BudgetRepository;
import com.financetracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final AccountService accountService;
    private final AccessControlService accessControlService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<BudgetDtos.BudgetResponse> getBudgets(Integer month, Integer year) {
        User user = userContextService.getCurrentUserEntity();
        Set<UUID> accessibleAccountIds = accessControlService.getAccessibleAccountIds();
        Map<UUID, Budget> merged = new LinkedHashMap<>();

        for (Budget budget : budgetRepository.findByUserIdAndMonthAndYearOrderByCreatedAtDesc(user.getId(), month, year)) {
            if (budget.getAccount() == null || accessibleAccountIds.contains(budget.getAccount().getId())) {
                merged.put(budget.getId(), budget);
            }
        }
        if (!accessibleAccountIds.isEmpty()) {
            for (Budget budget : budgetRepository.findByAccountIdInAndMonthAndYearOrderByCreatedAtDesc(accessibleAccountIds, month, year)) {
                merged.putIfAbsent(budget.getId(), budget);
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(Budget::getCreatedAt).reversed())
                .map(budget -> mapperService.toBudgetResponse(budget, calculateSpent(budget)))
                .toList();
    }

    @Transactional
    public BudgetDtos.BudgetResponse create(BudgetDtos.BudgetRequest request) {
        User user = userContextService.getCurrentUserEntity();
        Account account = request.accountId() == null || request.accountId().isBlank() ? null : accountService.getWritableAccount(request.accountId());
        Category category = categoryService.getOwnedCategory(request.categoryId());

        if (account == null) {
            if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYearAndAccountIsNull(user.getId(), category.getId(), request.month(), request.year())) {
                throw new BadRequestException("Budget already exists for this category and month");
            }
        } else if (budgetRepository.existsByCategoryIdAndMonthAndYearAndAccountId(category.getId(), request.month(), request.year(), account.getId())) {
            throw new BadRequestException("Budget already exists for this category, account, and month");
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .account(account)
                .month(request.month())
                .year(request.year())
                .amount(request.amount())
                .alertThresholdPercent(request.alertThresholdPercent())
                .build();
        Budget saved = budgetRepository.save(budget);
        activityLogService.record(account, "CREATE", "BUDGET", category.getName(), "Created budget for " + request.month() + "/" + request.year());
        return mapperService.toBudgetResponse(saved, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetDtos.BudgetResponse update(String id, BudgetDtos.BudgetRequest request) {
        Budget budget = getAccessibleBudget(id, true);
        Account account = request.accountId() == null || request.accountId().isBlank() ? null : accountService.getWritableAccount(request.accountId());
        budget.setCategory(resolveCategory(request.categoryId(), budget));
        budget.setAccount(account);
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setAmount(request.amount());
        budget.setAlertThresholdPercent(request.alertThresholdPercent());
        Budget saved = budgetRepository.save(budget);
        activityLogService.record(account, "UPDATE", "BUDGET", saved.getCategory().getName(), "Updated budget configuration");
        return mapperService.toBudgetResponse(saved, calculateSpent(saved));
    }

    @Transactional
    public void delete(String id) {
        Budget budget = getAccessibleBudget(id, true);
        activityLogService.record(budget.getAccount(), "DELETE", "BUDGET", budget.getCategory().getName(), "Deleted budget");
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    private Budget getAccessibleBudget(String id, boolean writeRequired) {
        Budget budget = budgetRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        UUID userId = userContextService.getCurrentUserEntity().getId();
        if (budget.getAccount() != null) {
            if (writeRequired) {
                accessControlService.requireAccountWriteAccess(budget.getAccount().getId().toString());
            } else {
                accessControlService.requireAccountReadAccess(budget.getAccount().getId().toString());
            }
            return budget;
        }
        if (!budget.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Budget not found");
        }
        return budget;
    }

    private Category resolveCategory(String categoryId, Budget existing) {
        if (existing != null && existing.getCategory() != null && existing.getCategory().getId().toString().equals(categoryId)) {
            return existing.getCategory();
        }
        return categoryService.getOwnedCategory(categoryId);
    }

    private BigDecimal calculateSpent(Budget budget) {
        LocalDate start = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<com.financetracker.entity.FinanceTransaction> transactions;
        if (budget.getAccount() != null) {
            transactions = transactionRepository.findByAccountIdInAndTransactionDateBetween(List.of(budget.getAccount().getId()), start, end);
        } else {
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(budget.getUser().getId(), start, end);
        }
        BigDecimal spent = BigDecimal.ZERO;
        for (com.financetracker.entity.FinanceTransaction tx : transactions) {
            if (tx.getType() == TransactionType.EXPENSE && tx.getCategory() != null && tx.getCategory().getId().equals(budget.getCategory().getId())) {
                spent = spent.add(tx.getAmount());
            }
        }
        return spent;
    }
}

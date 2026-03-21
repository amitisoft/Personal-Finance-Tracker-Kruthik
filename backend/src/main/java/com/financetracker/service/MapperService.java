package com.financetracker.service;

import com.financetracker.dto.AccountDtos;
import com.financetracker.dto.BudgetDtos;
import com.financetracker.dto.CategoryDtos;
import com.financetracker.dto.GoalDtos;
import com.financetracker.dto.RecurringDtos;
import com.financetracker.dto.TransactionDtos;
import com.financetracker.entity.Account;
import com.financetracker.entity.Budget;
import com.financetracker.entity.Category;
import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.Goal;
import com.financetracker.entity.RecurringTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MapperService {

    public AccountDtos.AccountResponse toAccountResponse(Account account) {
        return new AccountDtos.AccountResponse(
                account.getId().toString(),
                account.getName(),
                account.getType(),
                account.getOpeningBalance(),
                account.getCurrentBalance(),
                account.getInstitutionName(),
                account.getCreatedAt() == null ? null : account.getCreatedAt().toString()
        );
    }

    public CategoryDtos.CategoryResponse toCategoryResponse(Category category) {
        return new CategoryDtos.CategoryResponse(
                category.getId().toString(),
                category.getName(),
                category.getType(),
                category.getColor(),
                category.getIcon(),
                category.getIsArchived()
        );
    }

    public TransactionDtos.TransactionResponse toTransactionResponse(FinanceTransaction transaction) {
        return new TransactionDtos.TransactionResponse(
                transaction.getId().toString(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getAccount().getId().toString(),
                transaction.getAccount().getName(),
                transaction.getCategory() == null ? null : transaction.getCategory().getId().toString(),
                transaction.getCategory() == null ? null : transaction.getCategory().getName(),
                transaction.getMerchant(),
                transaction.getNote(),
                transaction.getPaymentMethod(),
                List.copyOf(transaction.getTags()),
                transaction.getCreatedAt() == null ? null : transaction.getCreatedAt().toString()
        );
    }

    public BudgetDtos.BudgetResponse toBudgetResponse(Budget budget, BigDecimal spent) {
        BigDecimal remaining = budget.getAmount().subtract(spent);
        double utilization = budget.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 : spent
                .multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP)
                .doubleValue();
        return new BudgetDtos.BudgetResponse(
                budget.getId().toString(),
                budget.getCategory().getId().toString(),
                budget.getCategory().getName(),
                budget.getMonth(),
                budget.getYear(),
                budget.getAmount(),
                spent,
                remaining,
                budget.getAlertThresholdPercent(),
                utilization
        );
    }

    public GoalDtos.GoalResponse toGoalResponse(Goal goal) {
        double progress = goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 : goal.getCurrentAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                .doubleValue();
        return new GoalDtos.GoalResponse(
                goal.getId().toString(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                goal.getLinkedAccount() == null ? null : goal.getLinkedAccount().getId().toString(),
                goal.getIcon(),
                goal.getColor(),
                goal.getStatus(),
                progress
        );
    }

    public RecurringDtos.RecurringResponse toRecurringResponse(RecurringTransaction recurring) {
        return new RecurringDtos.RecurringResponse(
                recurring.getId().toString(),
                recurring.getTitle(),
                recurring.getType(),
                recurring.getAmount(),
                recurring.getCategory() == null ? null : recurring.getCategory().getId().toString(),
                recurring.getCategory() == null ? null : recurring.getCategory().getName(),
                recurring.getAccount() == null ? null : recurring.getAccount().getId().toString(),
                recurring.getAccount() == null ? null : recurring.getAccount().getName(),
                recurring.getFrequency(),
                recurring.getStartDate(),
                recurring.getEndDate(),
                recurring.getNextRunDate(),
                recurring.getAutoCreateTransaction(),
                recurring.getPaused()
        );
    }
}

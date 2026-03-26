package com.financetracker.repository;

import com.financetracker.entity.Budget;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserIdAndMonthAndYearOrderByCreatedAtDesc(UUID userId, Integer month, Integer year);
    List<Budget> findByAccountIdInAndMonthAndYearOrderByCreatedAtDesc(Collection<UUID> accountIds, Integer month, Integer year);
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndCategoryIdAndMonthAndYearAndAccountIsNull(UUID userId, UUID categoryId, Integer month, Integer year);
    boolean existsByCategoryIdAndMonthAndYearAndAccountId(UUID categoryId, Integer month, Integer year, UUID accountId);
}

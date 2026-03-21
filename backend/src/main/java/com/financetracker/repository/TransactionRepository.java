package com.financetracker.repository;

import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
    Optional<FinanceTransaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select t from FinanceTransaction t
            where t.user.id = :userId
            and (:accountId is null or t.account.id = :accountId)
            and (:categoryId is null or t.category.id = :categoryId)
            and (:type is null or t.type = :type)
            and (:searchPattern is null or lower(coalesce(t.merchant, '')) like :searchPattern
                 or lower(coalesce(t.note, '')) like :searchPattern)
            and (:startDate is null or t.transactionDate >= :startDate)
            and (:endDate is null or t.transactionDate <= :endDate)
            order by t.transactionDate desc, t.createdAt desc
            """)
    Page<FinanceTransaction> search(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("type") TransactionType type,
            @Param("searchPattern") String searchPattern,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    List<FinanceTransaction> findByUserIdAndTransactionDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("""
            select coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.INCOME then t.amount else 0 end), 0),
                   coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.EXPENSE then t.amount else 0 end), 0)
            from FinanceTransaction t
            where t.user.id = :userId and t.transactionDate between :startDate and :endDate
            """)
    Object[] getIncomeExpenseSummary(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select t.category.name, coalesce(sum(t.amount), 0)
            from FinanceTransaction t
            where t.user.id = :userId
              and t.type = com.financetracker.entity.TransactionType.EXPENSE
              and t.transactionDate between :startDate and :endDate
              and t.category is not null
            group by t.category.name
            order by sum(t.amount) desc
            """)
    List<Object[]> getCategorySpend(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select t from FinanceTransaction t
            where t.user.id = :userId
            order by t.transactionDate desc, t.createdAt desc
            """)
    Page<FinanceTransaction> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);
}

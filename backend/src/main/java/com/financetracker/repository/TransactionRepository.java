package com.financetracker.repository;

import com.financetracker.entity.FinanceTransaction;
import com.financetracker.entity.TransactionType;
import java.time.LocalDate;
import java.util.Collection;
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
    Optional<FinanceTransaction> findByIdAndAccountIdIn(UUID id, Collection<UUID> accountIds);

    @Query("""
            select t from FinanceTransaction t
            where t.account.id in :accountIds
            and (:accountId is null or t.account.id = :accountId)
            and (:categoryId is null or t.category.id = :categoryId)
            and (:type is null or t.type = :type)
            and (:searchPattern is null or lower(coalesce(t.merchant, '')) like :searchPattern
                 or lower(coalesce(t.note, '')) like :searchPattern)
            and (:startDate is null or t.transactionDate >= :startDate)
            and (:endDate is null or t.transactionDate <= :endDate)
            order by t.transactionDate desc, t.createdAt desc
            """)
    Page<FinanceTransaction> searchByAccountIds(
            @Param("accountIds") Collection<UUID> accountIds,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("type") TransactionType type,
            @Param("searchPattern") String searchPattern,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            select t from FinanceTransaction t
            where t.account.id in :accountIds
            and t.transactionDate between :startDate and :endDate
            """)
    List<FinanceTransaction> findByAccountIdInAndTransactionDateBetween(@Param("accountIds") Collection<UUID> accountIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<FinanceTransaction> findByUserIdAndTransactionDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("""
            select coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.INCOME then t.amount else 0 end), 0),
                   coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.EXPENSE then t.amount else 0 end), 0)
            from FinanceTransaction t
            where t.user.id = :userId and t.transactionDate between :startDate and :endDate
            """)
    Object[] getIncomeExpenseSummary(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select year(t.transactionDate), month(t.transactionDate),
                   coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.INCOME then t.amount else 0 end), 0),
                   coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.EXPENSE then t.amount else 0 end), 0)
            from FinanceTransaction t
            where t.user.id = :userId and t.transactionDate between :startDate and :endDate
            group by year(t.transactionDate), month(t.transactionDate)
            order by year(t.transactionDate), month(t.transactionDate)
            """)
    List<Object[]> getMonthlyIncomeExpenseTotals(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select t.transactionDate,
                   coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.INCOME then t.amount else 0 end), 0),
                   coalesce(sum(case when t.type = com.financetracker.entity.TransactionType.EXPENSE then t.amount else 0 end), 0)
            from FinanceTransaction t
            where t.user.id = :userId and t.transactionDate between :startDate and :endDate
            group by t.transactionDate
            order by t.transactionDate
            """)
    List<Object[]> getDailyIncomeExpenseTotals(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select year(t.transactionDate), month(t.transactionDate), t.category.name, coalesce(sum(t.amount), 0)
            from FinanceTransaction t
            where t.user.id = :userId
              and t.type = com.financetracker.entity.TransactionType.EXPENSE
              and t.transactionDate between :startDate and :endDate
              and t.category is not null
            group by year(t.transactionDate), month(t.transactionDate), t.category.name
            order by year(t.transactionDate), month(t.transactionDate), sum(t.amount) desc
            """)
    List<Object[]> getCategorySpendTrend(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

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
            where t.account.id in :accountIds
            order by t.transactionDate desc, t.createdAt desc
            """)
    Page<FinanceTransaction> findRecentByAccountIds(@Param("accountIds") Collection<UUID> accountIds, Pageable pageable);

    @Query("""
            select t from FinanceTransaction t
            where t.user.id = :userId
            order by t.transactionDate desc, t.createdAt desc
            """)
    Page<FinanceTransaction> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);
}

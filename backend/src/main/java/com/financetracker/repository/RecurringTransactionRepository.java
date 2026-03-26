package com.financetracker.repository;

import com.financetracker.entity.RecurringTransaction;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {
    List<RecurringTransaction> findByUserIdOrderByNextRunDateAsc(UUID userId);
    List<RecurringTransaction> findByAccountIdInOrderByNextRunDateAsc(Collection<UUID> accountIds);
    Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);
    List<RecurringTransaction> findByPausedFalseAndAutoCreateTransactionTrueAndNextRunDateLessThanEqual(LocalDate date);
}

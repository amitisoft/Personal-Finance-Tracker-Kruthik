package com.financetracker.repository;

import com.financetracker.entity.Account;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Account> findByIdInOrderByCreatedAtDesc(Collection<UUID> ids);
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
}

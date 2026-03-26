package com.financetracker.repository;

import com.financetracker.entity.AccountMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMemberRepository extends JpaRepository<AccountMember, UUID> {
    List<AccountMember> findByUserId(UUID userId);
    List<AccountMember> findByAccountId(UUID accountId);
    Optional<AccountMember> findByAccountIdAndUserId(UUID accountId, UUID userId);
    boolean existsByAccountIdAndUserId(UUID accountId, UUID userId);
}

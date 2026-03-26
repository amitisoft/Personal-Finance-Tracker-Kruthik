package com.financetracker.repository;

import com.financetracker.entity.Goal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Goal> findByLinkedAccountIdInOrderByCreatedAtDesc(Collection<UUID> accountIds);
    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);
}

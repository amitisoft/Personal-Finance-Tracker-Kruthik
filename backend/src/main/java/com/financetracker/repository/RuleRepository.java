package com.financetracker.repository;

import com.financetracker.entity.Rule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Rule> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);
    Optional<Rule> findByIdAndUserId(UUID id, UUID userId);
}

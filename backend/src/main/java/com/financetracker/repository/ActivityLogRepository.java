package com.financetracker.repository;

import com.financetracker.entity.ActivityLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop20ByAccountIdOrderByCreatedAtDesc(UUID accountId);
}

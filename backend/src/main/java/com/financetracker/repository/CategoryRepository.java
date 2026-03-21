package com.financetracker.repository;

import com.financetracker.entity.Category;
import com.financetracker.entity.CategoryType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserIdOrderByNameAsc(UUID userId);
    List<Category> findByUserIdAndTypeOrderByNameAsc(UUID userId, CategoryType type);
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
}

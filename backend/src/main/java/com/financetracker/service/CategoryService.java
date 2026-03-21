package com.financetracker.service;

import com.financetracker.dto.CategoryDtos;
import com.financetracker.entity.Category;
import com.financetracker.entity.User;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserContextService userContextService;
    private final MapperService mapperService;

    public List<CategoryDtos.CategoryResponse> getCategories() {
        User user = userContextService.getCurrentUserEntity();
        return categoryRepository.findByUserIdOrderByNameAsc(user.getId()).stream()
                .map(mapperService::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest request) {
        User user = userContextService.getCurrentUserEntity();
        Category category = Category.builder()
                .user(user)
                .name(request.name())
                .type(request.type())
                .color(request.color())
                .icon(request.icon())
                .isArchived(Boolean.TRUE.equals(request.isArchived()))
                .build();
        return mapperService.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(String id, CategoryDtos.CategoryRequest request) {
        Category category = getOwnedCategory(id);
        category.setName(request.name());
        category.setType(request.type());
        category.setColor(request.color());
        category.setIcon(request.icon());
        category.setIsArchived(Boolean.TRUE.equals(request.isArchived()));
        return mapperService.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(String id) {
        categoryRepository.delete(getOwnedCategory(id));
    }

    public Category getOwnedCategory(String id) {
        UUID userId = userContextService.getCurrentUserEntity().getId();
        return categoryRepository.findByIdAndUserId(UUID.fromString(id), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}

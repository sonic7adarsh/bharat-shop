package com.bharatshop.storefront.service;

import com.bharatshop.shared.dto.CategoryResponseDto;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.repository.CategoryRepository;
import com.bharatshop.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Storefront category service for customer-facing category operations.
 * Provides read-only category browsing functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StorefrontCategoryService {
    
    private final CategoryRepository categoryRepository;
    
    /**
     * Get all active categories for customers
     */
    @Cacheable(value = "storefront:categories")
    public List<CategoryResponseDto> getCustomerCategories() {
        Long tenantId = TenantContext.getCurrentTenant();
        log.debug("Fetching customer categories for tenant: {}", tenantId);
        
        List<Category> categories = categoryRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId);
        
        return categories.stream()
                .filter(category -> Boolean.TRUE.equals(category.getIsActive()))
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Map Category entity to CategoryResponseDto
     */
    private CategoryResponseDto mapToResponseDto(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(null) // Category entity doesn't have imageUrl field
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
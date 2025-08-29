package com.bharatshop.platform.service;

import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.repository.CategoryRepository;
import com.bharatshop.shared.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FeatureFlagService featureFlagService;

    public List<Category> getAllCategoriesByTenant(UUID tenantId) {
        return categoryRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId);
    }

    public List<Category> getActiveCategoriesByTenant(UUID tenantId) {
        return categoryRepository.findByTenantIdAndIsActiveAndDeletedAtIsNullOrderBySortOrderAsc(tenantId, true);
    }

    public Optional<Category> getCategoryById(UUID id, UUID tenantId) {
        return categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId);
    }

    public Optional<Category> getCategoryBySlug(String slug, UUID tenantId) {
        return categoryRepository.findBySlugAndTenantIdAndDeletedAtIsNull(slug, tenantId);
    }

    public List<Category> getRootCategories(UUID tenantId) {
        return categoryRepository.findRootCategories(tenantId);
    }

    public List<Category> getChildCategories(UUID tenantId, UUID parentId) {
        // Convert UUID parentId to Long for repository method
        Long parentIdLong = Long.parseLong(parentId.toString().replace("-", "").substring(0, 10));
        return categoryRepository.findChildCategories(tenantId, parentIdLong);
    }

    public List<Category> getCategoryHierarchy(UUID tenantId) {
        List<Category> rootCategories = getRootCategories(tenantId);
        return buildCategoryTree(rootCategories, tenantId);
    }

    public List<Category> searchCategories(UUID tenantId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAllCategoriesByTenant(tenantId);
        }
        return categoryRepository.searchByTenantIdAndKeyword(tenantId, keyword.trim());
    }

    public Category createCategory(Category category, UUID tenantId) {
        validateCategory(category, tenantId);
        
        // Check category limit before creating
        int currentCategoryCount = getAllCategoriesByTenant(tenantId).size();
        featureFlagService.enforceCategoryLimit(tenantId, currentCategoryCount);
        
        category.setTenantId(tenantId);
        category.setSlug(generateSlug(category.getName(), tenantId));
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        // deletedAt is null by default for new categories
        
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }
        
        if (category.getSortOrder() == null) {
            category.setSortOrder(getNextSortOrder(tenantId, category.getParentId()));
        }
        
        log.info("Creating category: {} for tenant: {}", category.getName(), tenantId);
        return categoryRepository.save(category);
    }

    public Category updateCategory(UUID id, Category categoryUpdates, UUID tenantId) {
        Category existingCategory = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        validateCategoryUpdate(categoryUpdates, tenantId, id);
        
        // Prevent circular references
        if (categoryUpdates.getParentId() != null && 
            isCircularReference(id, categoryUpdates.getParentId(), tenantId)) {
            throw new IllegalArgumentException("Cannot set parent category - would create circular reference");
        }
        
        // Update fields
        if (StringUtils.hasText(categoryUpdates.getName())) {
            existingCategory.setName(categoryUpdates.getName());
            // Update slug if name changed
            if (!existingCategory.getName().equals(categoryUpdates.getName())) {
                existingCategory.setSlug(generateSlug(categoryUpdates.getName(), tenantId));
            }
        }
        
        if (StringUtils.hasText(categoryUpdates.getDescription())) {
            existingCategory.setDescription(categoryUpdates.getDescription());
        }
        
        if (categoryUpdates.getParentId() != null) {
            existingCategory.setParentId(categoryUpdates.getParentId());
        }
        
        if (categoryUpdates.getSortOrder() != null) {
            existingCategory.setSortOrder(categoryUpdates.getSortOrder());
        }
        
        if (categoryUpdates.getIsActive() != null) {
            existingCategory.setIsActive(categoryUpdates.getIsActive());
        }
        
        existingCategory.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating category: {} for tenant: {}", existingCategory.getName(), tenantId);
        return categoryRepository.save(existingCategory);
    }

    public void deleteCategory(UUID id, UUID tenantId) {
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        // Check if category has children - convert UUID to Long for the query
        // Note: This assumes category IDs can be converted to Long, which may need revision
        long childCount = categoryRepository.countChildCategories(tenantId, Long.parseLong(id.toString().replace("-", "").substring(0, 10)));
        if (childCount > 0) {
            throw new IllegalStateException("Cannot delete category with child categories. Delete children first.");
        }
        
        category.setDeletedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        
        log.info("Deleting category: {} for tenant: {}", category.getName(), tenantId);
        categoryRepository.save(category);
    }

    public Category updateCategoryStatus(UUID id, Boolean isActive, UUID tenantId) {
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        category.setIsActive(isActive);
        category.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating category status: {} to {} for tenant: {}", category.getName(), isActive, tenantId);
        return categoryRepository.save(category);
    }

    public List<Category> reorderCategories(UUID tenantId, UUID parentId, List<UUID> categoryIds) {
        List<Category> categories = new ArrayList<>();
        
        for (int i = 0; i < categoryIds.size(); i++) {
            UUID categoryId = categoryIds.get(i);
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            
            category.setSortOrder(i + 1);
            category.setUpdatedAt(LocalDateTime.now());
            categories.add(categoryRepository.save(category));
        }
        
        log.info("Reordered {} categories for tenant: {}", categories.size(), tenantId);
        return categories;
    }

    public long getCategoryCount(UUID tenantId) {
        return categoryRepository.findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId).size();
    }

    public long getActiveCategoryCount(UUID tenantId) {
        return categoryRepository.findByTenantIdAndIsActiveAndDeletedAtIsNullOrderBySortOrderAsc(tenantId, true).size();
    }

    public long getRootCategoryCount(UUID tenantId) {
        return categoryRepository.findRootCategories(tenantId).size();
    }

    public List<Category> getDirectChildren(UUID tenantId, UUID parentId) {
        return getChildCategories(tenantId, parentId);
    }

    public List<Category> reorderCategories(UUID tenantId, List<Map<String, Object>> reorderData) {
        List<Category> categories = new ArrayList<>();
        
        for (int i = 0; i < reorderData.size(); i++) {
            Map<String, Object> data = reorderData.get(i);
            UUID categoryId = UUID.fromString(data.get("id").toString());
            
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            
            category.setSortOrder(i + 1);
            category.setUpdatedAt(LocalDateTime.now());
            categories.add(categoryRepository.save(category));
        }
        
        log.info("Reordered {} categories for tenant: {}", categories.size(), tenantId);
        return categories;
    }

    public Page<Category> getAllCategoriesByTenant(UUID tenantId, Pageable pageable) {
        List<Category> categories = getAllCategoriesByTenant(tenantId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), categories.size());
        return new PageImpl<>(categories.subList(start, end), pageable, categories.size());
    }

    public Page<Category> searchCategories(UUID tenantId, String keyword, Pageable pageable) {
        List<Category> categories = searchCategories(tenantId, keyword);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), categories.size());
        return new PageImpl<>(categories.subList(start, end), pageable, categories.size());
    }

    public Page<Category> getChildCategories(UUID tenantId, Long parentId, Pageable pageable) {
        List<Category> categories = categoryRepository.findByTenantIdAndParentIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId, parentId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), categories.size());
        return new PageImpl<>(categories.subList(start, end), pageable, categories.size());
    }

    public Page<Category> getCategoriesByStatus(UUID tenantId, Boolean isActive, Pageable pageable) {
        List<Category> categories = categoryRepository.findByTenantIdAndIsActiveAndDeletedAtIsNullOrderBySortOrderAsc(tenantId, isActive);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), categories.size());
        return new PageImpl<>(categories.subList(start, end), pageable, categories.size());
    }

    public List<Category> getCategoryTree(UUID tenantId) {
        return getCategoryHierarchy(tenantId);
    }

    private List<Category> buildCategoryTree(List<Category> categories, UUID tenantId) {
        for (Category category : categories) {
            List<Category> children = getChildCategories(tenantId, category.getId());
            if (!children.isEmpty()) {
                category.setChildren(buildCategoryTree(children, tenantId));
            }
        }
        return categories;
    }

    private void validateCategory(Category category, UUID tenantId) {
        if (!StringUtils.hasText(category.getName())) {
            throw new IllegalArgumentException("Category name is required");
        }
        
        // Validate parent exists if specified
        if (category.getParentId() != null) {
            categoryRepository.findByTenantIdAndParentIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId, category.getParentId())
                    .stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }
    }

    private void validateCategoryUpdate(Category category, UUID tenantId, UUID categoryId) {
        validateCategory(category, tenantId);
        
        // Check slug uniqueness if provided
        if (StringUtils.hasText(category.getName())) {
            String newSlug = generateSlug(category.getName(), tenantId);
            if (categoryRepository.existsBySlugAndTenantIdAndIdNotAndDeletedAtIsNull(newSlug, tenantId, categoryId)) {
                throw new IllegalArgumentException("Category with this name already exists");
            }
        }
    }

    private boolean isCircularReference(UUID categoryId, Long parentId, UUID tenantId) {
        if (parentId == null) {
            return false;
        }
        
        // Find the parent category by converting parentId to UUID for lookup
        Optional<Category> parent = categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(UUID.fromString(parentId.toString()), tenantId);
        if (parent.isPresent()) {
            if (parent.get().getId().equals(categoryId)) {
                return true;
            }
            if (parent.get().getParentId() != null) {
                return isCircularReference(categoryId, parent.get().getParentId(), tenantId);
            }
        }
        
        return false;
    }

    private String generateSlug(String name, UUID tenantId) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        String slug = baseSlug;
        int counter = 1;
        
        while (categoryRepository.existsBySlugAndTenantIdAndDeletedAtIsNull(slug, tenantId)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        return slug;
    }

    private Integer getNextSortOrder(UUID tenantId, Long parentId) {
        List<Category> siblings = parentId == null ? 
                getRootCategories(tenantId) : 
                categoryRepository.findByTenantIdAndParentIdAndDeletedAtIsNullOrderBySortOrderAsc(tenantId, parentId);
        
        return siblings.stream()
                .mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }
}
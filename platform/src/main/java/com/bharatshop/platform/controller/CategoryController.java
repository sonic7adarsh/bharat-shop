package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.CategoryService;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long parentId,
            Authentication authentication) {
        
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Category> categories;
        
        if (search != null && !search.trim().isEmpty()) {
            categories = categoryService.searchCategories(tenantId, search, pageable);
        } else if (parentId != null) {
            categories = categoryService.getChildCategories(tenantId, parentId, pageable);
        } else if (isActive != null) {
            categories = categoryService.getCategoriesByStatus(tenantId, isActive, pageable);
        } else {
            categories = categoryService.getAllCategoriesByTenant(tenantId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("categories", categories.getContent());
        response.put("currentPage", categories.getNumber());
        response.put("totalItems", categories.getTotalElements());
        response.put("totalPages", categories.getTotalPages());
        response.put("hasNext", categories.hasNext());
        response.put("hasPrevious", categories.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree")
    public ResponseEntity<List<Category>> getCategoryTree(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<Category> categoryTree = categoryService.getCategoryTree(tenantId);
        return ResponseEntity.ok(categoryTree);
    }

    @GetMapping("/root")
    public ResponseEntity<List<Category>> getRootCategories(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<Category> rootCategories = categoryService.getRootCategories(tenantId);
        return ResponseEntity.ok(rootCategories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable UUID id, Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Optional<Category> category = categoryService.getCategoryById(id, tenantId);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Category> getCategoryBySlug(@PathVariable String slug, Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Optional<Category> category = categoryService.getCategoryBySlug(slug, tenantId);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<Category>> getCategoryChildren(
            @PathVariable UUID id, 
            Authentication authentication) {
        
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<Category> children = categoryService.getDirectChildren(tenantId, id);
        return ResponseEntity.ok(children);
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category, Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Category createdCategory = categoryService.createCategory(category, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable UUID id, 
            @RequestBody Category category, 
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Category updatedCategory = categoryService.updateCategory(id, category, tenantId);
            return ResponseEntity.ok(updatedCategory);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("circular reference")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            categoryService.deleteCategory(id, tenantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("has children")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Category> updateCategoryStatus(
            @PathVariable UUID id, 
            @RequestBody Map<String, Boolean> statusUpdate, 
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Boolean isActive = statusUpdate.get("isActive");
            
            if (isActive == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Category updatedCategory = categoryService.updateCategoryStatus(id, isActive, tenantId);
            return ResponseEntity.ok(updatedCategory);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<Category>> reorderCategories(
            @RequestBody List<Map<String, Object>> reorderData, 
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            List<Category> reorderedCategories = categoryService.reorderCategories(tenantId, reorderData);
            return ResponseEntity.ok(reorderedCategories);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCategoryStats(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        // Enforce analytics feature access
        featureFlagService.enforceFeatureAccess(tenantId, "analytics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCategories", categoryService.getCategoryCount(tenantId));
        stats.put("activeCategories", categoryService.getActiveCategoryCount(tenantId));
        stats.put("rootCategories", categoryService.getRootCategoryCount(tenantId));
        
        return ResponseEntity.ok(stats);
    }

    private UUID getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return UUID.fromString("00000000-0000-0000-0000-000000000001"); // For now, return a default tenant ID
    }
}
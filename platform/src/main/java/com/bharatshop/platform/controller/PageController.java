package com.bharatshop.platform.controller;

import com.bharatshop.platform.shared.ApiResponse;
import com.bharatshop.shared.dto.PageRequestDto;
import com.bharatshop.shared.dto.PageResponseDto;
import com.bharatshop.shared.service.PageService;
import com.bharatshop.shared.service.FeatureFlagService;
import com.bharatshop.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
// import java.util.UUID; // Replaced with Long

/**
 * Controller for managing pages in the platform.
 * Provides APIs for vendors to customize and manage pages.
 */
@RestController
@RequestMapping("/api/platform/pages")
@Tag(name = "Page Management", description = "APIs for page customization and management")
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);
    
    private final PageService pageService;
    private final FeatureFlagService featureFlagService;
    
    public PageController(@Qualifier("sharedPageService") PageService pageService, FeatureFlagService featureFlagService) {
        this.pageService = pageService;
        this.featureFlagService = featureFlagService;
    }
    
    /**
     * Get all pages with pagination
     * GET /api/platform/pages
     */
    @GetMapping
    @Operation(summary = "Get all pages", description = "Retrieve all pages with pagination")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<PageResponseDto>>> getAllPages(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, 
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
            
            org.springframework.data.domain.Page<PageResponseDto> pages = pageService.getAllPages(pageable);
            
            return ResponseEntity.ok(ApiResponse.success(pages));
            
        } catch (Exception e) {
            log.error("Error fetching pages", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching pages: " + e.getMessage()));
        }
    }
    
    /**
     * Get page by ID
     * GET /api/platform/pages/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get page by ID", description = "Retrieve a specific page by its ID")
    public ResponseEntity<ApiResponse<PageResponseDto>> getPageById(
            @Parameter(description = "Page ID")
            @PathVariable Long id) {
        
        try {
            return pageService.getPageById(id)
                    .map(page -> ResponseEntity.ok(ApiResponse.success(page)))
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (Exception e) {
            log.error("Error fetching page by ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching page: " + e.getMessage()));
        }
    }
    
    /**
     * Get pages by type
     * GET /api/platform/pages/type/{pageType}
     */
    @GetMapping("/type/{pageType}")
    @Operation(summary = "Get pages by type", description = "Retrieve pages filtered by page type")
    public ResponseEntity<ApiResponse<List<PageResponseDto>>> getPagesByType(
            @Parameter(description = "Page type")
            @PathVariable com.bharatshop.shared.enums.PageType pageType) {
        
        try {
            Long tenantId = TenantContext.getCurrentTenant();
            
            List<PageResponseDto> pages = pageService.getPagesByType(pageType, tenantId);
            return ResponseEntity.ok(ApiResponse.success(pages));
            
        } catch (Exception e) {
            log.error("Error fetching pages by type: {}", pageType, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching pages: " + e.getMessage()));
        }
    }
    
    /**
     * Create new page
     * POST /api/platform/pages
     */
    @PostMapping
    @Operation(summary = "Create page", description = "Create a new page")
    public ResponseEntity<ApiResponse<PageResponseDto>> createPage(
            @Parameter(description = "Page data")
            @Valid @RequestBody PageRequestDto pageRequest) {
        
        try {
            Long tenantId =  TenantContext.getCurrentTenant();
            
            // Enforce advanced features access for page creation
            featureFlagService.enforceFeatureAccess(tenantId, "advancedFeatures");
            
            PageResponseDto createdPage = pageService.createPage(pageRequest, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdPage));
                    
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error creating page", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error creating page: " + e.getMessage()));
        }
    }
    
    /**
     * Update existing page
     * PUT /api/platform/pages/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update page", description = "Update an existing page")
    public ResponseEntity<ApiResponse<PageResponseDto>> updatePage(
            @Parameter(description = "Page ID")
            @PathVariable Long id,
            
            @Parameter(description = "Updated page data")
            @Valid @RequestBody PageRequestDto pageRequest) {
        
        try {
            Long tenantId =  TenantContext.getCurrentTenant();
            
            // Enforce advanced features access for page updates
            featureFlagService.enforceFeatureAccess(tenantId, "advancedFeatures");
            
            PageResponseDto updatedPage = pageService.updatePage(id, pageRequest, tenantId);
            return ResponseEntity.ok(ApiResponse.success(updatedPage));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid page data or ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error updating page with ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error updating page: " + e.getMessage()));
        }
    }
    
    /**
     * Update page layout (for template customization)
     * PATCH /api/platform/pages/{id}/layout
     */
    @PatchMapping("/{id}/layout")
    @Operation(summary = "Update page layout", description = "Update page layout configuration for template customization")
    public ResponseEntity<ApiResponse<PageResponseDto>> updatePageLayout(
            @Parameter(description = "Page ID")
            @PathVariable Long id,
            
            @Parameter(description = "Layout JSON configuration")
            @RequestBody Map<String, Object> request) {
        
        try {
            String layoutJson = request.get("layout").toString();
            PageResponseDto updatedPage = pageService.updatePageLayout(id, layoutJson);
            return ResponseEntity.ok(ApiResponse.success(updatedPage));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid layout data or page ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error updating page layout for ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error updating page layout: " + e.getMessage()));
        }
    }
    
    /**
     * Update page SEO settings
     * PATCH /api/platform/pages/{id}/seo
     */
    @PatchMapping("/{id}/seo")
    @Operation(summary = "Update page SEO", description = "Update page SEO configuration")
    public ResponseEntity<ApiResponse<PageResponseDto>> updatePageSeo(
            @Parameter(description = "Page ID")
            @PathVariable Long id,
            
            @Parameter(description = "SEO JSON configuration")
            @RequestBody Map<String, Object> request) {
        
        try {
            String seoJson = request.get("seo").toString();
            PageResponseDto updatedPage = pageService.updatePageSeo(id, seoJson);
            return ResponseEntity.ok(ApiResponse.success(updatedPage));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid SEO data or page ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error updating page SEO for ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error updating page SEO: " + e.getMessage()));
        }
    }
    
    /**
     * Toggle page publish status
     * PATCH /api/platform/pages/{id}/toggle-publish
     */
    @PatchMapping("/{id}/toggle-publish")
    @Operation(summary = "Toggle page publish status", description = "Publish or unpublish a page")
    public ResponseEntity<ApiResponse<PageResponseDto>> togglePagePublishStatus(
            @Parameter(description = "Page ID")
            @PathVariable Long id) {
        
        try {
            PageResponseDto page = pageService.togglePagePublishStatus(id);
            return ResponseEntity.ok(ApiResponse.success(page));
            
        } catch (IllegalArgumentException e) {
            log.warn("Page not found with ID: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error toggling page publish status for ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error toggling page publish status: " + e.getMessage()));
        }
    }
    
    /**
     * Delete page
     * DELETE /api/platform/pages/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete page", description = "Soft delete a page")
    public ResponseEntity<ApiResponse<Void>> deletePage(
            @Parameter(description = "Page ID")
            @PathVariable Long id) {
        
        try {
            pageService.deletePage(id);
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (IllegalArgumentException e) {
            log.warn("Page not found with ID: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting page with ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting page: " + e.getMessage()));
        }
    }
}
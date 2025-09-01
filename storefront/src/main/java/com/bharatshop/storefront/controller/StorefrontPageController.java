package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.PageResponseDto;
import com.bharatshop.storefront.service.StorefrontPageService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public storefront API controller for CMS pages.
 * Provides tenant-scoped content management functionality for customers.
 */
@RestController
@RequestMapping("/store")
@Slf4j
@Tag(name = "Storefront Pages", description = "Public APIs for CMS page content")
public class StorefrontPageController {
    
    private final StorefrontPageService pageService;
    
    public StorefrontPageController(@Qualifier("storefrontPageService") StorefrontPageService pageService) {
        this.pageService = pageService;
    }
    
    /**
     * Get page by slug
     * GET /store/pages/{slug}
     */
    @GetMapping("/pages/{slug}")
    @Operation(summary = "Get page by slug", description = "Retrieve a CMS page by its slug")
    public ResponseEntity<ApiResponse<PageResponseDto>> getPageBySlug(
            @Parameter(description = "Page slug")
            @PathVariable String slug,
            
            @Parameter(description = "Tenant ID header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            
            @Parameter(description = "Tenant domain header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            log.info("Fetching page by slug: {} for tenant: {}", slug, tenantIdHeader != null ? tenantIdHeader : tenantDomain);
            
            PageResponseDto page;
            
            // If tenant ID is provided, use tenant-aware lookup
            if (tenantIdHeader != null && !tenantIdHeader.trim().isEmpty()) {
                try {
                    UUID tenantId = UUID.fromString(tenantIdHeader.trim());
                    page = pageService.getPageBySlug(slug, tenantId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant ID format: {}", tenantIdHeader);
                    // Fallback to non-tenant lookup
                    page = pageService.getPageBySlug(slug);
                }
            } else {
                // Fallback to non-tenant lookup
                page = pageService.getPageBySlug(slug);
            }
            
            return ResponseEntity.ok(ApiResponse.success(page));
            
        } catch (RuntimeException e) {
            log.warn("Page not found with slug: {}", slug);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching page by slug: {}", slug, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching page: " + e.getMessage()));
        }
    }
    
    /**
     * Get all pages (optional endpoint for navigation)
     * GET /store/pages
     */
    @GetMapping("/pages")
    @Operation(summary = "Get all pages", description = "Retrieve all published CMS pages")
    public ResponseEntity<ApiResponse<List<PageResponseDto>>> getAllPages(
            @Parameter(description = "Tenant ID header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            
            @Parameter(description = "Tenant domain header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            log.info("Fetching all pages for tenant: {}", tenantIdHeader != null ? tenantIdHeader : tenantDomain);
            
            List<PageResponseDto> pages;
            
            // If tenant ID is provided, use tenant-aware lookup
            if (tenantIdHeader != null && !tenantIdHeader.trim().isEmpty()) {
                try {
                    UUID tenantId = UUID.fromString(tenantIdHeader.trim());
                    pages = pageService.getAllPages(tenantId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant ID format: {}", tenantIdHeader);
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid tenant ID format"));
                }
            } else {
                // Return empty list if no tenant specified
                return ResponseEntity.ok(ApiResponse.success(List.of()));
            }
            
            return ResponseEntity.ok(ApiResponse.success(pages));
            
        } catch (Exception e) {
            log.error("Error fetching pages", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching pages: " + e.getMessage()));
        }
    }
    
    /**
     * Get page render data with template and layout
     * GET /store/pages/{slug}/render
     */
    @GetMapping("/pages/{slug}/render")
    @Operation(summary = "Get page render data", description = "Retrieve page with template and layout configuration for storefront rendering")
    public ResponseEntity<ApiResponse<StorefrontPageService.PageRenderData>> getPageRenderData(
            @Parameter(description = "Page slug")
            @PathVariable String slug,
            
            @Parameter(description = "Tenant ID header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            
            @Parameter(description = "Tenant domain header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            log.info("Fetching page render data for slug: {} and tenant: {}", slug, tenantIdHeader != null ? tenantIdHeader : tenantDomain);
            
            // Set tenant context if provided
            if (tenantIdHeader != null && !tenantIdHeader.trim().isEmpty()) {
                try {
                    UUID tenantId = UUID.fromString(tenantIdHeader.trim());
                    // Note: You might need to set tenant context here if using TenantContext
                    // TenantContext.setCurrentTenant(tenantId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant ID format: {}", tenantIdHeader);
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid tenant ID format"));
                }
            }
            
            return pageService.getPageRenderData(slug)
                    .map(renderData -> ResponseEntity.ok(ApiResponse.success(renderData)))
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (Exception e) {
            log.error("Error fetching page render data for slug: {}", slug, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching page render data: " + e.getMessage()));
        }
    }
}
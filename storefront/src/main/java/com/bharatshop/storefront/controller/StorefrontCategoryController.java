package com.bharatshop.storefront.controller;

import com.bharatshop.shared.dto.ApiResponse;
import com.bharatshop.shared.dto.CategoryResponseDto;
import com.bharatshop.shared.service.HttpCacheService;
import com.bharatshop.storefront.service.StorefrontCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Public storefront API controller for customer-facing category operations.
 * Provides tenant-scoped category browsing functionality.
 */
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storefront Categories", description = "Public APIs for customer category browsing")
public class StorefrontCategoryController {
    
    private final StorefrontCategoryService storefrontCategoryService;
    private final HttpCacheService httpCacheService;
    
    /**
     * Get all available categories
     * GET /store/categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Get categories", description = "Retrieve all available product categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategories(
            @Parameter(description = "Tenant domain header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain,
            HttpServletRequest request) {
        
        try {
            log.info("Fetching all categories for tenant: {}", tenantDomain);
            
            List<CategoryResponseDto> categories = storefrontCategoryService.getCustomerCategories();
            
            ApiResponse<List<CategoryResponseDto>> response = ApiResponse.success(categories);
            
            // Check for conditional requests
            String etag = httpCacheService.generateETag(response);
            if (httpCacheService.hasMatchingETag(request, etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .build();
            }
            
            // Return with caching headers (longer cache for categories)
            return httpCacheService.createCachedResponse(response, HttpCacheService.CacheConfig.longTerm());
            
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching categories: " + e.getMessage()));
        }
    }
}
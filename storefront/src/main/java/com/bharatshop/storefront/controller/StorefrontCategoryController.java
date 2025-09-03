package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.service.StorefrontProductService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Public storefront API controller for customer-facing category operations.
 * Provides tenant-scoped category browsing functionality.
 */
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Tag(name = "Storefront Categories", description = "Public APIs for customer category browsing")
public class StorefrontCategoryController {
    
    private static final Logger log = LoggerFactory.getLogger(StorefrontCategoryController.class);
    
    private final StorefrontProductService storefrontProductService;
    
    /**
     * Get all available categories
     * GET /store/categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Get categories", description = "Retrieve all available product categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories(
            @Parameter(description = "Tenant domain header for multi-tenancy")
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            log.info("Fetching all categories for tenant: {}", tenantDomain);
            
            List<String> categories = storefrontProductService.getCustomerCategories();
            
            return ResponseEntity.ok(ApiResponse.success(categories));
            
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching categories: " + e.getMessage()));
        }
    }
}
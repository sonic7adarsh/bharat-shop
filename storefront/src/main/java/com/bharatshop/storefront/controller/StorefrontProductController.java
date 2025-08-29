package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.ProductResponseDto;
import com.bharatshop.storefront.service.StorefrontProductService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public storefront API controller for customer-facing product operations.
 * Provides tenant-scoped product browsing and search functionality.
 */
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storefront Products", description = "Public APIs for customer product browsing")
public class StorefrontProductController {
    
    private final StorefrontProductService storefrontProductService;
    
    /**
     * Get products with filtering and pagination
     * GET /store/products?search=&category=&minPrice=&maxPrice=&page=
     */
    @GetMapping("/products")
    @Operation(summary = "Browse products", description = "Get paginated list of products with optional filters")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProducts(
            @Parameter(description = "Search term for product name/description")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir,
            
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            // Create pageable with sorting
            Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Convert BigDecimal to Double for service compatibility
            Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
            Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;
            
            log.info("Fetching products - search: {}, category: {}, minPrice: {}, maxPrice: {}, page: {}, size: {}", 
                    search, category, minPriceDouble, maxPriceDouble, page, size);
            
            Page<ProductResponseDto> products = storefrontProductService.getCustomerProducts(
                    category, search, minPriceDouble, maxPriceDouble, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(products));
            
        } catch (Exception e) {
            log.error("Error fetching products", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching products: " + e.getMessage()));
        }
    }
    
    /**
     * Get product by slug
     * GET /store/products/{slug}
     */
    @GetMapping("/products/{slug}")
    @Operation(summary = "Get product by slug", description = "Retrieve a specific product by its slug")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySlug(
            @Parameter(description = "Product slug")
            @PathVariable String slug,
            
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            log.info("Fetching product by slug: {}", slug);
            
            ProductResponseDto product = storefrontProductService.getCustomerProductBySlug(slug);
            
            return ResponseEntity.ok(ApiResponse.success(product));
            
        } catch (RuntimeException e) {
            log.warn("Product not found with slug: {}", slug);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching product by slug: {}", slug, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching product: " + e.getMessage()));
        }
    }
    
    /**
     * Advanced search products with multiple filters
     * GET /store/search
     */
    @GetMapping("/search")
    @Operation(summary = "Advanced product search", description = "Search products with advanced filtering options")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @Parameter(description = "Search query")
            @RequestParam String query,
            
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Filter by brand")
            @RequestParam(required = false) String brand,
            
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) Double minPrice,
            
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) Double maxPrice,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir,
            
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, 
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
            
            Page<ProductResponseDto> products;
            
            if (category != null && !category.isEmpty()) {
                products = storefrontProductService.searchCustomerProductsByCategory(query, category, pageable);
            } else if (brand != null && !brand.isEmpty()) {
                products = storefrontProductService.getCustomerProductsByBrand(brand, query, pageable);
            } else if (minPrice != null && maxPrice != null) {
                products = storefrontProductService.getCustomerProductsByPriceRange(minPrice, maxPrice, query, pageable);
            } else {
                products = storefrontProductService.searchCustomerProducts(query, pageable);
            }
            
            return ResponseEntity.ok(ApiResponse.success(products));
            
        } catch (Exception e) {
            log.error("Error searching products", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error searching products: " + e.getMessage()));
        }
    }
    
    /**
     * Get all available brands
     * GET /store/brands
     */
    @GetMapping("/brands")
    @Operation(summary = "Get brands", description = "Retrieve all available product brands")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getBrands(
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            java.util.List<String> brands = storefrontProductService.getCustomerBrands();
            return ResponseEntity.ok(ApiResponse.success(brands));
            
        } catch (Exception e) {
            log.error("Error fetching brands", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching brands: " + e.getMessage()));
        }
    }
    
    /**
     * Get top-rated products
     * GET /store/top-rated
     */
    @GetMapping("/top-rated")
    @Operation(summary = "Get top-rated products", description = "Retrieve products with high ratings")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getTopRatedProducts(
            @Parameter(description = "Minimum rating threshold")
            @RequestParam(defaultValue = "4.0") Double minRating,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenantDomain) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponseDto> products = storefrontProductService.getCustomerTopRatedProducts(minRating, pageable);
            return ResponseEntity.ok(ApiResponse.success(products));
            
        } catch (Exception e) {
            log.error("Error fetching top-rated products", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching top-rated products: " + e.getMessage()));
        }
    }
}
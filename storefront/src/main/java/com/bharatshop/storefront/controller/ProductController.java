package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.ProductResponseDto;
import com.bharatshop.storefront.service.ProductService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "APIs for browsing products in the storefront")
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve a paginated list of all active products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Pageable pageable) {
        Page<ProductResponseDto> products = productService.getAllProducts(category, search, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable UUID id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
    
    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieve a list of featured products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getFeaturedProducts() {
        List<ProductResponseDto> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/categories")
    @Operation(summary = "Get product categories", description = "Retrieve all available product categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = productService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name, description, or category")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @RequestParam String query,
            Pageable pageable) {
        Page<ProductResponseDto> products = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
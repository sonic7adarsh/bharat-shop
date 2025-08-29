package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.ProductService;
import com.bharatshop.shared.entity.Product;
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
@RequestMapping("/api/platform/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Product.ProductStatus status,
            Authentication authentication) {
        
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products;
        
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(tenantId, search, pageable);
        } else if (status != null) {
            products = productService.getProductsByStatus(tenantId, status, pageable);
        } else {
            products = productService.getAllProductsByTenant(tenantId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", products.getContent());
        response.put("currentPage", products.getNumber());
        response.put("totalItems", products.getTotalElements());
        response.put("totalPages", products.getTotalPages());
        response.put("hasNext", products.hasNext());
        response.put("hasPrevious", products.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id, Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Optional<Product> product = productService.getProductById(id, tenantId);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Product> getProductBySlug(@PathVariable String slug, Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Optional<Product> product = productService.getProductBySlug(slug, tenantId);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable UUID categoryId, 
            Authentication authentication) {
        
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<Product> products = productService.getProductsByCategory(tenantId, categoryId);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product, Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Product createdProduct = productService.createProduct(product, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable UUID id, 
            @RequestBody Product product, 
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Product updatedProduct = productService.updateProduct(id, product, tenantId);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            productService.deleteProduct(id, tenantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Product> updateProductStatus(
            @PathVariable UUID id, 
            @RequestBody Map<String, String> statusUpdate, 
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            String statusStr = statusUpdate.get("status");
            
            if (statusStr == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Product.ProductStatus status = Product.ProductStatus.valueOf(statusStr.toUpperCase());
            Product updatedProduct = productService.updateProductStatus(id, status, tenantId);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProductStats(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productService.getProductCount(tenantId));
        stats.put("activeProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.ACTIVE));
        stats.put("draftProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.DRAFT));
        stats.put("inactiveProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.INACTIVE));
        stats.put("outOfStockProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.OUT_OF_STOCK));
        
        return ResponseEntity.ok(stats);
    }

    private UUID getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440000"); // For now, return a default tenant ID
    }
}
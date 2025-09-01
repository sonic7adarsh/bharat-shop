package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.ProductService;
import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.OptionService;
import com.bharatshop.platform.service.OptionValueService;
import com.bharatshop.platform.service.ProductOptionService;
import com.bharatshop.shared.dto.*;
import com.bharatshop.shared.entity.Product;
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

import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@RestController
@RequestMapping("/api/platform/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final OptionService optionService;
    private final OptionValueService optionValueService;
    private final ProductOptionService productOptionService;
    private final FeatureFlagService featureFlagService;

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
        
        // Enforce analytics feature access
        featureFlagService.enforceFeatureAccess(tenantId, "analytics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productService.getProductCount(tenantId));
        stats.put("activeProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.ACTIVE));
        stats.put("draftProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.DRAFT));
        stats.put("inactiveProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.INACTIVE));
        stats.put("outOfStockProducts", productService.getProductCountByStatus(tenantId, Product.ProductStatus.OUT_OF_STOCK));
        
        return ResponseEntity.ok(stats);
    }

    // Product Variant endpoints
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantDto>> getProductVariants(
            @PathVariable UUID productId,
            Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<ProductVariantDto> variants = productVariantService.getVariantsByProduct(productId, tenantId);
        return ResponseEntity.ok(variants);
    }

    @GetMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ProductVariantDto> getProductVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        Optional<ProductVariantDto> variant = productVariantService.getVariantById(variantId, tenantId);
        return variant.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ProductVariantDto> createProductVariant(
            @PathVariable UUID productId,
            @RequestBody ProductVariantDto variantDto,
            @RequestParam(required = false) Map<String, String> optionValues,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            variantDto.setProductId(productId);
            
            Map<UUID, UUID> optionValueMap = null;
            if (optionValues != null && !optionValues.isEmpty()) {
                optionValueMap = new HashMap<>();
                for (Map.Entry<String, String> entry : optionValues.entrySet()) {
                    optionValueMap.put(UUID.fromString(entry.getKey()), UUID.fromString(entry.getValue()));
                }
            }
            
            ProductVariantDto createdVariant = productVariantService.createVariant(variantDto, optionValueMap, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdVariant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ProductVariantDto> updateProductVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @RequestBody ProductVariantDto variantDto,
            @RequestParam(required = false) Map<String, String> optionValues,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            
            Map<UUID, UUID> optionValueMap = null;
            if (optionValues != null && !optionValues.isEmpty()) {
                optionValueMap = new HashMap<>();
                for (Map.Entry<String, String> entry : optionValues.entrySet()) {
                    optionValueMap.put(UUID.fromString(entry.getKey()), UUID.fromString(entry.getValue()));
                }
            }
            
            ProductVariantDto updatedVariant = productVariantService.updateVariant(variantId, variantDto, optionValueMap, tenantId);
            return ResponseEntity.ok(updatedVariant);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<Void> deleteProductVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            productVariantService.deleteVariant(variantId, tenantId);
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

    @PatchMapping("/{productId}/variants/{variantId}/default")
    public ResponseEntity<ProductVariantDto> setDefaultVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            ProductVariantDto updatedVariant = productVariantService.setDefaultVariant(variantId, tenantId);
            return ResponseEntity.ok(updatedVariant);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/{productId}/variants/{variantId}/stock")
    public ResponseEntity<ProductVariantDto> updateVariantStock(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @RequestBody Map<String, Integer> stockUpdate,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Integer stock = stockUpdate.get("stock");
            
            if (stock == null) {
                return ResponseEntity.badRequest().build();
            }
            
            ProductVariantDto updatedVariant = productVariantService.updateStock(variantId, stock, tenantId);
            return ResponseEntity.ok(updatedVariant);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Product Option endpoints
    @GetMapping("/{productId}/options")
    public ResponseEntity<List<ProductOptionDto>> getProductOptions(
            @PathVariable UUID productId,
            Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<ProductOptionDto> options = productOptionService.getProductOptions(productId, tenantId);
        return ResponseEntity.ok(options);
    }

    @PostMapping("/{productId}/options/{optionId}")
    public ResponseEntity<ProductOptionDto> addOptionToProduct(
            @PathVariable UUID productId,
            @PathVariable UUID optionId,
            @RequestParam(defaultValue = "false") boolean isRequired,
            @RequestParam(defaultValue = "0") Integer sortOrder,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            ProductOptionDto productOption = productOptionService.addOptionToProduct(productId, optionId, isRequired, sortOrder, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(productOption);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{productId}/options/{optionId}")
    public ResponseEntity<Void> removeOptionFromProduct(
            @PathVariable UUID productId,
            @PathVariable UUID optionId,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            productOptionService.removeOptionFromProduct(productId, optionId, tenantId);
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

    // Global Option endpoints
    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getAllOptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        
        UUID tenantId = getTenantIdFromAuth(authentication);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<OptionDto> options;
        
        if (search != null && !search.trim().isEmpty()) {
            options = optionService.searchOptions(tenantId, search, pageable);
        } else {
            options = optionService.getAllOptions(tenantId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("options", options.getContent());
        response.put("currentPage", options.getNumber());
        response.put("totalItems", options.getTotalElements());
        response.put("totalPages", options.getTotalPages());
        response.put("hasNext", options.hasNext());
        response.put("hasPrevious", options.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/options")
    public ResponseEntity<OptionDto> createOption(
            @RequestBody OptionDto optionDto,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            OptionDto createdOption = optionService.createOption(optionDto, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOption);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/options/{optionId}")
    public ResponseEntity<OptionDto> getOption(
            @PathVariable UUID optionId,
            Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        Optional<OptionDto> option = optionService.getOptionById(optionId, tenantId);
        return option.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<OptionDto> updateOption(
            @PathVariable UUID optionId,
            @RequestBody OptionDto optionDto,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            OptionDto updatedOption = optionService.updateOption(optionId, optionDto, tenantId);
            return ResponseEntity.ok(updatedOption);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(
            @PathVariable UUID optionId,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            optionService.deleteOption(optionId, tenantId);
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

    // Option Value endpoints
    @GetMapping("/options/{optionId}/values")
    public ResponseEntity<List<OptionValueDto>> getOptionValues(
            @PathVariable UUID optionId,
            Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<OptionValueDto> optionValues = optionValueService.getOptionValuesByOption(optionId, tenantId);
        return ResponseEntity.ok(optionValues);
    }

    @PostMapping("/options/{optionId}/values")
    public ResponseEntity<OptionValueDto> createOptionValue(
            @PathVariable UUID optionId,
            @RequestBody OptionValueDto optionValueDto,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            optionValueDto.setOptionId(optionId);
            OptionValueDto createdOptionValue = optionValueService.createOptionValue(optionValueDto, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOptionValue);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/options/{optionId}/values/{valueId}")
    public ResponseEntity<OptionValueDto> updateOptionValue(
            @PathVariable UUID optionId,
            @PathVariable UUID valueId,
            @RequestBody OptionValueDto optionValueDto,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            OptionValueDto updatedOptionValue = optionValueService.updateOptionValue(valueId, optionValueDto, tenantId);
            return ResponseEntity.ok(updatedOptionValue);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/options/{optionId}/values/{valueId}")
    public ResponseEntity<Void> deleteOptionValue(
            @PathVariable UUID optionId,
            @PathVariable UUID valueId,
            Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            optionValueService.deleteOptionValue(valueId, tenantId);
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

    private UUID getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440000"); // For now, return a default tenant ID
    }
}
package com.bharatshop.platform.controller;


import com.bharatshop.platform.service.OptionService;
import com.bharatshop.platform.service.OptionValueService;
import com.bharatshop.platform.service.ProductOptionService;
import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.PlatformProductService;
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
// import java.util.UUID; // Replaced with Long

@RestController
@RequestMapping("/api/platform/products")
@RequiredArgsConstructor
public class ProductController {

    private final PlatformProductService platformProductService;
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
        
        Long tenantId = getTenantIdFromAuth(authentication);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products;
        
        if (search != null && !search.trim().isEmpty()) {
            products = platformProductService.searchProducts(tenantId, search, pageable);
        } else if (status != null) {
            products = platformProductService.getProductsByStatus(tenantId, status, pageable);
        } else {
            products = platformProductService.getAllProductsByTenant(tenantId, pageable);
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
    public ResponseEntity<Product> getProductById(@PathVariable Long id, Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        
        Optional<Product> product = platformProductService.getProductById(id, tenantId);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Product> getProductBySlug(@PathVariable String slug, Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        
        Optional<Product> product = platformProductService.getProductBySlug(slug, tenantId);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable Long categoryId, 
            Authentication authentication) {
        
        Long tenantId = getTenantIdFromAuth(authentication);
        List<Product> products = platformProductService.getProductsByCategory(tenantId, categoryId);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product, Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            Product createdProduct = platformProductService.createProduct(product, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id, 
            @RequestBody Product product, 
            Authentication authentication) {
        
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            Product updatedProduct = platformProductService.updateProduct(id, product, tenantId);
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
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            platformProductService.deleteProduct(id, tenantId);
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
            @PathVariable Long id, 
            @RequestBody Map<String, String> statusUpdate, 
            Authentication authentication) {
        
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            String statusStr = statusUpdate.get("status");
            
            if (statusStr == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Product.ProductStatus status = Product.ProductStatus.valueOf(statusStr.toUpperCase());
            Product updatedProduct = platformProductService.updateProductStatus(id, status, tenantId);
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
        Long tenantId = getTenantIdFromAuth(authentication);
        
        // Enforce analytics feature access
        featureFlagService.enforceFeatureAccess(tenantId, "analytics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", platformProductService.getProductCount(tenantId));
        stats.put("activeProducts", platformProductService.getProductCountByStatus(tenantId, Product.ProductStatus.ACTIVE));
        stats.put("draftProducts", platformProductService.getProductCountByStatus(tenantId, Product.ProductStatus.DRAFT));
        stats.put("inactiveProducts", platformProductService.getProductCountByStatus(tenantId, Product.ProductStatus.INACTIVE));
        stats.put("outOfStockProducts", platformProductService.getProductCountByStatus(tenantId, Product.ProductStatus.OUT_OF_STOCK));
        
        return ResponseEntity.ok(stats);
    }

    // Product Variant endpoints
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantDto>> getProductVariants(
            @PathVariable Long productId,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        List<ProductVariantDto> variants = productVariantService.getVariantsByProduct(productId, tenantId);
        return ResponseEntity.ok(variants);
    }

    @GetMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ProductVariantDto> getProductVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        Optional<ProductVariantDto> variant = productVariantService.getVariantById(variantId, tenantId);
        return variant.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ProductVariantDto> createProductVariant(
            @PathVariable Long productId,
            @RequestBody ProductVariantDto variantDto,
            @RequestParam(required = false) Map<String, String> optionValues,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            variantDto.setProductId(productId);
            
            Map<Long, Long> optionValueMap = null;
            if (optionValues != null && !optionValues.isEmpty()) {
                optionValueMap = new HashMap<>();
                for (Map.Entry<String, String> entry : optionValues.entrySet()) {
                    optionValueMap.put(Long.parseLong(entry.getKey()), Long.parseLong(entry.getValue()));
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
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestBody ProductVariantDto variantDto,
            @RequestParam(required = false) Map<String, String> optionValues,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            Map<Long, Long> optionValueMap = null;
            if (optionValues != null && !optionValues.isEmpty()) {
                optionValueMap = new HashMap<>();
                for (Map.Entry<String, String> entry : optionValues.entrySet()) {
                    optionValueMap.put(Long.parseLong(entry.getKey()), Long.parseLong(entry.getValue()));
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
            @PathVariable Long productId,
            @PathVariable Long variantId,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long productId,
            @PathVariable Long variantId,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestBody Map<String, Integer> stockUpdate,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long productId,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        List<ProductOptionDto> options = productOptionService.getProductOptions(productId, tenantId);
        return ResponseEntity.ok(options);
    }

    @PostMapping("/{productId}/options/{optionId}")
    public ResponseEntity<ProductOptionDto> addOptionToProduct(
            @PathVariable Long productId,
            @PathVariable Long optionId,
            @RequestParam(defaultValue = "false") boolean isRequired,
            @RequestParam(defaultValue = "0") Integer sortOrder,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long productId,
            @PathVariable Long optionId,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
        
        Long tenantId = getTenantIdFromAuth(authentication);
        
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
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long optionId,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        Optional<OptionDto> option = optionService.getOptionById(optionId, tenantId);
        return option.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<OptionDto> updateOption(
            @PathVariable Long optionId,
            @RequestBody OptionDto optionDto,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long optionId,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long optionId,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuth(authentication);
        List<OptionValueDto> optionValues = optionValueService.getOptionValuesByOption(optionId, tenantId);
        return ResponseEntity.ok(optionValues);
    }

    @PostMapping("/options/{optionId}/values")
    public ResponseEntity<OptionValueDto> createOptionValue(
            @PathVariable Long optionId,
            @RequestBody OptionValueDto optionValueDto,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long optionId,
            @PathVariable Long valueId,
            @RequestBody OptionValueDto optionValueDto,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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
            @PathVariable Long optionId,
            @PathVariable Long valueId,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
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

    private Long getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return 1L; // For now, return a default tenant ID
    }
}
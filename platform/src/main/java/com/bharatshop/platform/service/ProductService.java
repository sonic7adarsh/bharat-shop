package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.repository.ProductRepository;
import com.bharatshop.shared.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final FeatureFlagService featureFlagService;
    private final ProductVariantService productVariantService;

    public List<Product> getAllProductsByTenant(UUID tenantId) {
        return productRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public Page<Product> getAllProductsByTenant(UUID tenantId, Pageable pageable) {
        return productRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    public Optional<Product> getProductById(UUID id, UUID tenantId) {
        return productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId);
    }

    public Optional<Product> getProductBySlug(String slug, UUID tenantId) {
        return productRepository.findBySlugAndTenantIdAndDeletedAtIsNull(slug, tenantId);
    }

    public List<Product> getProductsByStatus(UUID tenantId, Product.ProductStatus status) {
        return productRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    public Page<Product> getProductsByStatus(UUID tenantId, Product.ProductStatus status, Pageable pageable) {
        return productRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status, pageable);
    }

    public Page<Product> searchProducts(UUID tenantId, String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return getAllProductsByTenant(tenantId, pageable);
        }
        return productRepository.searchByTenantIdAndKeyword(tenantId, keyword.trim(), pageable);
    }

    public List<Product> getProductsByCategory(UUID tenantId, UUID categoryId) {
        return productRepository.findByTenantIdAndCategoryId(tenantId, categoryId);
    }

    public Product createProduct(Product product, UUID tenantId) {
        validateProduct(product);
        
        // Check product limit before creating
        int currentProductCount = (int) getProductCount(tenantId);
        featureFlagService.enforceProductLimit(tenantId, currentProductCount);
        
        product.setTenantId(tenantId);
        product.setSlug(generateSlug(product.getName(), tenantId));
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setDeletedAt(null);
        
        if (product.getStatus() == null) {
            product.setStatus(Product.ProductStatus.DRAFT);
        }
        
        log.info("Creating product: {} for tenant: {}", product.getName(), tenantId);
        Product savedProduct = productRepository.save(product);
        
        // Create default variant if product has price and stock
        if (product.getPrice() != null && product.getStock() != null) {
            createDefaultVariant(savedProduct, tenantId);
        }
        
        return savedProduct;
    }

    public Product updateProduct(UUID id, Product productUpdates, UUID tenantId) {
        Product existingProduct = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        validateProduct(productUpdates);
        
        // Update fields
        if (StringUtils.hasText(productUpdates.getName())) {
            existingProduct.setName(productUpdates.getName());
            // Update slug if name changed
            if (!existingProduct.getName().equals(productUpdates.getName())) {
                existingProduct.setSlug(generateSlug(productUpdates.getName(), tenantId));
            }
        }
        
        if (StringUtils.hasText(productUpdates.getDescription())) {
            existingProduct.setDescription(productUpdates.getDescription());
        }
        
        if (productUpdates.getPrice() != null) {
            existingProduct.setPrice(productUpdates.getPrice());
        }
        
        if (productUpdates.getStock() != null) {
            existingProduct.setStock(productUpdates.getStock());
        }
        
        if (productUpdates.getImages() != null) {
            existingProduct.setImages(productUpdates.getImages());
        }
        
        if (productUpdates.getCategories() != null) {
            existingProduct.setCategories(productUpdates.getCategories());
        }
        
        if (StringUtils.hasText(productUpdates.getAttributes())) {
            existingProduct.setAttributes(productUpdates.getAttributes());
        }
        
        if (productUpdates.getStatus() != null) {
            existingProduct.setStatus(productUpdates.getStatus());
        }
        
        existingProduct.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating product: {} for tenant: {}", existingProduct.getName(), tenantId);
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(UUID id, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Delete all variants first
        productVariantService.deleteVariantsByProduct(id, tenantId);
        
        product.setDeletedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        
        log.info("Deleting product: {} for tenant: {}", product.getName(), tenantId);
        productRepository.save(product);
    }

    public Product updateProductStatus(UUID id, Product.ProductStatus status, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating product status: {} to {} for tenant: {}", product.getName(), status, tenantId);
        return productRepository.save(product);
    }

    public long getProductCount(UUID tenantId) {
        return productRepository.countByTenantId(tenantId);
    }

    public long getProductCountByStatus(UUID tenantId, Product.ProductStatus status) {
        return productRepository.countByTenantIdAndStatus(tenantId, status);
    }

    private void validateProduct(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            throw new IllegalArgumentException("Product name is required");
        }
        
        if (product.getPrice() == null || product.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be non-negative");
        }
        
        if (product.getStock() == null || product.getStock() < 0) {
            throw new IllegalArgumentException("Product stock must be non-negative");
        }
    }

    private String generateSlug(String name, UUID tenantId) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        String slug = baseSlug;
        int counter = 1;
        
        while (productRepository.findBySlugAndTenantIdAndDeletedAtIsNull(slug, tenantId).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        return slug;
    }
    
    /**
     * Creates a default variant for a product using the product's price and stock.
     */
    private void createDefaultVariant(Product product, UUID tenantId) {
        try {
            ProductVariantDto defaultVariant = ProductVariantDto.builder()
                    .productId(product.getId())
                    .sku(generateVariantSku(product.getName(), tenantId))
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .isDefault(true)
                    .status(ProductVariant.VariantStatus.ACTIVE)
                    .sortOrder(0)
                    .build();
            
            productVariantService.createVariant(defaultVariant, tenantId);
            log.info("Created default variant for product: {} with SKU: {}", product.getName(), defaultVariant.getSku());
        } catch (Exception e) {
            log.warn("Failed to create default variant for product: {} - {}", product.getName(), e.getMessage());
        }
    }
    
    /**
     * Generates a unique SKU for a variant.
     */
    private String generateVariantSku(String productName, UUID tenantId) {
        String baseSku = productName.toUpperCase()
                .replaceAll("[^A-Z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        if (baseSku.length() > 20) {
            baseSku = baseSku.substring(0, 20);
        }
        
        String sku = baseSku;
        int counter = 1;
        
        while (productVariantService.getVariantBySku(sku, tenantId).isPresent()) {
            sku = baseSku + "-" + counter;
            counter++;
        }
        
        return sku;
    }
}
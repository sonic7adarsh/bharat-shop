package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
// import java.util.UUID; // Replaced with Long

@Entity(name = "Product")
@Table(name = "products", indexes = {
        @Index(name = "idx_product_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_product_slug", columnList = "slug"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_name", columnList = "name")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Deprecated // Will be moved to ProductVariant
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Deprecated // Will be moved to ProductVariant
    @Column(name = "stock")
    private Integer stock;

    // Images will be handled by ProductImage entity relationship
    // @ElementCollection removed due to schema mismatch with product_images table
    private List<String> images;
    
    // Manual getter for images removed - field no longer exists
    public List<String> getImages() {
        return images;
    }
    
    // Manual getter for slug to fix Lombok issue
    public String getSlug() {
        return slug;
    }
    
    // Manual getter for name to fix Lombok issue
    public String getName() {
        return name;
    }

    @ElementCollection
    @CollectionTable(name = "product_categories", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "category_id")
    private List<Long> categories;

    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;

    // Tax-related fields
    @Column(name = "hsn_code", length = 10)
    private String hsnCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_preference", nullable = false)
    private TaxPreference taxPreference = TaxPreference.TAXABLE;

    @Column(name = "is_tax_inclusive", nullable = false)
    private Boolean isTaxInclusive = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    // Relationships with variant entities
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductOption> productOptions;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariant> variants;

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        OUT_OF_STOCK,
        DISCONTINUED
    }

    public enum TaxPreference {
        TAXABLE,
        EXEMPT
    }

    /**
     * Get the default variant for this product
     */
    public ProductVariant getDefaultVariant() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(ProductVariant::getIsDefault)
                .findFirst()
                .orElse(variants.get(0));
    }

    /**
     * Check if product has variants
     */
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    /**
     * Get effective price from default variant or fallback to product price
     */
    public BigDecimal getEffectivePrice() {
        ProductVariant defaultVariant = getDefaultVariant();
        if (defaultVariant != null) {
            return defaultVariant.getEffectivePrice();
        }
        return price; // Fallback to deprecated price field
    }

    /**
     * Get total stock from all variants or fallback to product stock
     */
    public Integer getTotalStock() {
        if (variants != null && !variants.isEmpty()) {
            return variants.stream()
                    .mapToInt(ProductVariant::getAvailableStock)
                    .sum();
        }
        return stock != null ? stock : 0; // Fallback to deprecated stock field
    }
}
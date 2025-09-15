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

    @Column(name = "previous_slug", length = 255)
    private String previousSlug; // For tracking slug changes

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription; // For SEO meta description

    @Column(name = "meta_title", length = 60)
    private String metaTitle;

    @Column(name = "meta_description", length = 160)
    private String metaDescription;

    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

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
    
    // Manual setter methods to fix Lombok issue
    public void setPreviousSlug(String previousSlug) {
        this.previousSlug = previousSlug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
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

    // SEO metadata relationship
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "seo_metadata_id")
    private SeoMetadata seoMetadata;

    // Structured data for SEO
    @Column(name = "structured_data", columnDefinition = "TEXT")
    private String structuredData; // JSON-LD format for Product schema

    // SEO flags
    @Column(name = "featured_in_sitemap")
    private Boolean featuredInSitemap = true;

    @Column(name = "sitemap_priority", precision = 3, scale = 2)
    private Double sitemapPriority = 0.8;

    @Column(name = "sitemap_change_frequency", length = 20)
    private String sitemapChangeFrequency = "weekly";

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

    // Manual getters since Lombok is not working properly
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Long> getCategories() { return categories; }
    public String getMetaTitle() { return metaTitle; }
    public String getMetaDescription() { return metaDescription; }
    public String getShortDescription() { return shortDescription; }
    public String getMetaKeywords() { return metaKeywords; }
    public Boolean getFeaturedInSitemap() { return featuredInSitemap; }
    public String getSitemapChangeFrequency() { return sitemapChangeFrequency; }
    public Double getSitemapPriority() { return sitemapPriority; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Manual getter for hsnCode
     */
    public String getHsnCode() {
        return hsnCode;
    }
    
    /**
     * Manual getter for taxPreference
     */
    public TaxPreference getTaxPreference() {
        return taxPreference;
    }
    
    /**
     * Manual getter for isTaxInclusive
     */
    public Boolean getIsTaxInclusive() {
        return isTaxInclusive;
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
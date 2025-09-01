package com.bharatshop.storefront.model;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "storefront_products", indexes = {
        @Index(name = "idx_storefront_product_category", columnList = "category"),
        @Index(name = "idx_storefront_product_brand", columnList = "brand"),
        @Index(name = "idx_storefront_product_sku", columnList = "sku"),
        @Index(name = "idx_storefront_product_active", columnList = "active"),
        @Index(name = "idx_storefront_product_featured", columnList = "featured"),
        @Index(name = "idx_storefront_product_tenant", columnList = "tenant_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {
    

    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal discountPrice;
    
    @Column(nullable = false, length = 100)
    private String category;
    
    @Column(length = 100)
    private String brand;
    
    @Column(unique = true, nullable = false, length = 50)
    private String sku;
    
    @Column(nullable = false)
    private Integer stockQuantity = 0;
    
    @ElementCollection
    @CollectionTable(name = "storefront_product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> imageUrls;
    
    @Column(nullable = false)
    private Boolean featured = false;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;
    
    @Column
    private Integer reviewCount = 0;
    
    @Column(length = 500)
    private String metaTitle;
    
    @Column(length = 1000)
    private String metaDescription;
    
    @Column(length = 200)
    private String slug;
    
    @Column(precision = 8, scale = 3)
    private BigDecimal weight;
    
    @Column(length = 50)
    private String dimensions;
    
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (active == null) {
            active = true;
        }
        if (featured == null) {
            featured = false;
        }
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
    }
    
    // Manual getter methods for compilation compatibility
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public String getSku() {
        return sku;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public Boolean getFeatured() {
        return featured;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public BigDecimal getRating() {
        return rating;
    }
    
    public Integer getReviewCount() {
        return reviewCount;
    }
    
    public String getMetaTitle() {
        return metaTitle;
    }
    
    public String getMetaDescription() {
        return metaDescription;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
}
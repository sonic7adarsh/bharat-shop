package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
// import java.util.UUID; // Replaced with Long

/**
 * ProductVariant entity representing individual variants of a product.
 * Each variant has its own SKU, price, stock, and combination of option values.
 */
@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_product_variant_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_product_variant_product_id", columnList = "product_id"),
        @Index(name = "idx_product_variant_sku", columnList = "tenant_id, sku"),
        @Index(name = "idx_product_variant_barcode", columnList = "barcode"),
        @Index(name = "idx_product_variant_status", columnList = "status"),
        @Index(name = "idx_product_variant_tenant_product", columnList = "tenant_id, product_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variant_tenant_sku", 
                         columnNames = {"tenant_id", "sku"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductVariant extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    @Column(name = "weight", precision = 8, scale = 3)
    private BigDecimal weight;

    @Column(name = "dimensions", length = 100)
    private String dimensions; // Format: "L x W x H"

    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes; // JSON string for additional attributes

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VariantStatus status = VariantStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Many-to-one relationship with Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    // One-to-many relationship with ProductVariantOptionValue
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariantOptionValue> optionValues;

    // Many-to-one relationship with ProductImage
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", insertable = false, updatable = false)
    private ProductImage image;

    public enum VariantStatus {
        ACTIVE,
        INACTIVE,
        OUT_OF_STOCK,
        DISCONTINUED
    }

    /**
     * Get available stock (total stock - reserved stock)
     */
    public Integer getAvailableStock() {
        return Math.max(0, stock - reservedStock);
    }

    /**
     * Check if variant is in stock
     */
    public boolean isInStock() {
        return getAvailableStock() > 0 && status == VariantStatus.ACTIVE;
    }

    /**
     * Get effective price (sale price if available, otherwise regular price)
     */
    public BigDecimal getEffectivePrice() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 ? salePrice : price;
    }
    
    // Manual getter and setter methods to bypass Lombok issues
    
    public int getStock() {
        return stock;
    }
    
    public void setStock(int stock) {
        this.stock = stock;
    }
    
    public Long getId() {
        return id;
    }
    
    public VariantStatus getStatus() {
        return status;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setStatus(VariantStatus status) {
        this.status = status;
    }
    
    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getAttributes() {
        return attributes;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public BigDecimal getSalePrice() {
        return salePrice;
    }
    
    public String getSku() {
        return sku;
    }
    
    public String getBarcode() {
        return barcode;
    }
    
    public Long getProductId() {
        return productId;
    }
}
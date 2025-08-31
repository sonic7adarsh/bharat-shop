package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Wishlist entity representing customer's saved products for future purchase.
 * Maintains customer's wishlist items across sessions with tenant isolation.
 */
@Entity
@Table(name = "wishlists", indexes = {
    @Index(name = "idx_wishlist_customer_tenant", columnList = "customerId, tenantId"),
    @Index(name = "idx_wishlist_product", columnList = "productId"),
    @Index(name = "idx_wishlist_created_at", columnList = "createdAt")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist extends BaseEntity {
    
    /**
     * Customer ID who owns this wishlist item
     * Using Long to match the customer ID type used in Order entity
     */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    /**
     * Product ID that is saved in the wishlist
     * References the Product entity
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    /**
     * Optional notes or comments about the wishlist item
     */
    @Column(name = "notes", length = 500)
    private String notes;
    
    /**
     * Priority level for the wishlist item (1-5, where 1 is highest priority)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 3; // Default to medium priority
    
    /**
     * Whether the customer wants to be notified about price changes
     */
    @Column(name = "price_alert_enabled", nullable = false)
    private Boolean priceAlertEnabled = false;
    
    /**
     * Whether the customer wants to be notified when the product is back in stock
     */
    @Column(name = "stock_alert_enabled", nullable = false)
    private Boolean stockAlertEnabled = false;
    
    /**
     * The price of the product when it was added to wishlist (for price tracking)
     */
    @Column(name = "added_price", precision = 10, scale = 2)
    private java.math.BigDecimal addedPrice;
    
    /**
     * Whether this wishlist item is active (not removed)
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Soft delete the wishlist item
     */
    public void removeFromWishlist() {
        this.isActive = false;
        this.softDelete();
    }
    
    /**
     * Restore the wishlist item
     */
    public void restoreToWishlist() {
        this.isActive = true;
        this.restore();
    }
}
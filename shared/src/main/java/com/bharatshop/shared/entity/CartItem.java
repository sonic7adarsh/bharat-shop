package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "SharedCartItem")
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_item_cart", columnList = "cartId"),
    @Index(name = "idx_cart_item_product", columnList = "productId"),
    @Index(name = "idx_cart_item_variant", columnList = "variantId"),
    @Index(name = "idx_cart_item_cart_variant", columnList = "cartId, variantId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartId", nullable = false)
    private Cart cart;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", nullable = false)
    private Product product;
    
    @Column(name = "variantId")
    private UUID variantId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    // Store the price at the time of adding to cart to handle price changes
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Getter methods
    public Integer getQuantity() {
        return quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public UUID getVariantId() {
        return variantId;
    }
    
    public Product getProduct() {
        return product;
    }
    
    // Helper methods
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    public void increaseQuantity(int amount) {
        if (amount > 0) {
            this.quantity += amount;
        }
    }
    
    public void decreaseQuantity(int amount) {
        if (amount > 0 && this.quantity > amount) {
            this.quantity -= amount;
        } else if (amount > 0) {
            this.quantity = 0;
        }
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = Math.max(0, quantity != null ? quantity : 0);
    }
    
    public boolean isValidQuantity() {
        return quantity != null && quantity > 0;
    }
}
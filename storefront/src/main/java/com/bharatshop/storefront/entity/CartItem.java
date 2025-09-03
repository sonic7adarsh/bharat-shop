package com.bharatshop.storefront.entity;

import com.bharatshop.storefront.model.Product;
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

@Entity(name = "StorefrontCartItem")
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
    
    // Helper methods
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    // Manual getter methods to bypass Lombok issues
    public UUID getVariantId() {
        return variantId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public Long getId() {
        return id;
    }
    
    public Cart getCart() {
        return cart;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public static CartItemBuilder builder() {
        return new CartItemBuilder();
    }
    
    public static class CartItemBuilder {
        private Long id;
        private Cart cart;
        private Product product;
        private UUID variantId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public CartItemBuilder id(Long id) { this.id = id; return this; }
        public CartItemBuilder cart(Cart cart) { this.cart = cart; return this; }
        public CartItemBuilder product(Product product) { this.product = product; return this; }
        public CartItemBuilder variantId(UUID variantId) { this.variantId = variantId; return this; }
        public CartItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public CartItemBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public CartItemBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CartItemBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public CartItem build() {
            CartItem cartItem = new CartItem();
            cartItem.id = this.id;
            cartItem.cart = this.cart;
            cartItem.product = this.product;
            cartItem.variantId = this.variantId;
            cartItem.quantity = this.quantity;
            cartItem.unitPrice = this.unitPrice;
            cartItem.createdAt = this.createdAt;
            cartItem.updatedAt = this.updatedAt;
            return cartItem;
        }
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getProductName() {
        return product != null ? product.getName() : null;
    }
    
    public String getVariantName() {
        // This would need to be fetched from ProductVariant service
        return "Default Variant"; // Placeholder
    }
}
package com.bharatshop.storefront.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "StorefrontCart")
@Table(name = "carts", indexes = {
    @Index(name = "idx_cart_customer_tenant", columnList = "customerId, tenantId"),
    @Index(name = "idx_cart_tenant", columnList = "tenantId"),
    @Index(name = "idx_cart_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CartItem> items;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public int getTotalItems() {
        return items != null ? items.stream().mapToInt(CartItem::getQuantity).sum() : 0;
    }
    
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
    
    // Manual getter methods to bypass Lombok issues
    public Long getId() {
        return id;
    }
    
    public Long getCustomerId() {
        return customerId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public List<CartItem> getItems() {
        return items;
    }
    
    public void setItems(List<CartItem> items) {
        this.items = items;
    }
    
    public void clearItems() {
        if (items != null) {
            items.clear();
        }
    }
    
    public static CartBuilder builder() {
        return new CartBuilder();
    }
    
    public static class CartBuilder {
        private Long id;
        private Long customerId;
        private UUID tenantId;
        private List<CartItem> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public CartBuilder id(Long id) { this.id = id; return this; }
        public CartBuilder customerId(Long customerId) { this.customerId = customerId; return this; }
        public CartBuilder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public CartBuilder items(List<CartItem> items) { this.items = items; return this; }
        public CartBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CartBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public Cart build() {
            Cart cart = new Cart();
            cart.id = this.id;
            cart.customerId = this.customerId;
            cart.tenantId = this.tenantId;
            cart.items = this.items;
            cart.createdAt = this.createdAt;
            cart.updatedAt = this.updatedAt;
            return cart;
        }
    }
}
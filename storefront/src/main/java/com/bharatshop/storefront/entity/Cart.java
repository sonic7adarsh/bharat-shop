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

@Entity
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
    private Long tenantId;
    
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
    
    public void clearItems() {
        if (items != null) {
            items.clear();
        }
    }
}
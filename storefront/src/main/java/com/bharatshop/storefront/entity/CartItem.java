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

@Entity
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
    
    public String getProductName() {
        return product != null ? product.getName() : null;
    }
    
    public String getProductSku() {
        return product != null ? product.getSku() : null;
    }
    
    public String getProductImageUrl() {
        return product != null && product.getImageUrls() != null && !product.getImageUrls().isEmpty() 
            ? product.getImageUrls().get(0) : null;
    }
}
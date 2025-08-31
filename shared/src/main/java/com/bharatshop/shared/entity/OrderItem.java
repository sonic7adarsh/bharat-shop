package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.CartItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_item_order", columnList = "orderId"),
    @Index(name = "idx_order_item_product", columnList = "productId"),
    @Index(name = "idx_order_item_order_product", columnList = "orderId, productId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private Integer quantity;
    
    // Store the price at the time of order creation
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    // Store discount applied to this item
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    // Store product details at the time of order (for historical reference)
    @Column(nullable = false)
    private String productName;
    
    @Column
    private String productSku;
    
    @Column
    private String productImageUrl;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public BigDecimal getTotalPrice() {
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
        if (discountAmount != null) {
            total = total.subtract(discountAmount);
        }
        return total.max(BigDecimal.ZERO);
    }
    
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    
    public BigDecimal getDiscountPercentage() {
        if (discountAmount == null || discountAmount.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = getSubtotal();
        if (subtotal.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return discountAmount.divide(subtotal, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    // Factory method to create OrderItem from CartItem
    public static OrderItem fromCartItem(CartItem cartItem, Order order) {
        Product product = cartItem.getProduct();
        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(cartItem.getQuantity())
                .price(cartItem.getUnitPrice())
                .productName(product.getName())
                .productSku(product.getSlug())
                .productImageUrl(product.getImages() != null && !product.getImages().isEmpty() ? 
                    product.getImages().get(0) : null)
                .build();
    }
}
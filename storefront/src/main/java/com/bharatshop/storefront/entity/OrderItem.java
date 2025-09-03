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

@Entity(name = "StorefrontOrderItem")
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_item_order", columnList = "orderId"),
    @Index(name = "idx_order_item_product", columnList = "productId"),
    @Index(name = "idx_order_item_variant", columnList = "variantId"),
    @Index(name = "idx_order_item_order_product", columnList = "orderId, productId")
})
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    // Manual builder method
    public static OrderItemBuilder builder() {
        return new OrderItemBuilder();
    }
    
    public static class OrderItemBuilder {
        private Order order;
        private Product product;
        private UUID variantId;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal discountAmount;
        private String productName;
        private String productSku;
        private String productImageUrl;
        
        public OrderItemBuilder order(Order order) { this.order = order; return this; }
        public OrderItemBuilder product(Product product) { this.product = product; return this; }
        public OrderItemBuilder variantId(UUID variantId) { this.variantId = variantId; return this; }
        public OrderItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public OrderItemBuilder price(BigDecimal price) { this.price = price; return this; }
        public OrderItemBuilder discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
        public OrderItemBuilder productName(String productName) { this.productName = productName; return this; }
        public OrderItemBuilder productSku(String productSku) { this.productSku = productSku; return this; }
        public OrderItemBuilder productImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; return this; }
        
        public OrderItem build() {
            OrderItem item = new OrderItem();
            item.order = this.order;
            item.product = this.product;
            item.variantId = this.variantId;
            item.quantity = this.quantity;
            item.price = this.price;
            item.discountAmount = this.discountAmount;
            item.productName = this.productName;
            item.productSku = this.productSku;
            item.productImageUrl = this.productImageUrl;
            return item;
        }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", nullable = false)
    private Product product;
    
    @Column(name = "variantId")
    private UUID variantId;
    
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
    
    public static OrderItem fromCartItem(CartItem cartItem, Order order) {
        return OrderItem.builder()
                .order(order)
                .product(cartItem.getProduct())
                .variantId(cartItem.getVariantId())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getUnitPrice())
                .productName(cartItem.getProduct().getName())
                .productSku(cartItem.getProduct().getSlug())
                .productImageUrl(cartItem.getProduct().getDescription())
                .build();
    }
    
    // Manual getter methods to bypass Lombok issues
    public Long getId() {
        return id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public String getProductSku() {
        return productSku;
    }
    
    public String getProductImageUrl() {
        return productImageUrl;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
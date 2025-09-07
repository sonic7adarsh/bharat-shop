package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.OrderItem;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private BigDecimal subtotal;
    private boolean hasDiscount;
    private BigDecimal discountPercentage;
    private LocalDateTime createdAt;
    
    // Manual builder method to bypass Lombok issues
    public static OrderItemResponseBuilder builder() {
        return new OrderItemResponseBuilder();
    }
    
    public static class OrderItemResponseBuilder {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal discountAmount;
        private BigDecimal totalPrice;
        private BigDecimal subtotal;
        private boolean hasDiscount;
        private BigDecimal discountPercentage;
        private LocalDateTime createdAt;
        
        public OrderItemResponseBuilder id(Long id) { this.id = id; return this; }
        public OrderItemResponseBuilder productId(Long productId) { this.productId = productId; return this; }
        public OrderItemResponseBuilder productName(String productName) { this.productName = productName; return this; }
        public OrderItemResponseBuilder productSku(String productSku) { this.productSku = productSku; return this; }
        public OrderItemResponseBuilder productImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; return this; }
        public OrderItemResponseBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public OrderItemResponseBuilder price(BigDecimal price) { this.price = price; return this; }
        public OrderItemResponseBuilder discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
        public OrderItemResponseBuilder totalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; return this; }
        public OrderItemResponseBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public OrderItemResponseBuilder hasDiscount(boolean hasDiscount) { this.hasDiscount = hasDiscount; return this; }
        public OrderItemResponseBuilder discountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; return this; }
        public OrderItemResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        
        public OrderItemResponse build() {
            OrderItemResponse response = new OrderItemResponse();
            response.id = this.id;
            response.productId = this.productId;
            response.productName = this.productName;
            response.productSku = this.productSku;
            response.productImageUrl = this.productImageUrl;
            response.quantity = this.quantity;
            response.price = this.price;
            response.discountAmount = this.discountAmount;
            response.totalPrice = this.totalPrice;
            response.subtotal = this.subtotal;
            response.hasDiscount = this.hasDiscount;
            response.discountPercentage = this.discountPercentage;
            response.createdAt = this.createdAt;
            return response;
        }
    }
    
    public static OrderItemResponse fromEntity(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProductName())
                .productSku(orderItem.getProductSku())
                .productImageUrl(orderItem.getProductImageUrl())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .discountAmount(orderItem.getDiscountAmount())
                .totalPrice(orderItem.getTotalPrice())
                .subtotal(orderItem.getSubtotal())
                .hasDiscount(orderItem.hasDiscount())
                .discountPercentage(orderItem.getDiscountPercentage())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }
}
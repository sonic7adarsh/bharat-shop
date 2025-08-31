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
@Builder
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
    
    public static OrderItemResponse fromEntity(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId().getMostSignificantBits())
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
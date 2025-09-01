package com.bharatshop.storefront.dto;

import com.bharatshop.storefront.entity.Cart;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    
    private Long id;
    private Long customerId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private boolean isEmpty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CartResponse fromEntity(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems() != null ? 
                cart.getItems().stream()
                        .map(CartItemResponse::fromEntity)
                        .collect(Collectors.toList()) : 
                List.of();
        
        BigDecimal subtotal = calculateSubtotal(itemResponses);
        BigDecimal taxAmount = calculateTax(subtotal);
        BigDecimal shippingAmount = calculateShipping(subtotal);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount);
        
        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .items(itemResponses)
                .totalItems(cart.getTotalItems())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .totalAmount(totalAmount)
                .isEmpty(cart.isEmpty())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
    
    private static BigDecimal calculateSubtotal(List<CartItemResponse> items) {
        return items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private static BigDecimal calculateTax(BigDecimal subtotal) {
        // 18% GST
        BigDecimal taxRate = BigDecimal.valueOf(0.18);
        return subtotal.multiply(taxRate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    private static BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping for orders above 500
        if (subtotal.compareTo(BigDecimal.valueOf(500)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(50); // Flat shipping rate
    }
}
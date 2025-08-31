package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.CartItem;
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
public class CartItemResponse {
    
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer availableStock;
    private LocalDateTime addedAt;
    
    public static CartItemResponse fromEntity(CartItem cartItem) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct().getId().getMostSignificantBits())
                .productName(cartItem.getProduct().getName())
                .productSku(cartItem.getProduct().getSlug())
                .productImageUrl(cartItem.getProduct().getDescription())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getTotalPrice())
                .availableStock(cartItem.getProduct().getStock())
                .addedAt(cartItem.getCreatedAt())
                .build();
    }
}
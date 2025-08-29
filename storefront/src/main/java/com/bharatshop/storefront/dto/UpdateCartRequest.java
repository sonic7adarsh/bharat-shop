package com.bharatshop.storefront.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be at least 0")
    private Integer quantity;
}
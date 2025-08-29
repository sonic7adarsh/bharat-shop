package com.bharatshop.storefront.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    
    @NotNull(message = "Address ID is required")
    private Long addressId;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    private String couponCode;
}
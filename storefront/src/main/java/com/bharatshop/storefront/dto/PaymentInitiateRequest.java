package com.bharatshop.storefront.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentInitiateRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    private String notes;
}
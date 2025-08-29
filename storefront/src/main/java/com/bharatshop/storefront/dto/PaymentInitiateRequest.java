package com.bharatshop.storefront.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentInitiateRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    private String notes;
}
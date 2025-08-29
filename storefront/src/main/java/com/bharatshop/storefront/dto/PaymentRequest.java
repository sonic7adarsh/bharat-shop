package com.bharatshop.storefront.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotBlank(message = "Payment gateway ID is required")
    private String paymentGatewayId;
    
    @NotBlank(message = "Payment gateway order ID is required")
    private String paymentGatewayOrderId;
    
    @NotBlank(message = "Payment gateway payment ID is required")
    private String paymentGatewayPaymentId;
    
    @NotBlank(message = "Payment gateway signature is required")
    private String paymentGatewaySignature;
}
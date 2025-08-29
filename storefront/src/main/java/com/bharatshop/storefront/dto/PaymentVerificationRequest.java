package com.bharatshop.storefront.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentVerificationRequest {
    
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;
    
    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;
    
    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
package com.bharatshop.shared.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestDto {

    @NotNull(message = "Plan ID is required")
    private UUID planId;

    private Boolean autoRenew = true;
    
    // For payment verification
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    
    // For cancellation
    private String cancellationReason;
    
    // Manual getters to fix compilation issues
    public UUID getPlanId() {
        return this.planId;
    }
    
    public String getRazorpayOrderId() {
        return this.razorpayOrderId;
    }
    
    public String getRazorpayPaymentId() {
        return this.razorpayPaymentId;
    }
    
    public String getRazorpaySignature() {
        return this.razorpaySignature;
    }
    
    public String getCancellationReason() {
        return this.cancellationReason;
    }
}
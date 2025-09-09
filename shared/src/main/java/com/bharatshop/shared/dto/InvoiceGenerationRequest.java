package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceGenerationRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @Builder.Default
    private boolean generatePdf = true;
    
    @Builder.Default
    private boolean sendEmail = false;
    
    private String customEmailAddress;
    private String notes;
}
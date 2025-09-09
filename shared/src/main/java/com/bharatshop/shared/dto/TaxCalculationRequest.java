package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationRequest {
    
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @NotBlank(message = "HSN code is required")
    private String hsnCode;
    
    @NotBlank(message = "Seller state code is required")
    private String sellerStateCode;
    
    @NotBlank(message = "Buyer state code is required")
    private String buyerStateCode;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    private BigDecimal amount;
    
    @Builder.Default
    private boolean isTaxInclusive = false;
}
package com.bharatshop.shared.dto;

import com.bharatshop.shared.entity.TaxRate;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRateDto {
    
    private Long id;
    
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @NotBlank(message = "HSN code is required")
    @Size(max = 20, message = "HSN code must not exceed 20 characters")
    private String hsnCode;
    
    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
    private String stateCode;
    
    @NotBlank(message = "Tax type is required")
    private String taxType; // INTRA_STATE, INTER_STATE
    
    @DecimalMin(value = "0.0", message = "CGST rate must be non-negative")
    @DecimalMax(value = "100.0", message = "CGST rate must not exceed 100%")
    private BigDecimal cgstRate;
    
    @DecimalMin(value = "0.0", message = "SGST rate must be non-negative")
    @DecimalMax(value = "100.0", message = "SGST rate must not exceed 100%")
    private BigDecimal sgstRate;
    
    @DecimalMin(value = "0.0", message = "IGST rate must be non-negative")
    @DecimalMax(value = "100.0", message = "IGST rate must not exceed 100%")
    private BigDecimal igstRate;
    
    @DecimalMin(value = "0.0", message = "Cess rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Cess rate must not exceed 100%")
    private BigDecimal cessRate;
    
    private boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    public static TaxRateDto fromEntity(TaxRate taxRate) {
        if (taxRate == null) {
            return null;
        }
        
        return TaxRateDto.builder()
            .id(taxRate.getId())
            .tenantId(taxRate.getTenantId())
            .hsnCode(taxRate.getHsnCode())
            .stateCode(taxRate.getStateCode())
            .taxType(taxRate.getTaxType().name())
            .cgstRate(taxRate.getCgstRate())
            .sgstRate(taxRate.getSgstRate())
            .igstRate(taxRate.getIgstRate())
            .cessRate(taxRate.getCessRate())
            .isActive(taxRate.getStatus() == TaxRate.TaxRateStatus.ACTIVE)
            .createdAt(taxRate.getCreatedAt())
            .updatedAt(taxRate.getUpdatedAt())
            .build();
    }
    
    public TaxRate toEntity() {
        return TaxRate.builder()
            .id(this.id)
            .tenantId(this.tenantId)
            .hsnCode(this.hsnCode)
            .stateCode(this.stateCode)
            .taxType(this.taxType != null ? TaxRate.TaxType.valueOf(this.taxType) : null)
            .cgstRate(this.cgstRate)
            .sgstRate(this.sgstRate)
            .igstRate(this.igstRate)
            .cessRate(this.cessRate)
            .status(this.isActive ? TaxRate.TaxRateStatus.ACTIVE : TaxRate.TaxRateStatus.INACTIVE)
            .build();
    }
    
    // Helper methods
    public BigDecimal getTotalTaxRate() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (cgstRate != null) {
            total = total.add(cgstRate);
        }
        if (sgstRate != null) {
            total = total.add(sgstRate);
        }
        if (igstRate != null) {
            total = total.add(igstRate);
        }
        if (cessRate != null) {
            total = total.add(cessRate);
        }
        
        return total;
    }
    
    public boolean isIntraState() {
        return "INTRA_STATE".equals(taxType);
    }
    
    public boolean isInterState() {
        return "INTER_STATE".equals(taxType);
    }
    
    public boolean hasGst() {
        return (cgstRate != null && cgstRate.compareTo(BigDecimal.ZERO) > 0) ||
               (sgstRate != null && sgstRate.compareTo(BigDecimal.ZERO) > 0) ||
               (igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0);
    }
    
    public boolean hasCess() {
        return cessRate != null && cessRate.compareTo(BigDecimal.ZERO) > 0;
    }
}
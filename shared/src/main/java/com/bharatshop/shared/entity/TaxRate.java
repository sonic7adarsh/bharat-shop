package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * TaxRate entity for GST tax configuration by HSN code and state
 */
@Entity
@Table(name = "tax_rates", indexes = {
        @Index(name = "idx_tax_rate_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tax_rate_hsn_code", columnList = "hsn_code"),
        @Index(name = "idx_tax_rate_state", columnList = "state_code"),
        @Index(name = "idx_tax_rate_tenant_hsn_state", columnList = "tenant_id, hsn_code, state_code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tax_rate_tenant_hsn_state_type", 
                         columnNames = {"tenant_id", "hsn_code", "state_code", "tax_type"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TaxRate extends BaseEntity {

    @Column(name = "hsn_code", nullable = false, length = 10)
    private String hsnCode;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "igst_rate", precision = 5, scale = 2)
    private BigDecimal igstRate;

    @Column(name = "cess_rate", precision = 5, scale = 2)
    private BigDecimal cessRate;

    @Column(name = "is_tax_inclusive", nullable = false)
    private Boolean isTaxInclusive = false;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaxRateStatus status = TaxRateStatus.ACTIVE;

    public enum TaxType {
        INTRA_STATE,  // Within same state (CGST + SGST)
        INTER_STATE   // Between different states (IGST)
    }

    public enum TaxRateStatus {
        ACTIVE,
        INACTIVE
    }

    /**
     * Get total tax rate (CGST + SGST for intra-state, IGST for inter-state)
     */
    public BigDecimal getTotalTaxRate() {
        if (taxType == TaxType.INTRA_STATE) {
            BigDecimal cgst = cgstRate != null ? cgstRate : BigDecimal.ZERO;
            BigDecimal sgst = sgstRate != null ? sgstRate : BigDecimal.ZERO;
            BigDecimal cess = cessRate != null ? cessRate : BigDecimal.ZERO;
            return cgst.add(sgst).add(cess);
        } else {
            BigDecimal igst = igstRate != null ? igstRate : BigDecimal.ZERO;
            BigDecimal cess = cessRate != null ? cessRate : BigDecimal.ZERO;
            return igst.add(cess);
        }
    }
}
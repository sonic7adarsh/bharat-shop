package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity to manage invoice number sequences per tenant
 */
@Entity
@Table(name = "invoice_number_sequences", indexes = {
        @Index(name = "idx_invoice_seq_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_invoice_seq_financial_year", columnList = "financial_year")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoice_seq_tenant_year", 
                         columnNames = {"tenant_id", "financial_year"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InvoiceNumberSequence extends BaseEntity {

    @Column(name = "financial_year", nullable = false, length = 7)
    private String financialYear; // Format: 2024-25

    @Column(name = "prefix", length = 10)
    private String prefix = "INV";

    @Column(name = "current_number", nullable = false)
    private Long currentNumber = 0L;

    @Column(name = "padding_length", nullable = false)
    private Integer paddingLength = 6;

    @Column(name = "suffix", length = 10)
    private String suffix;

    /**
     * Generate next invoice number
     */
    public String generateNextNumber() {
        currentNumber++;
        return formatInvoiceNumber(currentNumber);
    }

    /**
     * Format invoice number with prefix, padding and suffix
     */
    public String formatInvoiceNumber(Long number) {
        StringBuilder sb = new StringBuilder();
        
        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix).append("-");
        }
        
        sb.append(financialYear).append("-");
        
        // Pad the number with leading zeros
        String paddedNumber = String.format("%0" + paddingLength + "d", number);
        sb.append(paddedNumber);
        
        if (suffix != null && !suffix.isEmpty()) {
            sb.append("-").append(suffix);
        }
        
        return sb.toString();
    }

    /**
     * Get current invoice number without incrementing
     */
    public String getCurrentInvoiceNumber() {
        return formatInvoiceNumber(currentNumber);
    }

    /**
     * Preview next invoice number without incrementing
     */
    public String previewNextNumber() {
        return formatInvoiceNumber(currentNumber + 1);
    }

    /**
     * Reset sequence for new financial year
     */
    public void resetForNewYear(String newFinancialYear) {
        this.financialYear = newFinancialYear;
        this.currentNumber = 0L;
    }

    /**
     * Get financial year from date (April to March)
     */
    public static String getFinancialYear(int year, int month) {
        if (month >= 4) { // April onwards
            return year + "-" + String.format("%02d", (year + 1) % 100);
        } else { // January to March
            return (year - 1) + "-" + String.format("%02d", year % 100);
        }
    }

    // Manual setter methods
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setCurrentNumber(Long currentNumber) {
        this.currentNumber = currentNumber;
    }

    public void setPaddingLength(Integer paddingLength) {
        this.paddingLength = paddingLength;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }


}
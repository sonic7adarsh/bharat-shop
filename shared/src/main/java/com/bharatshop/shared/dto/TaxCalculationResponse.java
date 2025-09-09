package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationResponse {
    
    private String hsnCode;
    private String sellerStateCode;
    private String buyerStateCode;
    private boolean isIntraState;
    private boolean isInterState;
    
    // Original amounts
    private BigDecimal originalAmount;
    private boolean isTaxInclusive;
    
    // Calculated amounts
    private BigDecimal netAmount;
    private BigDecimal taxableAmount;
    
    // Tax rates
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;
    private BigDecimal cessRate;
    private BigDecimal totalTaxRate;
    
    // Tax amounts
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal cessAmount;
    private BigDecimal totalTaxAmount;
    
    // Final amounts
    private BigDecimal totalAmount;
    
    // Tax breakdown summary
    private String taxBreakdown;
    
    public static TaxCalculationResponse create(
            String hsnCode, String sellerStateCode, String buyerStateCode,
            BigDecimal originalAmount, boolean isTaxInclusive,
            BigDecimal netAmount, BigDecimal cgstRate, BigDecimal sgstRate,
            BigDecimal igstRate, BigDecimal cessRate,
            BigDecimal cgstAmount, BigDecimal sgstAmount,
            BigDecimal igstAmount, BigDecimal cessAmount) {
        
        boolean isIntraState = sellerStateCode.equals(buyerStateCode);
        BigDecimal totalTaxRate = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        
        if (cgstRate != null) totalTaxRate = totalTaxRate.add(cgstRate);
        if (sgstRate != null) totalTaxRate = totalTaxRate.add(sgstRate);
        if (igstRate != null) totalTaxRate = totalTaxRate.add(igstRate);
        if (cessRate != null) totalTaxRate = totalTaxRate.add(cessRate);
        
        if (cgstAmount != null) totalTaxAmount = totalTaxAmount.add(cgstAmount);
        if (sgstAmount != null) totalTaxAmount = totalTaxAmount.add(sgstAmount);
        if (igstAmount != null) totalTaxAmount = totalTaxAmount.add(igstAmount);
        if (cessAmount != null) totalTaxAmount = totalTaxAmount.add(cessAmount);
        
        BigDecimal totalAmount = netAmount.add(totalTaxAmount);
        
        String taxBreakdown = buildTaxBreakdown(isIntraState, cgstRate, sgstRate, igstRate, cessRate);
        
        return TaxCalculationResponse.builder()
            .hsnCode(hsnCode)
            .sellerStateCode(sellerStateCode)
            .buyerStateCode(buyerStateCode)
            .isIntraState(isIntraState)
            .isInterState(!isIntraState)
            .originalAmount(originalAmount)
            .isTaxInclusive(isTaxInclusive)
            .netAmount(netAmount)
            .taxableAmount(netAmount)
            .cgstRate(cgstRate)
            .sgstRate(sgstRate)
            .igstRate(igstRate)
            .cessRate(cessRate)
            .totalTaxRate(totalTaxRate)
            .cgstAmount(cgstAmount)
            .sgstAmount(sgstAmount)
            .igstAmount(igstAmount)
            .cessAmount(cessAmount)
            .totalTaxAmount(totalTaxAmount)
            .totalAmount(totalAmount)
            .taxBreakdown(taxBreakdown)
            .build();
    }
    
    private static String buildTaxBreakdown(boolean isIntraState, 
                                          BigDecimal cgstRate, BigDecimal sgstRate,
                                          BigDecimal igstRate, BigDecimal cessRate) {
        StringBuilder breakdown = new StringBuilder();
        
        if (isIntraState) {
            if (cgstRate != null && cgstRate.compareTo(BigDecimal.ZERO) > 0) {
                breakdown.append("CGST: ").append(cgstRate).append("%");
            }
            if (sgstRate != null && sgstRate.compareTo(BigDecimal.ZERO) > 0) {
                if (breakdown.length() > 0) breakdown.append(", ");
                breakdown.append("SGST: ").append(sgstRate).append("%");
            }
        } else {
            if (igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0) {
                breakdown.append("IGST: ").append(igstRate).append("%");
            }
        }
        
        if (cessRate != null && cessRate.compareTo(BigDecimal.ZERO) > 0) {
            if (breakdown.length() > 0) breakdown.append(", ");
            breakdown.append("Cess: ").append(cessRate).append("%");
        }
        
        return breakdown.toString();
    }
    
    // Helper methods
    public boolean hasTax() {
        return totalTaxAmount != null && totalTaxAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean hasGst() {
        return (cgstAmount != null && cgstAmount.compareTo(BigDecimal.ZERO) > 0) ||
               (sgstAmount != null && sgstAmount.compareTo(BigDecimal.ZERO) > 0) ||
               (igstAmount != null && igstAmount.compareTo(BigDecimal.ZERO) > 0);
    }
    
    public boolean hasCess() {
        return cessAmount != null && cessAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
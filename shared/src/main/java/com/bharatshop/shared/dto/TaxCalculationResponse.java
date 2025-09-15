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
    
    // Manual builder method for compilation compatibility
    public static TaxCalculationResponseBuilder builder() {
        return new TaxCalculationResponseBuilder();
    }
    
    public static class TaxCalculationResponseBuilder {
        private String hsnCode;
        private String sellerStateCode;
        private String buyerStateCode;
        private boolean isIntraState;
        private boolean isInterState;
        private BigDecimal originalAmount;
        private boolean isTaxInclusive;
        private BigDecimal netAmount;
        private BigDecimal taxableAmount;
        private BigDecimal cgstRate;
        private BigDecimal sgstRate;
        private BigDecimal igstRate;
        private BigDecimal cessRate;
        private BigDecimal totalTaxRate;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal cessAmount;
        private BigDecimal totalTaxAmount;
        private BigDecimal totalAmount;
        private String taxBreakdown;
        
        public TaxCalculationResponseBuilder hsnCode(String hsnCode) { this.hsnCode = hsnCode; return this; }
        public TaxCalculationResponseBuilder sellerStateCode(String sellerStateCode) { this.sellerStateCode = sellerStateCode; return this; }
        public TaxCalculationResponseBuilder buyerStateCode(String buyerStateCode) { this.buyerStateCode = buyerStateCode; return this; }
        public TaxCalculationResponseBuilder isIntraState(boolean isIntraState) { this.isIntraState = isIntraState; return this; }
        public TaxCalculationResponseBuilder isInterState(boolean isInterState) { this.isInterState = isInterState; return this; }
        public TaxCalculationResponseBuilder originalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; return this; }
        public TaxCalculationResponseBuilder isTaxInclusive(boolean isTaxInclusive) { this.isTaxInclusive = isTaxInclusive; return this; }
        public TaxCalculationResponseBuilder netAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }
        public TaxCalculationResponseBuilder taxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; return this; }
        public TaxCalculationResponseBuilder cgstRate(BigDecimal cgstRate) { this.cgstRate = cgstRate; return this; }
        public TaxCalculationResponseBuilder sgstRate(BigDecimal sgstRate) { this.sgstRate = sgstRate; return this; }
        public TaxCalculationResponseBuilder igstRate(BigDecimal igstRate) { this.igstRate = igstRate; return this; }
        public TaxCalculationResponseBuilder cessRate(BigDecimal cessRate) { this.cessRate = cessRate; return this; }
        public TaxCalculationResponseBuilder totalTaxRate(BigDecimal totalTaxRate) { this.totalTaxRate = totalTaxRate; return this; }
        public TaxCalculationResponseBuilder cgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; return this; }
        public TaxCalculationResponseBuilder sgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; return this; }
        public TaxCalculationResponseBuilder igstAmount(BigDecimal igstAmount) { this.igstAmount = igstAmount; return this; }
        public TaxCalculationResponseBuilder cessAmount(BigDecimal cessAmount) { this.cessAmount = cessAmount; return this; }
        public TaxCalculationResponseBuilder totalTaxAmount(BigDecimal totalTaxAmount) { this.totalTaxAmount = totalTaxAmount; return this; }
        public TaxCalculationResponseBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public TaxCalculationResponseBuilder taxBreakdown(String taxBreakdown) { this.taxBreakdown = taxBreakdown; return this; }
        
        public TaxCalculationResponse build() {
            TaxCalculationResponse response = new TaxCalculationResponse();
            response.hsnCode = this.hsnCode;
            response.sellerStateCode = this.sellerStateCode;
            response.buyerStateCode = this.buyerStateCode;
            response.isIntraState = this.isIntraState;
            response.isInterState = this.isInterState;
            response.originalAmount = this.originalAmount;
            response.isTaxInclusive = this.isTaxInclusive;
            response.netAmount = this.netAmount;
            response.taxableAmount = this.taxableAmount;
            response.cgstRate = this.cgstRate;
            response.sgstRate = this.sgstRate;
            response.igstRate = this.igstRate;
            response.cessRate = this.cessRate;
            response.totalTaxRate = this.totalTaxRate;
            response.cgstAmount = this.cgstAmount;
            response.sgstAmount = this.sgstAmount;
            response.igstAmount = this.igstAmount;
            response.cessAmount = this.cessAmount;
            response.totalTaxAmount = this.totalTaxAmount;
            response.totalAmount = this.totalAmount;
            response.taxBreakdown = this.taxBreakdown;
            return response;
        }
    }
    
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
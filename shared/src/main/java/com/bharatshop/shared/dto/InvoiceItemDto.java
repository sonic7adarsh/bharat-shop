package com.bharatshop.shared.dto;

import com.bharatshop.shared.entity.InvoiceItem;
import com.bharatshop.shared.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {
    
    private Long id;
    private Long invoiceId;
    private Long productId;
    private Long productVariantId;
    private String productName;
    private String productDescription;
    private String productSku;
    private String hsnCode;
    
    // Quantity and Pricing
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal netAmount;
    
    // Tax Details
    private BigDecimal cgstRate;
    private BigDecimal cgstAmount;
    private BigDecimal sgstRate;
    private BigDecimal sgstAmount;
    private BigDecimal igstRate;
    private BigDecimal igstAmount;
    private BigDecimal cessRate;
    private BigDecimal cessAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalAmount;
    
    // Tax Preferences
    private String taxPreference; // TAXABLE, EXEMPT
    private boolean isTaxInclusive;
    private boolean isTaxExempt;
    
    public static InvoiceItemDto fromEntity(InvoiceItem item) {
        if (item == null) {
            return null;
        }
        
        return InvoiceItemDto.builder()
            .id(item.getId())
            .invoiceId(item.getInvoice() != null ? item.getInvoice().getId() : null)
            .productId(item.getProductId())
            .productVariantId(item.getProductVariantId())
            .productName(item.getProductName())
            .productDescription(item.getProductDescription())
            .productSku(item.getSku())
            .hsnCode(item.getHsnCode())
            .quantity(item.getQuantity())
            .unit(item.getUnit())
            .unitPrice(item.getUnitPrice())
            .netAmount(item.getNetAmount())
            .cgstRate(item.getCgstRate())
            .cgstAmount(item.getCgstAmount())
            .sgstRate(item.getSgstRate())
            .sgstAmount(item.getSgstAmount())
            .igstRate(item.getIgstRate())
            .igstAmount(item.getIgstAmount())
            .cessRate(item.getCessRate())
            .cessAmount(item.getCessAmount())
            .totalTaxAmount(item.getTotalTaxAmount())
            .totalAmount(item.getTotalAmount())
            .taxPreference(item.getTaxPreference() != null ? item.getTaxPreference().name() : null)
            .isTaxInclusive(item.getIsTaxInclusive())
            .isTaxExempt(item.isTaxExempt())
            .build();
    }
    
    public InvoiceItem toEntity() {
        return InvoiceItem.builder()
            .id(this.id)
            .productId(this.productId)
            .productVariantId(this.productVariantId)
            .productName(this.productName)
            .productDescription(this.productDescription)
            .sku(this.productSku)
            .hsnCode(this.hsnCode)
            .quantity(this.quantity)
            .unit(this.unit)
            .unitPrice(this.unitPrice)
            .netAmount(this.netAmount)
            .cgstRate(this.cgstRate)
            .cgstAmount(this.cgstAmount)
            .sgstRate(this.sgstRate)
            .sgstAmount(this.sgstAmount)
            .igstRate(this.igstRate)
            .igstAmount(this.igstAmount)
            .cessRate(this.cessRate)
            .cessAmount(this.cessAmount)
            .totalTaxAmount(this.totalTaxAmount)
            .totalAmount(this.totalAmount)
            .taxPreference(this.taxPreference != null ? Product.TaxPreference.valueOf(this.taxPreference) : null)
            .isTaxInclusive(this.isTaxInclusive)
            .build();
    }
    
    // Helper methods for calculations
    public BigDecimal getLineTotal() {
        if (quantity != null && unitPrice != null) {
            return quantity.multiply(unitPrice);
        }
        return BigDecimal.ZERO;
    }
    
    public BigDecimal getTaxRate() {
        BigDecimal totalRate = BigDecimal.ZERO;
        
        if (cgstRate != null) {
            totalRate = totalRate.add(cgstRate);
        }
        if (sgstRate != null) {
            totalRate = totalRate.add(sgstRate);
        }
        if (igstRate != null) {
            totalRate = totalRate.add(igstRate);
        }
        if (cessRate != null) {
            totalRate = totalRate.add(cessRate);
        }
        
        return totalRate;
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
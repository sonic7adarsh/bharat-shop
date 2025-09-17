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
    
    // Manual builder method
    public static InvoiceItemDtoBuilder builder() {
        return new InvoiceItemDtoBuilder();
    }
    
    public static class InvoiceItemDtoBuilder {
        private Long id;
        private Long invoiceId;
        private Long productId;
        private Long productVariantId;
        private String productName;
        private String productDescription;
        private String productSku;
        private String hsnCode;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal netAmount;
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
        private String taxPreference;
        private Boolean isTaxInclusive;
        private Boolean isTaxExempt;
        
        public InvoiceItemDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public InvoiceItemDtoBuilder invoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
            return this;
        }
        
        public InvoiceItemDtoBuilder productId(Long productId) {
            this.productId = productId;
            return this;
        }
        
        public InvoiceItemDtoBuilder productVariantId(Long productVariantId) {
            this.productVariantId = productVariantId;
            return this;
        }
        
        public InvoiceItemDtoBuilder productName(String productName) {
            this.productName = productName;
            return this;
        }
        
        public InvoiceItemDtoBuilder productDescription(String productDescription) {
            this.productDescription = productDescription;
            return this;
        }
        
        public InvoiceItemDtoBuilder productSku(String productSku) {
            this.productSku = productSku;
            return this;
        }
        
        public InvoiceItemDtoBuilder hsnCode(String hsnCode) {
            this.hsnCode = hsnCode;
            return this;
        }
        
        public InvoiceItemDtoBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public InvoiceItemDtoBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }
        
        public InvoiceItemDtoBuilder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }
        
        public InvoiceItemDtoBuilder netAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
            return this;
        }
        
        public InvoiceItemDtoBuilder cgstRate(BigDecimal cgstRate) {
            this.cgstRate = cgstRate;
            return this;
        }
        
        public InvoiceItemDtoBuilder cgstAmount(BigDecimal cgstAmount) {
            this.cgstAmount = cgstAmount;
            return this;
        }
        
        public InvoiceItemDtoBuilder sgstRate(BigDecimal sgstRate) {
            this.sgstRate = sgstRate;
            return this;
        }
        
        public InvoiceItemDtoBuilder sgstAmount(BigDecimal sgstAmount) {
            this.sgstAmount = sgstAmount;
            return this;
        }
        
        public InvoiceItemDtoBuilder igstRate(BigDecimal igstRate) {
            this.igstRate = igstRate;
            return this;
        }
        
        public InvoiceItemDtoBuilder igstAmount(BigDecimal igstAmount) {
            this.igstAmount = igstAmount;
            return this;
        }
        
        public InvoiceItemDtoBuilder cessRate(BigDecimal cessRate) {
            this.cessRate = cessRate;
            return this;
        }
        
        public InvoiceItemDtoBuilder cessAmount(BigDecimal cessAmount) {
            this.cessAmount = cessAmount;
            return this;
        }
        
        public InvoiceItemDtoBuilder totalTaxAmount(BigDecimal totalTaxAmount) {
            this.totalTaxAmount = totalTaxAmount;
            return this;
        }
        
        public InvoiceItemDtoBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public InvoiceItemDtoBuilder taxPreference(String taxPreference) {
            this.taxPreference = taxPreference;
            return this;
        }

        public InvoiceItemDtoBuilder isTaxInclusive(Boolean isTaxInclusive) {
            this.isTaxInclusive = isTaxInclusive;
            return this;
        }

        public InvoiceItemDtoBuilder isTaxExempt(Boolean isTaxExempt) {
            this.isTaxExempt = isTaxExempt;
            return this;
        }
        
        public InvoiceItemDto build() {
            InvoiceItemDto dto = new InvoiceItemDto();
            dto.id = this.id;
            dto.invoiceId = this.invoiceId;
            dto.productId = this.productId;
            dto.productVariantId = this.productVariantId;
            dto.productName = this.productName;
            dto.productDescription = this.productDescription;
            dto.productSku = this.productSku;
            dto.hsnCode = this.hsnCode;
            dto.quantity = this.quantity;
            dto.unitPrice = this.unitPrice;
            dto.netAmount = this.netAmount;
            dto.cgstRate = this.cgstRate;
            dto.cgstAmount = this.cgstAmount;
            dto.sgstRate = this.sgstRate;
            dto.sgstAmount = this.sgstAmount;
            dto.igstRate = this.igstRate;
            dto.igstAmount = this.igstAmount;
            dto.cessRate = this.cessRate;
            dto.cessAmount = this.cessAmount;
            dto.totalTaxAmount = this.totalTaxAmount;
            dto.totalAmount = this.totalAmount;
            dto.taxPreference = this.taxPreference;
            dto.isTaxInclusive = this.isTaxInclusive;
            dto.isTaxExempt = this.isTaxExempt;
            return dto;
        }
    }

    
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
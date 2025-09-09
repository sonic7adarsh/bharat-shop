package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Invoice line item entity for GST-compliant invoicing
 */
@Entity
@Table(name = "invoice_items", indexes = {
        @Index(name = "idx_invoice_item_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_invoice_item_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_invoice_item_product_id", columnList = "product_id"),
        @Index(name = "idx_invoice_item_hsn_code", columnList = "hsn_code")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InvoiceItem extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    // Product Details
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "hsn_code", nullable = false, length = 10)
    private String hsnCode;

    // Quantity and Pricing
    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit = "PCS";

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    // Tax Details
    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "cgst_amount", precision = 10, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "sgst_amount", precision = 10, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_rate", precision = 5, scale = 2)
    private BigDecimal igstRate;

    @Column(name = "igst_amount", precision = 10, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "cess_rate", precision = 5, scale = 2)
    private BigDecimal cessRate;

    @Column(name = "cess_amount", precision = 10, scale = 2)
    private BigDecimal cessAmount;

    @Column(name = "total_tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalTaxAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Tax Preference
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_preference", nullable = false)
    private Product.TaxPreference taxPreference = Product.TaxPreference.TAXABLE;

    @Column(name = "is_tax_inclusive", nullable = false)
    private Boolean isTaxInclusive = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", insertable = false, updatable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;

    /**
     * Get total tax rate (sum of all applicable taxes)
     */
    public BigDecimal getTotalTaxRate() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (cgstRate != null) total = total.add(cgstRate);
        if (sgstRate != null) total = total.add(sgstRate);
        if (igstRate != null) total = total.add(igstRate);
        if (cessRate != null) total = total.add(cessRate);
        
        return total;
    }

    /**
     * Check if item is tax exempt
     */
    public boolean isTaxExempt() {
        return taxPreference == Product.TaxPreference.EXEMPT;
    }

    /**
     * Get line total before tax
     */
    public BigDecimal getLineTotal() {
        BigDecimal lineTotal = unitPrice.multiply(quantity);
        if (discountAmount != null) {
            lineTotal = lineTotal.subtract(discountAmount);
        }
        return lineTotal;
    }

    /**
     * Get tax breakdown as string
     */
    public String getTaxBreakdown() {
        StringBuilder sb = new StringBuilder();
        
        if (cgstAmount != null && cgstAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("CGST(%.2f%%): ₹%.2f ", cgstRate, cgstAmount));
        }
        if (sgstAmount != null && sgstAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("SGST(%.2f%%): ₹%.2f ", sgstRate, sgstAmount));
        }
        if (igstAmount != null && igstAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("IGST(%.2f%%): ₹%.2f ", igstRate, igstAmount));
        }
        if (cessAmount != null && cessAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("CESS(%.2f%%): ₹%.2f ", cessRate, cessAmount));
        }
        
        return sb.toString().trim();
    }
}
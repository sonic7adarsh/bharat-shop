package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GST-compliant Invoice entity
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoice_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_invoice_order_id", columnList = "order_id"),
        @Index(name = "idx_invoice_number", columnList = "invoice_number"),
        @Index(name = "idx_invoice_date", columnList = "invoice_date"),
        @Index(name = "idx_invoice_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoice_tenant_number", 
                         columnNames = {"tenant_id", "invoice_number"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Invoice extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // Seller (Merchant) Details
    @Column(name = "seller_name", nullable = false, length = 255)
    private String sellerName;

    @Column(name = "seller_address", nullable = false, columnDefinition = "TEXT")
    private String sellerAddress;

    @Column(name = "seller_gstin", length = 15)
    private String sellerGstin;

    @Column(name = "seller_pan", length = 10)
    private String sellerPan;

    @Column(name = "seller_state_code", length = 2)
    private String sellerStateCode;

    @Column(name = "seller_email", length = 255)
    private String sellerEmail;

    @Column(name = "seller_phone", length = 20)
    private String sellerPhone;

    // Buyer (Customer) Details
    @Column(name = "buyer_name", nullable = false, length = 255)
    private String buyerName;

    @Column(name = "buyer_address", nullable = false, columnDefinition = "TEXT")
    private String buyerAddress;

    @Column(name = "buyer_gstin", length = 15)
    private String buyerGstin;

    @Column(name = "buyer_pan", length = 10)
    private String buyerPan;

    @Column(name = "buyer_state_code", nullable = false, length = 2)
    private String buyerStateCode;

    @Column(name = "buyer_email", length = 255)
    private String buyerEmail;

    @Column(name = "buyer_phone", length = 20)
    private String buyerPhone;

    // Tax Details
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "place_of_supply", nullable = false, length = 2)
    private String placeOfSupply;

    // Amount Details
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_cgst", precision = 12, scale = 2)
    private BigDecimal totalCgst;

    @Column(name = "total_sgst", precision = 12, scale = 2)
    private BigDecimal totalSgst;

    @Column(name = "total_igst", precision = 12, scale = 2)
    private BigDecimal totalIgst;

    @Column(name = "total_cess", precision = 12, scale = 2)
    private BigDecimal totalCess;

    @Column(name = "total_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTax;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_amount", precision = 12, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_in_words", length = 500)
    private String amountInWords;

    // Additional Fields
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // QR Code for digital verification
    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    // PDF Storage
    @Column(name = "pdf_media_id")
    private Long pdfMediaId;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    // Status and Tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    // Relationships
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceItem> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_media_id", insertable = false, updatable = false)
    private MediaFile pdfFile;

    public enum TaxType {
        INTRA_STATE,  // Within same state (CGST + SGST)
        INTER_STATE   // Between different states (IGST)
    }

    public enum InvoiceStatus {
        DRAFT,
        GENERATED,
        SENT,
        VIEWED,
        PAID,
        CANCELLED
    }

    /**
     * Check if invoice is for intra-state transaction
     */
    public boolean isIntraState() {
        return taxType == TaxType.INTRA_STATE;
    }

    /**
     * Check if invoice is for inter-state transaction
     */
    public boolean isInterState() {
        return taxType == TaxType.INTER_STATE;
    }

    /**
     * Get tax breakdown summary
     */
    public String getTaxSummary() {
        if (isIntraState()) {
            return String.format("CGST: ₹%.2f, SGST: ₹%.2f", 
                    totalCgst != null ? totalCgst : BigDecimal.ZERO,
                    totalSgst != null ? totalSgst : BigDecimal.ZERO);
        } else {
            return String.format("IGST: ₹%.2f", 
                    totalIgst != null ? totalIgst : BigDecimal.ZERO);
        }
    }
}
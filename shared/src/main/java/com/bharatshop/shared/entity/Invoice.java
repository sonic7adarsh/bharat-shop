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
    
    // Manual getter methods to fix Lombok issue
    public Long getId() {
        return id;
    }
    
    public String getInvoiceNumber() {
        return invoiceNumber;
    }
    
    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public TaxType getTaxType() {
        return taxType;
    }
    
    public String getSellerGstin() {
        return sellerGstin;
    }
    
    public BigDecimal getTotalTax() {
        return totalTax;
    }
    
    public String getBuyerGstin() {
        return buyerGstin;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public String getBuyerName() {
        return buyerName;
    }
    
    public String getBuyerEmail() {
        return buyerEmail;
    }
    
    public Long getPdfMediaId() {
        return pdfMediaId;
    }
    
    public List<InvoiceItem> getItems() {
        return items;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public String getSellerAddress() {
        return sellerAddress;
    }
    
    public String getSellerPan() {
        return sellerPan;
    }
    
    public String getSellerStateCode() {
        return sellerStateCode;
    }
    
    public String getSellerEmail() {
        return sellerEmail;
    }
    
    public String getSellerPhone() {
        return sellerPhone;
    }
    
    public String getBuyerAddress() {
        return buyerAddress;
    }
    
    public String getBuyerStateCode() {
        return buyerStateCode;
    }
    
    public String getBuyerPhone() {
        return buyerPhone;
    }
    
    public String getPlaceOfSupply() {
        return placeOfSupply;
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public BigDecimal getTotalCgst() {
        return totalCgst;
    }
    
    public BigDecimal getTotalSgst() {
        return totalSgst;
    }
    
    public BigDecimal getTotalIgst() {
        return totalIgst;
    }
    
    public BigDecimal getTotalCess() {
        return totalCess;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }
    
    public String getAmountInWords() {
        return amountInWords;
    }
    
    public InvoiceStatus getStatus() {
        return status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getBuyerPan() {
        return buyerPan;
    }
    
    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
    
    public String getPdfUrl() {
        return pdfUrl;
    }
    
    public String getQrCodeData() {
        return qrCodeData;
    }
    
    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    // Setter methods
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
    
    public void setSellerAddress(String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }
    
    public void setSellerGstin(String sellerGstin) {
        this.sellerGstin = sellerGstin;
    }
    
    public void setSellerPan(String sellerPan) {
        this.sellerPan = sellerPan;
    }
    
    public void setSellerStateCode(String sellerStateCode) {
        this.sellerStateCode = sellerStateCode;
    }
    
    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
    }
    
    public void setSellerPhone(String sellerPhone) {
        this.sellerPhone = sellerPhone;
    }
    
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }
    
    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }
    
    public void setBuyerGstin(String buyerGstin) {
        this.buyerGstin = buyerGstin;
    }
    
    public void setBuyerStateCode(String buyerStateCode) {
        this.buyerStateCode = buyerStateCode;
    }
    
    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }
    
    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }
    
    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public void setPlaceOfSupply(String placeOfSupply) {
        this.placeOfSupply = placeOfSupply;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    public void setTotalCgst(BigDecimal totalCgst) {
        this.totalCgst = totalCgst;
    }
    
    public void setTotalSgst(BigDecimal totalSgst) {
        this.totalSgst = totalSgst;
    }
    
    public void setTotalIgst(BigDecimal totalIgst) {
        this.totalIgst = totalIgst;
    }
    
    public void setTotalCess(BigDecimal totalCess) {
        this.totalCess = totalCess;
    }
    
    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public void setAmountInWords(String amountInWords) {
        this.amountInWords = amountInWords;
    }
    
    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }
    
    public void setPdfMediaId(Long pdfMediaId) {
        this.pdfMediaId = pdfMediaId;
    }
    
    public void setTaxType(TaxType taxType) {
        this.taxType = taxType;
    }
    
    public void setBuyerPan(String buyerPan) {
        this.buyerPan = buyerPan;
    }
    
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }
    
    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
    
    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
    
    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }
    
    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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
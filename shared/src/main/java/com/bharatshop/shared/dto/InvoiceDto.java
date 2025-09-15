package com.bharatshop.shared.dto;

import com.bharatshop.shared.entity.Invoice;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public class InvoiceDto {
    
    public Long id;
    public Long tenantId;
    public Long orderId;
    public String invoiceNumber;
    
    // Seller Details
    public String sellerName;
    public String sellerAddress;
    public String sellerGstin;
    public String sellerPan;
    public String sellerStateCode;
    public String sellerEmail;
    public String sellerPhone;
    
    // Buyer Details
    public String buyerName;
    public String buyerAddress;
    public String buyerGstin;
    public String buyerStateCode;
    public String buyerEmail;
    public String buyerPhone;
    
    // Invoice Details
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate invoiceDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate dueDate;
    
    public String placeOfSupply;
    public String currency;
    
    // Amounts
    public BigDecimal subtotal;
    public BigDecimal totalCgst;
    public BigDecimal totalSgst;
    public BigDecimal totalIgst;
    public BigDecimal totalCess;
    public BigDecimal totalTax;
    public BigDecimal totalAmount;
    public String amountInWords;
    
    // Status and Metadata
    public String status;
    public Long pdfFileId;
    public String notes;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime updatedAt;
    
    // Invoice Items
    public List<InvoiceItemDto> items;
    
    // Tax Summary
    public boolean isIntraState;
    public boolean isInterState;
    
    public static InvoiceDto fromEntity(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        
        List<InvoiceItemDto> itemDtos = null;
        if (invoice.getItems() != null) {
            itemDtos = invoice.getItems().stream()
                .map(InvoiceItemDto::fromEntity)
                .toList();
        }
        
        InvoiceDto dto = new InvoiceDto();
        dto.id = invoice.getId();
        dto.tenantId = invoice.getTenantId();
        dto.orderId = invoice.getOrderId();
        dto.invoiceNumber = invoice.getInvoiceNumber();
        // financialYear field not available in Invoice entity
        dto.sellerName = invoice.getSellerName();
        dto.sellerAddress = invoice.getSellerAddress();
        dto.sellerGstin = invoice.getSellerGstin();
        dto.sellerPan = invoice.getSellerPan();
        dto.sellerStateCode = invoice.getSellerStateCode();
        dto.sellerEmail = invoice.getSellerEmail();
        dto.sellerPhone = invoice.getSellerPhone();
        dto.buyerName = invoice.getBuyerName();
        dto.buyerAddress = invoice.getBuyerAddress();
        dto.buyerGstin = invoice.getBuyerGstin();
        dto.buyerStateCode = invoice.getBuyerStateCode();
        dto.buyerEmail = invoice.getBuyerEmail();
        dto.buyerPhone = invoice.getBuyerPhone();
        dto.invoiceDate = invoice.getInvoiceDate();
        dto.dueDate = invoice.getDueDate();
        dto.placeOfSupply = invoice.getPlaceOfSupply();
        dto.currency = invoice.getCurrency();
        dto.subtotal = invoice.getSubtotal();
        dto.totalCgst = invoice.getTotalCgst();
        dto.totalSgst = invoice.getTotalSgst();
        dto.totalIgst = invoice.getTotalIgst();
        dto.totalCess = invoice.getTotalCess();
        dto.totalTax = invoice.getTotalTax();
        dto.totalAmount = invoice.getTotalAmount();
        dto.amountInWords = invoice.getAmountInWords();
        dto.status = invoice.getStatus() != null ? invoice.getStatus().name() : null;
        // Note: PaymentStatus is tracked in Order entity
        dto.pdfFileId = invoice.getPdfMediaId();
        dto.notes = invoice.getNotes();
        dto.createdAt = invoice.getCreatedAt();
        dto.updatedAt = invoice.getUpdatedAt();
        dto.items = itemDtos;
        dto.isIntraState = invoice.isIntraState();
        dto.isInterState = invoice.isInterState();
        return dto;
    }
    
    public Invoice toEntity() {
        Invoice invoice = new Invoice();
        invoice.setId(this.id);
        invoice.setTenantId(this.tenantId);
        invoice.setOrderId(this.orderId);
        invoice.setInvoiceNumber(this.invoiceNumber);
        invoice.setSellerName(this.sellerName);
        invoice.setSellerAddress(this.sellerAddress);
        invoice.setSellerGstin(this.sellerGstin);
        invoice.setSellerPan(this.sellerPan);
        invoice.setSellerStateCode(this.sellerStateCode);
        invoice.setSellerEmail(this.sellerEmail);
        invoice.setSellerPhone(this.sellerPhone);
        invoice.setBuyerName(this.buyerName);
        invoice.setBuyerAddress(this.buyerAddress);
        invoice.setBuyerGstin(this.buyerGstin);
        invoice.setBuyerStateCode(this.buyerStateCode);
        invoice.setBuyerEmail(this.buyerEmail);
        invoice.setBuyerPhone(this.buyerPhone);
        invoice.setInvoiceDate(this.invoiceDate);
        invoice.setDueDate(this.dueDate);
        invoice.setPlaceOfSupply(this.placeOfSupply);
        invoice.setCurrency(this.currency);
        invoice.setSubtotal(this.subtotal);
        invoice.setTotalCgst(this.totalCgst);
        invoice.setTotalSgst(this.totalSgst);
        invoice.setTotalIgst(this.totalIgst);
        invoice.setTotalCess(this.totalCess);
        invoice.setTotalTax(this.totalTax);
        invoice.setTotalAmount(this.totalAmount);
        invoice.setAmountInWords(this.amountInWords);
        invoice.setStatus(this.status != null ? Invoice.InvoiceStatus.valueOf(this.status) : null);
        invoice.setPdfMediaId(this.pdfFileId);
        invoice.setNotes(this.notes);
        return invoice;
    }
}
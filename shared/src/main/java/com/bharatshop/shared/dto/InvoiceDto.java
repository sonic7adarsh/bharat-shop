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


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    
    private Long id;
    private Long tenantId;
    private Long orderId;
    private String invoiceNumber;
    
    // Seller Details
    private String sellerName;
    private String sellerAddress;
    private String sellerGstin;
    private String sellerPan;
    private String sellerStateCode;
    private String sellerEmail;
    private String sellerPhone;
    
    // Buyer Details
    private String buyerName;
    private String buyerAddress;
    private String buyerGstin;
    private String buyerStateCode;
    private String buyerEmail;
    private String buyerPhone;
    
    // Invoice Details
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    
    private String placeOfSupply;
    private String currency;
    
    // Amounts
    private BigDecimal subtotal;
    private BigDecimal totalCgst;
    private BigDecimal totalSgst;
    private BigDecimal totalIgst;
    private BigDecimal totalCess;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;
    private String amountInWords;
    
    // Status and Metadata
    private String status;
    private Long pdfFileId;
    private String notes;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Invoice Items
    private List<InvoiceItemDto> items;
    
    // Tax Summary
    private boolean isIntraState;
    private boolean isInterState;
    
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
        
        return InvoiceDto.builder()
            .id(invoice.getId())
            .tenantId(invoice.getTenantId())
            .orderId(invoice.getOrderId())
            .invoiceNumber(invoice.getInvoiceNumber())
            // financialYear field not available in Invoice entity
            .sellerName(invoice.getSellerName())
            .sellerAddress(invoice.getSellerAddress())
            .sellerGstin(invoice.getSellerGstin())
            .sellerPan(invoice.getSellerPan())
            .sellerStateCode(invoice.getSellerStateCode())
            .sellerEmail(invoice.getSellerEmail())
            .sellerPhone(invoice.getSellerPhone())
            .buyerName(invoice.getBuyerName())
            .buyerAddress(invoice.getBuyerAddress())
            .buyerGstin(invoice.getBuyerGstin())
            .buyerStateCode(invoice.getBuyerStateCode())
            .buyerEmail(invoice.getBuyerEmail())
            .buyerPhone(invoice.getBuyerPhone())
            .invoiceDate(invoice.getInvoiceDate())
            .dueDate(invoice.getDueDate())
            .placeOfSupply(invoice.getPlaceOfSupply())
            .currency(invoice.getCurrency())
            .subtotal(invoice.getSubtotal())
            .totalCgst(invoice.getTotalCgst())
            .totalSgst(invoice.getTotalSgst())
            .totalIgst(invoice.getTotalIgst())
            .totalCess(invoice.getTotalCess())
            .totalTax(invoice.getTotalTax())
            .totalAmount(invoice.getTotalAmount())
            .amountInWords(invoice.getAmountInWords())
            .status(invoice.getStatus().name())
            // Note: PaymentStatus is tracked in Order entity
            .pdfFileId(invoice.getPdfMediaId())
            .notes(invoice.getNotes())
            .createdAt(invoice.getCreatedAt())
            .updatedAt(invoice.getUpdatedAt())
            .items(itemDtos)
            .isIntraState(invoice.isIntraState())
            .isInterState(invoice.isInterState())
            .build();
    }
    
    public Invoice toEntity() {
        return Invoice.builder()
            .id(this.id)
            .tenantId(this.tenantId)
            .orderId(this.orderId)
            .invoiceNumber(this.invoiceNumber)
            .sellerName(this.sellerName)
            .sellerAddress(this.sellerAddress)
            .sellerGstin(this.sellerGstin)
            .sellerPan(this.sellerPan)
            .sellerStateCode(this.sellerStateCode)
            .sellerEmail(this.sellerEmail)
            .sellerPhone(this.sellerPhone)
            .buyerName(this.buyerName)
            .buyerAddress(this.buyerAddress)
            .buyerGstin(this.buyerGstin)
            .buyerStateCode(this.buyerStateCode)
            .buyerEmail(this.buyerEmail)
            .buyerPhone(this.buyerPhone)
            .invoiceDate(this.invoiceDate)
            .dueDate(this.dueDate)
            .placeOfSupply(this.placeOfSupply)
            .currency(this.currency)
            .subtotal(this.subtotal)
            .totalCgst(this.totalCgst)
            .totalSgst(this.totalSgst)
            .totalIgst(this.totalIgst)
            .totalCess(this.totalCess)
            .totalTax(this.totalTax)
            .totalAmount(this.totalAmount)
            .amountInWords(this.amountInWords)
            .status(this.status != null ? Invoice.InvoiceStatus.valueOf(this.status) : null)
            .pdfMediaId(this.pdfFileId)
            .notes(this.notes)
            .build();
    }
}
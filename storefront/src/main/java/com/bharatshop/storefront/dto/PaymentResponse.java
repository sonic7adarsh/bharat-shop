package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
    
    private Long id;
    private Long orderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String method;
    private String description;
    private String receipt;
    private String notes;
    private String failureReason;
    private Boolean webhookVerified;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    
    // Manual getter methods
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getMethod() { return method; }
    public String getDescription() { return description; }
    public String getReceipt() { return receipt; }
    public String getNotes() { return notes; }
    public String getFailureReason() { return failureReason; }
    public Boolean getWebhookVerified() { return webhookVerified; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Manual builder method
    public static PaymentResponseBuilder builder() {
        return new PaymentResponseBuilder();
    }
    
    public static class PaymentResponseBuilder {
        private Long id;
        private Long orderId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String method;
        private String description;
        private String receipt;
        private String notes;
        private String failureReason;
        private Boolean webhookVerified;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
        
        public PaymentResponseBuilder id(Long id) { this.id = id; return this; }
        public PaymentResponseBuilder orderId(Long orderId) { this.orderId = orderId; return this; }
        public PaymentResponseBuilder razorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; return this; }
        public PaymentResponseBuilder razorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; return this; }
        public PaymentResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentResponseBuilder currency(String currency) { this.currency = currency; return this; }
        public PaymentResponseBuilder status(String status) { this.status = status; return this; }
        public PaymentResponseBuilder method(String method) { this.method = method; return this; }
        public PaymentResponseBuilder description(String description) { this.description = description; return this; }
        public PaymentResponseBuilder receipt(String receipt) { this.receipt = receipt; return this; }
        public PaymentResponseBuilder notes(String notes) { this.notes = notes; return this; }
        public PaymentResponseBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public PaymentResponseBuilder webhookVerified(Boolean webhookVerified) { this.webhookVerified = webhookVerified; return this; }
        public PaymentResponseBuilder paidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }
        public PaymentResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        
        public PaymentResponse build() {
            PaymentResponse response = new PaymentResponse();
            response.id = this.id;
            response.orderId = this.orderId;
            response.razorpayOrderId = this.razorpayOrderId;
            response.razorpayPaymentId = this.razorpayPaymentId;
            response.amount = this.amount;
            response.currency = this.currency;
            response.status = this.status;
            response.method = this.method;
            response.description = this.description;
            response.receipt = this.receipt;
            response.notes = this.notes;
            response.failureReason = this.failureReason;
            response.webhookVerified = this.webhookVerified;
            response.paidAt = this.paidAt;
            response.createdAt = this.createdAt;
            return response;
        }
    }
    
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .razorpayOrderId(payment.getRazorpayOrderId())
            .razorpayPaymentId(payment.getRazorpayPaymentId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .method(payment.getMethod() != null ? payment.getMethod().name() : null)
            .description(payment.getDescription())
            .receipt(payment.getReceipt())
            .notes(payment.getNotes())
            .failureReason(payment.getFailureReason())
            .webhookVerified(payment.getWebhookVerified())
            .paidAt(payment.getPaidAt())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
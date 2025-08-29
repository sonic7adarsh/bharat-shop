package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
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
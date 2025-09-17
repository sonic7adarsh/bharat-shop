package com.bharatshop.shared.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Domain event fired when a refund is successfully processed for a customer.
 */
@Getter
public class RefundProcessedEvent extends BaseDomainEvent {
    
    private static final String EVENT_TYPE = "RefundProcessed";
    
    private final String orderId;
    private final String refundId;
    private final String customerId;
    private final String customerEmail;
    private final String customerName;
    private final BigDecimal refundAmount;
    private final String currency;
    private final String refundReason;
    private final String refundMethod;
    private final String originalPaymentId;
    private final String gatewayProvider;
    private final String refundTransactionId;
    
    public RefundProcessedEvent(String tenantId, String userId, String orderId, String refundId,
                               String customerId, String customerEmail, String customerName,
                               BigDecimal refundAmount, String currency, String refundReason,
                               String refundMethod, String originalPaymentId, String gatewayProvider,
                               String refundTransactionId) {
        super(tenantId, userId, null);
        this.orderId = orderId;
        this.refundId = refundId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.refundAmount = refundAmount;
        this.currency = currency;
        this.refundReason = refundReason;
        this.refundMethod = refundMethod;
        this.originalPaymentId = originalPaymentId;
        this.gatewayProvider = gatewayProvider;
        this.refundTransactionId = refundTransactionId;
        
        // Add refund-specific data to event data map
        addEventData("orderId", orderId);
        addEventData("refundId", refundId);
        addEventData("customerId", customerId);
        addEventData("customerEmail", customerEmail);
        addEventData("customerName", customerName);
        addEventData("refundAmount", refundAmount);
        addEventData("currency", currency);
        addEventData("refundReason", refundReason);
        addEventData("refundMethod", refundMethod);
        addEventData("originalPaymentId", originalPaymentId);
        addEventData("gatewayProvider", gatewayProvider);
        addEventData("refundTransactionId", refundTransactionId);
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    

}
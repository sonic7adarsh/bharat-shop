package com.bharatshop.shared.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Domain event fired when a payment is successfully captured for an order.
 */
@Getter
public class PaymentCapturedEvent extends BaseDomainEvent {
    
    private static final String EVENT_TYPE = "PaymentCaptured";
    
    private final String orderId;
    private final String paymentId;
    private final String customerId;
    private final String customerEmail;
    private final BigDecimal amount;
    private final String currency;
    private final String paymentMethod;
    private final String transactionId;
    private final String gatewayProvider;
    
    public PaymentCapturedEvent(String tenantId, String userId, String orderId, String paymentId,
                               String customerId, String customerEmail, BigDecimal amount,
                               String currency, String paymentMethod, String transactionId,
                               String gatewayProvider) {
        super(tenantId, userId, null);
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.gatewayProvider = gatewayProvider;
        
        // Add payment-specific data to event data map
        addEventData("orderId", orderId);
        addEventData("paymentId", paymentId);
        addEventData("customerId", customerId);
        addEventData("customerEmail", customerEmail);
        addEventData("amount", amount);
        addEventData("currency", currency);
        addEventData("paymentMethod", paymentMethod);
        addEventData("transactionId", transactionId);
        addEventData("gatewayProvider", gatewayProvider);
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    

}
package com.bharatshop.shared.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Domain event fired when a new order is placed by a customer.
 */
@Getter
public class OrderPlacedEvent extends BaseDomainEvent {
    
    private static final String EVENT_TYPE = "OrderPlaced";
    
    private final String orderId;
    private final String customerId;
    private final String customerEmail;
    private final String customerName;
    private final BigDecimal totalAmount;
    private final String currency;
    private final List<OrderItem> items;
    private final String shippingAddress;
    private final String billingAddress;
    
    public OrderPlacedEvent(String tenantId, String userId, String orderId, String customerId,
                           String customerEmail, String customerName, BigDecimal totalAmount,
                           String currency, List<OrderItem> items, String shippingAddress,
                           String billingAddress) {
        super(tenantId, userId, null);
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        
        // Add order-specific data to event data map
        addEventData("orderId", orderId);
        addEventData("customerId", customerId);
        addEventData("customerEmail", customerEmail);
        addEventData("customerName", customerName);
        addEventData("totalAmount", totalAmount);
        addEventData("currency", currency);
        addEventData("itemCount", items.size());
        addEventData("shippingAddress", shippingAddress);
        addEventData("billingAddress", billingAddress);
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    

    
    @Getter
    public static class OrderItem {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
        
        public OrderItem(String productId, String productName, int quantity, 
                        BigDecimal unitPrice, BigDecimal totalPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }
    }
}
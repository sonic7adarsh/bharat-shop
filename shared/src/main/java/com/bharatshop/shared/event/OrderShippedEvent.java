package com.bharatshop.shared.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain event fired when an order is shipped to the customer.
 */
@Getter
public class OrderShippedEvent extends BaseDomainEvent {
    
    private static final String EVENT_TYPE = "OrderShipped";
    
    private final String orderId;
    private final String customerId;
    private final String customerEmail;
    private final String customerName;
    private final String trackingNumber;
    private final String carrier;
    private final String shippingMethod;
    private final String shippingAddress;
    private final LocalDateTime estimatedDeliveryDate;
    
    public OrderShippedEvent(String tenantId, String userId, String orderId, String customerId,
                            String customerEmail, String customerName, String trackingNumber,
                            String carrier, String shippingMethod, String shippingAddress,
                            LocalDateTime estimatedDeliveryDate) {
        super(tenantId, userId, null);
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.shippingMethod = shippingMethod;
        this.shippingAddress = shippingAddress;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        
        // Add shipping-specific data to event data map
        addEventData("orderId", orderId);
        addEventData("customerId", customerId);
        addEventData("customerEmail", customerEmail);
        addEventData("customerName", customerName);
        addEventData("trackingNumber", trackingNumber);
        addEventData("carrier", carrier);
        addEventData("shippingMethod", shippingMethod);
        addEventData("shippingAddress", shippingAddress);
        addEventData("estimatedDeliveryDate", estimatedDeliveryDate);
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
package com.bharatshop.shared.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Domain event fired when an order is successfully delivered to the customer.
 */
@Getter
public class OrderDeliveredEvent extends BaseDomainEvent {
    
    private static final String EVENT_TYPE = "OrderDelivered";
    
    private final String orderId;
    private final String customerId;
    private final String customerEmail;
    private final String customerName;
    private final String trackingNumber;
    private final String carrier;
    private final LocalDateTime deliveredAt;
    private final String deliveryAddress;
    private final String deliverySignature;
    private final String deliveryNotes;
    
    public OrderDeliveredEvent(String tenantId, String userId, String orderId, String customerId,
                              String customerEmail, String customerName, String trackingNumber,
                              String carrier, LocalDateTime deliveredAt, String deliveryAddress,
                              String deliverySignature, String deliveryNotes) {
        super(tenantId, userId, null);
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.deliveredAt = deliveredAt;
        this.deliveryAddress = deliveryAddress;
        this.deliverySignature = deliverySignature;
        this.deliveryNotes = deliveryNotes;
        
        // Add delivery-specific data to event data map
        addEventData("orderId", orderId);
        addEventData("customerId", customerId);
        addEventData("customerEmail", customerEmail);
        addEventData("customerName", customerName);
        addEventData("trackingNumber", trackingNumber);
        addEventData("carrier", carrier);
        addEventData("deliveredAt", deliveredAt);
        addEventData("deliveryAddress", deliveryAddress);
        addEventData("deliverySignature", deliverySignature);
        addEventData("deliveryNotes", deliveryNotes);
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    

}
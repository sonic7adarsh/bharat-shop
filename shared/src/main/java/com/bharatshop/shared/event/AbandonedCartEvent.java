package com.bharatshop.shared.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event fired when a customer abandons their shopping cart without completing the purchase.
 */
@Getter
public class AbandonedCartEvent extends BaseDomainEvent {
    
    private static final String EVENT_TYPE = "AbandonedCart";
    
    private final String cartId;
    private final String customerId;
    private final String customerEmail;
    private final String customerName;
    private final BigDecimal cartValue;
    private final String currency;
    private final List<CartItem> items;
    private final LocalDateTime lastActivityAt;
    private final int abandonmentDurationMinutes;
    private final String abandonmentStage; // "browsing", "checkout_started", "payment_info_entered"
    
    public AbandonedCartEvent(String tenantId, String userId, String cartId, String customerId,
                             String customerEmail, String customerName, BigDecimal cartValue,
                             String currency, List<CartItem> items, LocalDateTime lastActivityAt,
                             int abandonmentDurationMinutes, String abandonmentStage) {
        super(tenantId, userId, null);
        this.cartId = cartId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.cartValue = cartValue;
        this.currency = currency;
        this.items = items;
        this.lastActivityAt = lastActivityAt;
        this.abandonmentDurationMinutes = abandonmentDurationMinutes;
        this.abandonmentStage = abandonmentStage;
        
        // Add cart abandonment-specific data to event data map
        addEventData("cartId", cartId);
        addEventData("customerId", customerId);
        addEventData("customerEmail", customerEmail);
        addEventData("customerName", customerName);
        addEventData("cartValue", cartValue);
        addEventData("currency", currency);
        addEventData("itemCount", items.size());
        addEventData("lastActivityAt", lastActivityAt);
        addEventData("abandonmentDurationMinutes", abandonmentDurationMinutes);
        addEventData("abandonmentStage", abandonmentStage);
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    

    
    @Getter
    public static class CartItem {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
        private final String productImageUrl;
        
        public CartItem(String productId, String productName, int quantity,
                       BigDecimal unitPrice, BigDecimal totalPrice, String productImageUrl) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
            this.productImageUrl = productImageUrl;
        }
    }
}
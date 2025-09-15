package com.bharatshop.shared.event;

import com.bharatshop.shared.entity.Orders.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Event published when an order status changes.
 * Used for webhooks, notifications, and audit logging.
 */
@Data
@Builder
public class OrderStatusChangeEvent {
    
    private Long orderId;
    private Long tenantId;
    private Long customerId;
    private String orderNumber;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private LocalDateTime timestamp;
    
    // Manual constructor for compatibility
    public OrderStatusChangeEvent(Long orderId, Long tenantId, Long customerId, String orderNumber, 
                                 OrderStatus fromStatus, OrderStatus toStatus, LocalDateTime timestamp) {
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.timestamp = timestamp;
    }
    
    // Default constructor for Lombok
    public OrderStatusChangeEvent() {}
    
    // Manual getter methods (in case Lombok is not working)
    public Long getOrderId() {
        return orderId;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public Long getCustomerId() {
        return customerId;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public OrderStatus getFromStatus() {
        return fromStatus;
    }
    
    public OrderStatus getToStatus() {
        return toStatus;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if this is a transition to a terminal state
     */
    public boolean isTerminalTransition() {
        return toStatus.isTerminal();
    }
    
    /**
     * Check if this is a cancellation event
     */
    public boolean isCancellation() {
        return toStatus == OrderStatus.CANCELLED;
    }
    
    /**
     * Check if this is a delivery event
     */
    public boolean isDelivery() {
        return toStatus == OrderStatus.DELIVERED;
    }
    
    /**
     * Check if this is a return-related event
     */
    public boolean isReturnRelated() {
        return toStatus == OrderStatus.RETURN_REQUESTED || 
               toStatus == OrderStatus.RETURNED || 
               toStatus == OrderStatus.REFUNDED;
    }
    
    /**
     * Get event type for webhook categorization
     */
    public String getEventType() {
        return switch (toStatus) {
            case CONFIRMED -> "order.confirmed";
            case PACKED -> "order.packed";
            case SHIPPED -> "order.shipped";
            case DELIVERED -> "order.delivered";
            case CANCELLED -> "order.cancelled";
            case RETURN_REQUESTED -> "order.return_requested";
            case RETURNED -> "order.returned";
            case REFUNDED -> "order.refunded";
            default -> "order.status_changed";
        };
    }
}
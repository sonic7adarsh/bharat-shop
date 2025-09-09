package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
// import java.util.UUID; // Replaced with Long

@Entity(name = "SharedOrder")
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_customer_tenant", columnList = "customerId, tenantId"),
    @Index(name = "idx_order_tenant", columnList = "tenantId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_payment_status", columnList = "paymentStatus"),
    @Index(name = "idx_order_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long tenantId;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(unique = true, nullable = false)
    private String orderNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal shippingAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;
    
    // Shipping Address Information
    @Column
    private Long shippingAddressId; // Reference to CustomerAddress
    
    // Captured shipping address details (for historical reference)
    @Column(length = 100)
    private String shippingName;
    
    @Column(length = 20)
    private String shippingPhone;
    
    @Column(length = 255)
    private String shippingLine1;
    
    @Column(length = 255)
    private String shippingLine2;
    
    @Column(length = 100)
    private String shippingCity;
    
    @Column(length = 100)
    private String shippingState;
    
    @Column(length = 10)
    private String shippingPincode;
    
    @Column(length = 100)
    private String shippingCountry;
    
    // Payment gateway integration fields
    @Column
    private String paymentGatewayId; // Payment gateway identifier
    
    @Column
    private String paymentGatewayOrderId; // Razorpay order ID
    
    @Column
    private String paymentGatewayPaymentId; // Razorpay payment ID
    
    @Column
    private String paymentGatewaySignature; // Payment gateway signature
    
    @Column
    private String paymentMethod; // UPI, Card, NetBanking, etc.
    
    @Column(length = 1000)
    private String notes;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime deliveredAt;
    
    @Column
    private LocalDateTime cancelledAt;
    
    @Column
    private LocalDateTime confirmedAt;
    
    @Column
    private LocalDateTime packedAt;
    
    @Column
    private LocalDateTime shippedAt;
    
    @Column(length = 100)
    private String trackingNumber;
    
    @Column(length = 100)
    private String courierPartner;
    
    @Column(length = 500)
    private String cancellationReason;
    
    // Enums
    public enum OrderStatus {
        // Main flow states
        PENDING_PAYMENT,    // Initial state when order is created
        CONFIRMED,          // Payment successful, order confirmed
        PACKED,             // Order items packed and ready for shipping
        SHIPPED,            // Order shipped to customer
        DELIVERED,          // Order delivered to customer
        
        // Branch states
        CANCELLED,          // Order cancelled (can happen from PENDING_PAYMENT, CONFIRMED, PACKED)
        RETURN_REQUESTED,   // Customer requested return (from DELIVERED)
        RETURNED,           // Items physically returned
        REFUNDED;           // Refund processed
        
        /**
         * Get allowed next states from current state
         */
        public Set<OrderStatus> getAllowedTransitions() {
            return switch (this) {
                case PENDING_PAYMENT -> Set.of(CONFIRMED, CANCELLED);
                case CONFIRMED -> Set.of(PACKED, CANCELLED);
                case PACKED -> Set.of(SHIPPED, CANCELLED);
                case SHIPPED -> Set.of(DELIVERED);
                case DELIVERED -> Set.of(RETURN_REQUESTED);
                case RETURN_REQUESTED -> Set.of(RETURNED, DELIVERED); // Can reject return
                case RETURNED -> Set.of(REFUNDED);
                case CANCELLED, REFUNDED -> Set.of(); // Terminal states
            };
        }
        
        /**
         * Check if transition to target state is allowed
         */
        public boolean canTransitionTo(OrderStatus targetStatus) {
            return getAllowedTransitions().contains(targetStatus);
        }
        
        /**
         * Check if this is a terminal state (no further transitions allowed)
         */
        public boolean isTerminal() {
            return this == CANCELLED || this == REFUNDED || this == DELIVERED;
        }
        
        /**
         * Check if order can be cancelled from this state
         */
        public boolean canBeCancelled() {
            return this == PENDING_PAYMENT || this == CONFIRMED || this == PACKED;
        }
        
        /**
         * Check if order can be returned from this state
         */
        public boolean canBeReturned() {
            return this == DELIVERED;
        }
    }
    
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
    
    // Helper methods
    public BigDecimal getSubtotal() {
        return items != null ? 
            items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : 
            BigDecimal.ZERO;
    }
    
    public int getTotalItems() {
        return items != null ? items.stream().mapToInt(OrderItem::getQuantity).sum() : 0;
    }
    
    public boolean canBeCancelled() {
        return status != null && status.canBeCancelled();
    }
    
    public boolean isPaymentCompleted() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }
    
    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED && deliveredAt != null;
    }
    
    public void markAsDelivered() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
    
    public void markAsPacked() {
        this.status = OrderStatus.PACKED;
        this.packedAt = LocalDateTime.now();
    }
    
    public void markAsShipped(String trackingNumber, String courierPartner) {
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
        this.trackingNumber = trackingNumber;
        this.courierPartner = courierPartner;
    }
    
    public boolean canBeShipped() {
        return status == OrderStatus.PACKED;
    }
    
    public boolean canBePacked() {
        return status == OrderStatus.CONFIRMED && isPaymentCompleted();
    }
    
    public String getFullShippingAddress() {
        if (shippingLine1 == null) return null;
        
        StringBuilder address = new StringBuilder();
        address.append(shippingLine1);
        
        if (shippingLine2 != null && !shippingLine2.trim().isEmpty()) {
            address.append(", ").append(shippingLine2);
        }
        
        address.append(", ").append(shippingCity)
               .append(", ").append(shippingState)
               .append(" - ").append(shippingPincode)
               .append(", ").append(shippingCountry);
        
        return address.toString();
    }
    
    public String getShortShippingAddress() {
        if (shippingCity == null) return null;
        return shippingCity + ", " + shippingState + " - " + shippingPincode;
    }
}
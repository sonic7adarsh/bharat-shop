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
    private LocalDateTime packedAt;
    
    @Column
    private LocalDateTime shippedAt;
    
    @Column(length = 100)
    private String trackingNumber;
    
    @Column(length = 100)
    private String courierPartner;
    
    // Enums
    public enum OrderStatus {
        DRAFT,
        PENDING,
        CONFIRMED,
        PACKED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        RETURNED
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
        return status == OrderStatus.DRAFT || 
               status == OrderStatus.PENDING || 
               status == OrderStatus.CONFIRMED || 
               status == OrderStatus.PACKED;
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
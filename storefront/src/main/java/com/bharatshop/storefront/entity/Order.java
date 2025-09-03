package com.bharatshop.storefront.entity;

import com.bharatshop.storefront.entity.OrderItem;
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
import java.util.UUID;

@Entity(name = "StorefrontOrder")
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
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID tenantId;
    
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
    
    // Manual builder method to bypass Lombok issues
    public static OrderBuilder builder() {
        return new OrderBuilder();
    }
    
    // Manual setter methods to bypass Lombok issues
    public void setShippingAddressId(Long shippingAddressId) {
        this.shippingAddressId = shippingAddressId;
    }
    
    public void setShippingName(String shippingName) {
        this.shippingName = shippingName;
    }
    
    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }
    
    public void setShippingLine1(String shippingLine1) {
        this.shippingLine1 = shippingLine1;
    }
    
    public void setShippingLine2(String shippingLine2) {
        this.shippingLine2 = shippingLine2;
    }
    
    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }
    
    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }
    
    public void setShippingPincode(String shippingPincode) {
        this.shippingPincode = shippingPincode;
    }
    
    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }
    
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setPaymentGatewaySignature(String paymentGatewaySignature) {
        this.paymentGatewaySignature = paymentGatewaySignature;
    }
    
    public void setPaymentGatewayOrderId(String paymentGatewayOrderId) {
        this.paymentGatewayOrderId = paymentGatewayOrderId;
    }
    
    public void setPaymentGatewayPaymentId(String paymentGatewayPaymentId) {
        this.paymentGatewayPaymentId = paymentGatewayPaymentId;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public void setPackedAt(LocalDateTime packedAt) {
        this.packedAt = packedAt;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }
    
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    // Manual OrderBuilder class
    public static class OrderBuilder {
        private UUID tenantId;
        private Long customerId;
        private OrderStatus status;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal shippingAmount;
        private PaymentStatus paymentStatus;
        private Long shippingAddressId;
        private String orderNumber;
        private String notes;
        private java.util.List<OrderItem> items;
        
        public OrderBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public OrderBuilder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public OrderBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }
        
        public OrderBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }
        
        public OrderBuilder discountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }
        
        public OrderBuilder taxAmount(BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }
        
        public OrderBuilder shippingAmount(BigDecimal shippingAmount) {
            this.shippingAmount = shippingAmount;
            return this;
        }
        
        public OrderBuilder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }
        
        public OrderBuilder shippingAddressId(Long shippingAddressId) {
            this.shippingAddressId = shippingAddressId;
            return this;
        }
        
        public OrderBuilder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }
        
        public OrderBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public OrderBuilder items(java.util.List<OrderItem> items) {
            this.items = items;
            return this;
        }
        
        public Order build() {
            Order order = new Order();
            order.tenantId = this.tenantId;
            order.customerId = this.customerId;
            order.status = this.status;
            order.totalAmount = this.totalAmount;
            order.discountAmount = this.discountAmount;
            order.taxAmount = this.taxAmount;
            order.shippingAmount = this.shippingAmount;
            order.paymentStatus = this.paymentStatus;
            order.shippingAddressId = this.shippingAddressId;
            order.orderNumber = this.orderNumber;
            order.notes = this.notes;
            order.items = this.items;
            return order;
        }
    }
    
    // Manual getter methods to bypass Lombok issues
    public Long getCustomerId() {
        return customerId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }
    
    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }
    
    public Long getShippingAddressId() {
        return shippingAddressId;
    }
    
    public String getShippingName() {
        return shippingName;
    }
    
    public String getShippingPhone() {
        return shippingPhone;
    }
    
    public String getShippingLine1() {
        return shippingLine1;
    }
    
    public String getShippingLine2() {
        return shippingLine2;
    }
    
    public String getShippingCity() {
        return shippingCity;
    }
    
    public String getShippingState() {
        return shippingState;
    }
    
    public String getShippingPincode() {
        return shippingPincode;
    }
    
    public String getShippingCountry() {
        return shippingCountry;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public String getPaymentGatewayId() {
        return paymentGatewayId;
    }
    
    public String getPaymentGatewayOrderId() {
        return paymentGatewayOrderId;
    }
    
    public String getPaymentGatewayPaymentId() {
        return paymentGatewayPaymentId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public LocalDateTime getPackedAt() {
        return packedAt;
    }
    
    public LocalDateTime getShippedAt() {
        return shippedAt;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public String getCourierPartner() {
        return courierPartner;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
}
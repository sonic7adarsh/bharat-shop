package com.bharatshop.storefront.dto;

import com.bharatshop.storefront.entity.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    private String orderNumber;
    private Long customerId;
    private Order.OrderStatus status;
    private Order.PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal subtotal;
    
    // Shipping Address Information
    private Long shippingAddressId;
    private String shippingName;
    private String shippingPhone;
    private String shippingLine1;
    private String shippingLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String shippingCountry;
    private String fullShippingAddress;
    private String shortShippingAddress;
    
    private String notes;
    private List<OrderItemResponse> items;
    private Integer totalItems;
    private String paymentGatewayId;
    private String paymentGatewayOrderId;
    private String paymentGatewayPaymentId;
    private boolean canBeCancelled;
    private boolean isPaymentCompleted;
    private boolean isDelivered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime packedAt;
    private LocalDateTime shippedAt;
    private String trackingNumber;
    private String courierPartner;
    private boolean canBePacked;
    private boolean canBeShipped;
    
    // Manual builder method to bypass Lombok issues
    public static OrderResponseBuilder builder() {
        return new OrderResponseBuilder();
    }
    
    public static class OrderResponseBuilder {
        private Long id;
        private String orderNumber;
        private Long customerId;
        private Order.OrderStatus status;
        private Order.PaymentStatus paymentStatus;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal shippingAmount;
        private BigDecimal subtotal;
        private Long shippingAddressId;
        private String shippingName;
        private String shippingPhone;
        private String shippingLine1;
        private String shippingLine2;
        private String shippingCity;
        private String shippingState;
        private String shippingPincode;
        private String shippingCountry;
        private String fullShippingAddress;
        private String shortShippingAddress;
        private String notes;
        private List<OrderItemResponse> items;
        private Integer totalItems;
        private String paymentGatewayId;
        private String paymentGatewayOrderId;
        private String paymentGatewayPaymentId;
        private boolean canBeCancelled;
        private boolean isPaymentCompleted;
        private boolean isDelivered;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deliveredAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime packedAt;
        private LocalDateTime shippedAt;
        private String trackingNumber;
        private String courierPartner;
        private boolean canBePacked;
        private boolean canBeShipped;
        
        public OrderResponseBuilder id(Long id) { this.id = id; return this; }
        public OrderResponseBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public OrderResponseBuilder customerId(Long customerId) { this.customerId = customerId; return this; }
        public OrderResponseBuilder status(Order.OrderStatus status) { this.status = status; return this; }
        public OrderResponseBuilder paymentStatus(Order.PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public OrderResponseBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public OrderResponseBuilder discountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
        public OrderResponseBuilder taxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; return this; }
        public OrderResponseBuilder shippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; return this; }
        public OrderResponseBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public OrderResponseBuilder shippingAddressId(Long shippingAddressId) { this.shippingAddressId = shippingAddressId; return this; }
        public OrderResponseBuilder shippingName(String shippingName) { this.shippingName = shippingName; return this; }
        public OrderResponseBuilder shippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; return this; }
        public OrderResponseBuilder shippingLine1(String shippingLine1) { this.shippingLine1 = shippingLine1; return this; }
        public OrderResponseBuilder shippingLine2(String shippingLine2) { this.shippingLine2 = shippingLine2; return this; }
        public OrderResponseBuilder shippingCity(String shippingCity) { this.shippingCity = shippingCity; return this; }
        public OrderResponseBuilder shippingState(String shippingState) { this.shippingState = shippingState; return this; }
        public OrderResponseBuilder shippingPincode(String shippingPincode) { this.shippingPincode = shippingPincode; return this; }
        public OrderResponseBuilder shippingCountry(String shippingCountry) { this.shippingCountry = shippingCountry; return this; }
        public OrderResponseBuilder fullShippingAddress(String fullShippingAddress) { this.fullShippingAddress = fullShippingAddress; return this; }
        public OrderResponseBuilder shortShippingAddress(String shortShippingAddress) { this.shortShippingAddress = shortShippingAddress; return this; }
        public OrderResponseBuilder notes(String notes) { this.notes = notes; return this; }
        public OrderResponseBuilder items(List<OrderItemResponse> items) { this.items = items; return this; }
        public OrderResponseBuilder totalItems(Integer totalItems) { this.totalItems = totalItems; return this; }
        public OrderResponseBuilder paymentGatewayId(String paymentGatewayId) { this.paymentGatewayId = paymentGatewayId; return this; }
        public OrderResponseBuilder paymentGatewayOrderId(String paymentGatewayOrderId) { this.paymentGatewayOrderId = paymentGatewayOrderId; return this; }
        public OrderResponseBuilder paymentGatewayPaymentId(String paymentGatewayPaymentId) { this.paymentGatewayPaymentId = paymentGatewayPaymentId; return this; }
        public OrderResponseBuilder canBeCancelled(boolean canBeCancelled) { this.canBeCancelled = canBeCancelled; return this; }
        public OrderResponseBuilder isPaymentCompleted(boolean isPaymentCompleted) { this.isPaymentCompleted = isPaymentCompleted; return this; }
        public OrderResponseBuilder isDelivered(boolean isDelivered) { this.isDelivered = isDelivered; return this; }
        public OrderResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public OrderResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public OrderResponseBuilder deliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; return this; }
        public OrderResponseBuilder cancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; return this; }
        public OrderResponseBuilder packedAt(LocalDateTime packedAt) { this.packedAt = packedAt; return this; }
        public OrderResponseBuilder shippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; return this; }
        public OrderResponseBuilder trackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; return this; }
        public OrderResponseBuilder courierPartner(String courierPartner) { this.courierPartner = courierPartner; return this; }
        public OrderResponseBuilder canBePacked(boolean canBePacked) { this.canBePacked = canBePacked; return this; }
        public OrderResponseBuilder canBeShipped(boolean canBeShipped) { this.canBeShipped = canBeShipped; return this; }
        
        public OrderResponse build() {
            OrderResponse response = new OrderResponse();
            response.id = this.id;
            response.orderNumber = this.orderNumber;
            response.customerId = this.customerId;
            response.status = this.status;
            response.paymentStatus = this.paymentStatus;
            response.totalAmount = this.totalAmount;
            response.discountAmount = this.discountAmount;
            response.taxAmount = this.taxAmount;
            response.shippingAmount = this.shippingAmount;
            response.subtotal = this.subtotal;
            response.shippingAddressId = this.shippingAddressId;
            response.shippingName = this.shippingName;
            response.shippingPhone = this.shippingPhone;
            response.shippingLine1 = this.shippingLine1;
            response.shippingLine2 = this.shippingLine2;
            response.shippingCity = this.shippingCity;
            response.shippingState = this.shippingState;
            response.shippingPincode = this.shippingPincode;
            response.shippingCountry = this.shippingCountry;
            response.fullShippingAddress = this.fullShippingAddress;
            response.shortShippingAddress = this.shortShippingAddress;
            response.notes = this.notes;
            response.items = this.items;
            response.totalItems = this.totalItems;
            response.paymentGatewayId = this.paymentGatewayId;
            response.paymentGatewayOrderId = this.paymentGatewayOrderId;
            response.paymentGatewayPaymentId = this.paymentGatewayPaymentId;
            response.canBeCancelled = this.canBeCancelled;
            response.isPaymentCompleted = this.isPaymentCompleted;
            response.isDelivered = this.isDelivered;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            response.deliveredAt = this.deliveredAt;
            response.cancelledAt = this.cancelledAt;
            response.packedAt = this.packedAt;
            response.shippedAt = this.shippedAt;
            response.trackingNumber = this.trackingNumber;
            response.courierPartner = this.courierPartner;
            response.canBePacked = this.canBePacked;
            response.canBeShipped = this.canBeShipped;
            return response;
        }
    }
    
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null ? 
                order.getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList()) : 
                List.of();
        
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .subtotal(order.getSubtotal())
                .shippingAddressId(order.getShippingAddressId())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingLine1(order.getShippingLine1())
                .shippingLine2(order.getShippingLine2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPincode(order.getShippingPincode())
                .shippingCountry(order.getShippingCountry())
                .fullShippingAddress(order.getFullShippingAddress())
                .shortShippingAddress(order.getShortShippingAddress())
                .notes(order.getNotes())
                .items(itemResponses)
                .totalItems(order.getTotalItems())
                .paymentGatewayId(order.getPaymentGatewayId())
                .paymentGatewayOrderId(order.getPaymentGatewayOrderId())
                .paymentGatewayPaymentId(order.getPaymentGatewayPaymentId())
                .canBeCancelled(order.canBeCancelled())
                .isPaymentCompleted(order.isPaymentCompleted())
                .isDelivered(order.isDelivered())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .packedAt(order.getPackedAt())
                .shippedAt(order.getShippedAt())
                .trackingNumber(order.getTrackingNumber())
                .courierPartner(order.getCourierPartner())
                .canBePacked(order.canBePacked())
                .canBeShipped(order.canBeShipped())
                .build();
    }
}
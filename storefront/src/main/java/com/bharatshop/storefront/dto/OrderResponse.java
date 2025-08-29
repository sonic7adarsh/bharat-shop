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
@Builder
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
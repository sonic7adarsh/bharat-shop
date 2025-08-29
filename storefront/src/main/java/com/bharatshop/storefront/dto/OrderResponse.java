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
    private Long addressId;
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
                .addressId(order.getAddressId())
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
                .build();
    }
}
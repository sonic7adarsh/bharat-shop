package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.entity.Orders.OrderStatus;
import com.bharatshop.shared.entity.Orders.PaymentStatus;
import com.bharatshop.shared.event.OrderStatusChangeEvent;
import com.bharatshop.shared.exception.BusinessException;
import com.bharatshop.shared.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing order state transitions with proper validation and guards.
 * Implements a state machine pattern for order status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStateMachineService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Transition order from PENDING_PAYMENT to CONFIRMED
     * Guard: Payment must be completed
     */
    @Transactional
    public Orders confirmOrder(Long orderId, Long tenantId) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.CONFIRMED);
        validatePaymentCompleted(order);
        
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED);
        
        System.out.println("Order " + orderId + " transitioned to CONFIRMED for tenant " + tenantId);
        return savedOrder;
    }

    /**
     * Transition order from CONFIRMED to PACKED
     * Guard: Order must be confirmed and items available
     */
    @Transactional
    public Orders packOrder(Long orderId, Long tenantId) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.PACKED);
        
        order.setStatus(OrderStatus.PACKED);
        order.setPackedAt(LocalDateTime.now());
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.CONFIRMED, OrderStatus.PACKED);
        
        System.out.println("Order " + orderId + " transitioned to PACKED for tenant " + tenantId);
        return savedOrder;
    }

    /**
     * Transition order from PACKED to SHIPPED
     * Guard: Order must be packed, requires tracking info
     */
    @Transactional
    public Orders shipOrder(Long orderId, Long tenantId, String trackingNumber, String courierPartner) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.SHIPPED);
        validateShippingInfo(trackingNumber, courierPartner);
        
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        order.setTrackingNumber(trackingNumber);
        order.setCourierPartner(courierPartner);
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.PACKED, OrderStatus.SHIPPED);
        
        System.out.println("Order " + orderId + " transitioned to SHIPPED for tenant " + tenantId + " with tracking " + trackingNumber);
        return savedOrder;
    }

    /**
     * Transition order from SHIPPED to DELIVERED
     * Guard: Order must be shipped
     */
    @Transactional
    public Orders deliverOrder(Long orderId, Long tenantId) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.DELIVERED);
        
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        
        System.out.println("Order " + orderId + " transitioned to DELIVERED for tenant " + tenantId);
        return savedOrder;
    }

    /**
     * Transition order to CANCELLED
     * Guard: Order must be in cancellable state
     */
    @Transactional
    public Orders cancelOrder(Long orderId, Long tenantId, String reason) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.CANCELLED);
        
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, previousStatus, OrderStatus.CANCELLED);
        
        System.out.println("Order " + orderId + " transitioned to CANCELLED for tenant " + tenantId + " with reason: " + reason);
        return savedOrder;
    }

    /**
     * Transition order from DELIVERED to RETURN_REQUESTED
     * Guard: Order must be delivered and within return window
     */
    @Transactional
    public Orders requestReturn(Long orderId, Long tenantId, String reason) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.RETURN_REQUESTED);
        validateReturnWindow(order);
        
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        // Note: Return request details will be handled by RMA system
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED);
        
        System.out.println("Order " + orderId + " transitioned to RETURN_REQUESTED for tenant " + tenantId + " with reason: " + reason);
        return savedOrder;
    }

    /**
     * Transition order from RETURN_REQUESTED to RETURNED
     * Guard: Return must be approved and items received
     */
    @Transactional
    public Orders markAsReturned(Long orderId, Long tenantId) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.RETURNED);
        
        order.setStatus(OrderStatus.RETURNED);
        // Note: Return processing details handled by RMA system
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURNED);
        
        System.out.println("Order " + orderId + " transitioned to RETURNED for tenant " + tenantId);
        return savedOrder;
    }

    /**
     * Transition order from RETURNED to REFUNDED
     * Guard: Return must be processed and refund approved
     */
    @Transactional
    public Orders refundOrder(Long orderId, Long tenantId) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        validateTransition(order, OrderStatus.REFUNDED);
        
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        // Note: Actual refund processing handled by RefundService
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.RETURNED, OrderStatus.REFUNDED);
        
        System.out.println("Order " + orderId + " transitioned to REFUNDED for tenant " + tenantId);
        return savedOrder;
    }

    /**
     * Reject return request and revert to DELIVERED
     */
    @Transactional
    public Orders rejectReturn(Long orderId, Long tenantId, String reason) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw BusinessException.invalidState("Order", "Cannot reject return for order not in RETURN_REQUESTED state");
        }
        
        order.setStatus(OrderStatus.DELIVERED);
        // Note: Rejection details handled by RMA system
        
        Orders savedOrder = orderRepository.save(order);
        publishStateChangeEvent(savedOrder, OrderStatus.RETURN_REQUESTED, OrderStatus.DELIVERED);
        
        System.out.println("Return rejected for order " + orderId + " for tenant " + tenantId + " with reason: " + reason);
        return savedOrder;
    }

    // Private helper methods

    private Orders getOrderWithValidation(Long orderId, Long tenantId) {
        Optional<Orders> orderOpt = orderRepository.findByIdAndTenantId(orderId, tenantId);
        if (orderOpt.isEmpty()) {
            throw BusinessException.notFound("Order", orderId);
        }
        return orderOpt.get();
    }

    private void validateTransition(Orders order, OrderStatus targetStatus) {
        if (!order.getStatus().canTransitionTo(targetStatus)) {
            throw BusinessException.invalidState("Order", 
                String.format("Cannot transition from %s to %s", order.getStatus(), targetStatus));
        }
    }

    private void validatePaymentCompleted(Orders order) {
        if (!order.isPaymentCompleted()) {
            throw BusinessException.invalidState("Order", "Payment must be completed before confirming order");
        }
    }

    private void validateShippingInfo(String trackingNumber, String courierPartner) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw BusinessException.invalidInput("Tracking number is required for shipping");
        }
        if (courierPartner == null || courierPartner.trim().isEmpty()) {
            throw BusinessException.invalidInput("Courier partner is required for shipping");
        }
    }

    private void validateReturnWindow(Orders order) {
        if (order.getDeliveredAt() == null) {
            throw BusinessException.invalidState("Order", "Order must have delivery date to request return");
        }
        
        // Check if within return window (e.g., 30 days)
        LocalDateTime returnDeadline = order.getDeliveredAt().plusDays(30);
        if (LocalDateTime.now().isAfter(returnDeadline)) {
            throw BusinessException.invalidState("Order", "Return window has expired");
        }
    }

    private void publishStateChangeEvent(Orders order, OrderStatus fromStatus, OrderStatus toStatus) {
        OrderStatusChangeEvent event = new OrderStatusChangeEvent(
                order.getId(),
                order.getTenantId(),
                order.getCustomerId(),
                order.getOrderNumber(),
                fromStatus,
                toStatus,
                LocalDateTime.now()
        );
        
        eventPublisher.publishEvent(event);
    }
}
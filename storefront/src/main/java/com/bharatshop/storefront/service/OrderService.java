package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.CustomerAddress;
import com.bharatshop.shared.entity.Payment;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.entity.Reservation;
import com.bharatshop.shared.repository.CustomerAddressRepository;
import com.bharatshop.storefront.repository.StorefrontProductRepository;
import com.bharatshop.shared.repository.ProductVariantRepository;
import com.bharatshop.shared.service.FeatureFlagService;
import com.bharatshop.shared.service.ReservationService;
import com.bharatshop.shared.service.OrderStateMachineService;
import com.bharatshop.shared.entity.Cart;
import com.bharatshop.storefront.repository.StorefrontOrderItemRepository;
import com.bharatshop.storefront.repository.StorefrontOrderRepository;
import com.bharatshop.shared.entity.CartItem;
import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    @Qualifier("storefrontOrderRepository")
    private final StorefrontOrderRepository orderRepository;
    @Qualifier("storefrontOrderItemRepository")
    private final StorefrontOrderItemRepository orderItemRepository;
    @Qualifier("storefrontProductRepository")
    private final StorefrontProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final AddressService addressService;
    private final CustomerAddressRepository customerAddressRepository;
    private final FeatureFlagService featureFlagService;
    private final ReservationService reservationService;
    private final OrderStateMachineService orderStateMachineService;
    
    /**
     * Create order from cart (checkout)
     */
    public Orders createOrderFromCart(Long customerId, Long tenantId, Long addressId, String notes) {
        // Check order limit before creating
        int currentOrderCount = orderRepository.countByTenantId(tenantId).intValue();
        featureFlagService.enforceOrderLimit(tenantId, currentOrderCount);
        
        // Get customer's cart
        Cart cart = cartService.getOrCreateCart(customerId, tenantId);
        
        if (cart.isEmpty()) {
            throw new RuntimeException("Cannot create order from empty cart");
        }
        
        // Validate cart before checkout
        cartService.validateCartForCheckout(customerId, tenantId);
        
        // Calculate order totals
        BigDecimal subtotal = calculateSubtotal(cart);
        
        // Apply cart discount if coupon is applied
        BigDecimal discountAmount = cart.hasCouponApplied() ? cart.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);
        
        BigDecimal taxAmount = calculateTax(discountedSubtotal);
        BigDecimal shippingAmount = calculateShipping(discountedSubtotal);
        BigDecimal totalAmount = discountedSubtotal.add(taxAmount).add(shippingAmount);
        
        // Get and capture shipping address details
        CustomerAddress shippingAddress = null;
        if (addressId != null) {
            shippingAddress = customerAddressRepository.findByIdAndCustomerIdAndTenantId(addressId, customerId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Shipping address not found or inactive"));
        }
        
        // Create order
        Orders order = Orders.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .status(Orders.OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .appliedCoupon(cart.getAppliedCoupon())
                .couponCode(cart.hasCouponApplied() ? cart.getAppliedCoupon().getCode() : null)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .paymentStatus(Orders.PaymentStatus.PENDING)
                .shippingAddressId(addressId)
                .orderNumber(generateOrderNumber())
                .notes(notes)
                .items(new ArrayList<>())
                .build();
        
        // Capture shipping address details for historical reference
        if (shippingAddress != null) {
            order.setShippingAddressId(shippingAddress.getId());
            order.setShippingName(shippingAddress.getName());
            order.setShippingPhone(shippingAddress.getPhone());
            order.setShippingLine1(shippingAddress.getLine1());
            order.setShippingLine2(shippingAddress.getLine2());
            order.setShippingCity(shippingAddress.getCity());
            order.setShippingState(shippingAddress.getState());
            order.setShippingPincode(shippingAddress.getPincode());
            order.setShippingCountry(shippingAddress.getCountry());
        }
        
        order = orderRepository.save(order);
        
        // Create reservations for cart items (atomic stock reservation)
        List<Reservation> reservations = new ArrayList<>();
        
        try {
            for (CartItem cartItem : cart.getItems()) {
                // Reserve stock for each cart item
                if (cartItem.getVariantId() != null) {
                    // Use variant-based reservation
                    Reservation reservation = reservationService.reserveStock(
                        tenantId,
                        cartItem.getVariantId(), 
                        cartItem.getQuantity()
                    );
                    reservations.add(reservation);
                } else {
                    // Fallback for legacy products without variants
                    throw new RuntimeException("Product variants are required for checkout: " + cartItem.getProduct().getName());
                }
            }
        } catch (Exception e) {
            // Release any reservations that were created before the failure
            for (Reservation reservation : reservations) {
                try {
                    reservationService.releaseReservation(tenantId, reservation.getId());
                } catch (Exception releaseException) {
                    log.error("Failed to release reservation {} during rollback", reservation.getId(), releaseException);
                }
            }
            throw e;
        }
        
        // Create order items from cart items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.fromCartItem(cartItem, order);
            orderItems.add(orderItem);
        }
        
        orderItemRepository.saveAll(orderItems);
        order.setItems(orderItems);
        
        // Clear the cart after successful order creation
        cartService.clearCart(customerId, tenantId);
        
        log.info("Order created successfully: {} for customer: {}", order.getOrderNumber(), customerId);
        
        return orderRepository.save(order);
    }
    
    /**
     * Create Razorpay order for payment
     */
    public String createPaymentOrder(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getPaymentStatus() != Orders.PaymentStatus.PENDING) {
            throw new RuntimeException("Order payment is not in pending status");
        }
        
        try {
            String receipt = "order_" + order.getOrderNumber();
            Payment payment = paymentService.createRazorpayOrder(orderId, order.getTotalAmount(), receipt, null);
            
            // Update order with Razorpay order ID
            order.setPaymentGatewayOrderId(payment.getRazorpayOrderId());
            orderRepository.save(order);
            
            String razorpayOrderId = payment.getRazorpayOrderId();
            
            log.info("Razorpay order created for order: {} with Razorpay order ID: {}", order.getOrderNumber(), razorpayOrderId);
            
            return razorpayOrderId;
            
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for order: {}", orderId, e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }
    
    /**
     * Process payment for order
     */
    public Orders processPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getPaymentStatus() == Orders.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order payment already completed");
        }
        
        if (order.getStatus() == Orders.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot process payment for cancelled order");
        }
        
        try {
            // Verify payment with Razorpay through PaymentService
            Payment payment = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            
            // Update order with payment information
            order.setPaymentGatewayOrderId(razorpayOrderId);
            order.setPaymentGatewayPaymentId(razorpayPaymentId);
            order.setPaymentGatewaySignature(razorpaySignature);
            order.setPaymentStatus(Orders.PaymentStatus.COMPLETED);
            
            // Use state machine to transition to CONFIRMED
            order = orderStateMachineService.confirmOrder(order.getId(), order.getTenantId());
            
            // Commit reservations - convert to actual stock decrements
            try {
                reservationService.commitReservations(order.getTenantId(), order.getId());
                log.info("Reservations committed for order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to commit reservations for order: {}", order.getOrderNumber(), e);
                // Note: Payment was successful but reservation commit failed
                // This should be handled by admin intervention or retry mechanism
            }
            
            log.info("Payment processed successfully for order: {} with payment ID: {}", order.getOrderNumber(), payment.getId());
            
            return orderRepository.save(order);
            
        } catch (Exception e) {
            log.error("Payment verification failed for order: {}", orderId, e);
            order.setPaymentStatus(Orders.PaymentStatus.FAILED);
            orderRepository.save(order);
            
            // Release reservations since payment failed
            try {
                reservationService.releaseOrderReservations(order.getTenantId(), order.getId());
                log.info("Reservations released for failed payment on order: {}", order.getOrderNumber());
            } catch (Exception releaseException) {
                log.error("Failed to release reservations for failed payment on order: {}", order.getOrderNumber(), releaseException);
            }
            
            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Get order by ID for customer
     */
    public Optional<Orders> getOrderById(Long orderId, Long customerId, Long tenantId) {
        return orderRepository.findByIdAndCustomerIdAndTenantId(orderId, customerId, tenantId);
    }
    
    /**
     * Get order by ID (for payment processing)
     */
    public Optional<Orders> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
    
    /**
     * Confirm order after successful payment
     */
    public Orders confirmOrder(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getStatus() == Orders.OrderStatus.CONFIRMED) {
            log.info("Order already confirmed: {}", order.getOrderNumber());
            return order;
        }
        
        if (order.getStatus() == Orders.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot confirm cancelled order: " + order.getOrderNumber());
        }
        
        // Use state machine to transition to CONFIRMED
        order = orderStateMachineService.confirmOrder(orderId, order.getTenantId());
        order.setPaymentStatus(Orders.PaymentStatus.COMPLETED);
        
        log.info("Order confirmed after payment: {}", order.getOrderNumber());
        
        return orderRepository.save(order);
    }
    
    /**
     * Get customer orders with pagination
     */
    public Page<Orders> getCustomerOrders(Long customerId, Long tenantId, Pageable pageable) {
        return orderRepository.findByCustomerIdAndTenantIdOrderByCreatedAtDesc(customerId, tenantId, pageable);
    }
    
    /**
     * Get customer orders by status
     */
    public List<Orders> getCustomerOrdersByStatus(Long customerId, Long tenantId, Orders.OrderStatus status) {
        return orderRepository.findByCustomerIdAndTenantIdAndStatusOrderByCreatedAtDesc(customerId, tenantId, status);
    }
    
    /**
     * Cancel order
     */
    public Orders cancelOrder(Long orderId, Long customerId, Long tenantId, String reason) {
        Orders order = orderRepository.findByIdAndCustomerIdAndTenantId(orderId, customerId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.canBeCancelled()) {
            throw new RuntimeException("Order cannot be cancelled in current status: " + order.getStatus());
        }
        
        // Release reservations for cancelled order
        try {
            reservationService.releaseOrderReservations(tenantId, order.getId());
            log.info("Reservations released for cancelled order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to release reservations for cancelled order: {}", order.getOrderNumber(), e);
            // Continue with cancellation even if reservation release fails
        }
        
        // Use state machine to transition to CANCELLED
        order = orderStateMachineService.cancelOrder(orderId, tenantId, reason);
        order.setNotes(order.getNotes() + "\nCancellation reason: " + reason);
        
        log.info("Order cancelled: {} for customer: {}", order.getOrderNumber(), customerId);
        
        return orderRepository.save(order);
    }
    
    /**
     * Mark order as packed
     */
    public Orders markOrderAsPacked(Long orderId, String tenantId) {
        Long tenantIdLong = Long.parseLong(tenantId);
        Orders order = orderRepository.findByIdAndTenantId(orderId, tenantIdLong)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Use state machine to transition to PACKED
        order = orderStateMachineService.packOrder(orderId, tenantIdLong);
        
        log.info("Order marked as packed: {}", order.getOrderNumber());
        
        return order;
    }
    
    /**
     * Mark order as shipped
     */
    public Orders markOrderAsShipped(Long orderId, Long tenantId, String trackingNumber, String courierPartner) {
        Orders order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Use state machine to transition to SHIPPED
        order = orderStateMachineService.shipOrder(orderId, tenantId, trackingNumber, courierPartner);
        
        log.info("Order marked as shipped: {} with tracking: {}", order.getOrderNumber(), trackingNumber);
        
        return order;
    }
    
    /**
     * Mark order as delivered
     */
    public Orders markOrderAsDelivered(Long orderId, Long tenantId) {
        Orders order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Use state machine to transition to DELIVERED
        order = orderStateMachineService.deliverOrder(orderId, tenantId);
        
        log.info("Order marked as delivered: {}", order.getOrderNumber());
        
        return order;
    }
    
    /**
     * Update order status using state machine
     */
    public Orders updateOrderStatus(Long orderId, Long tenantId, Orders.OrderStatus newStatus) {
        Orders order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Orders.OrderStatus currentStatus = order.getStatus();
        
        // Use state machine for transitions
        switch (newStatus) {
            case CONFIRMED:
                order = orderStateMachineService.confirmOrder(orderId, tenantId);
                break;
            case PACKED:
                order = orderStateMachineService.packOrder(orderId, tenantId);
                break;
            case SHIPPED:
                // Note: This requires tracking info, should use markOrderAsShipped instead
                throw new RuntimeException("Use markOrderAsShipped method for SHIPPED status with tracking info");
            case DELIVERED:
                order = orderStateMachineService.deliverOrder(orderId, tenantId);
                break;
            case CANCELLED:
                order = orderStateMachineService.cancelOrder(orderId, tenantId, "Status update");
                break;
            case RETURN_REQUESTED:
                order = orderStateMachineService.requestReturn(orderId, tenantId, "Return requested");
                break;
            case RETURNED:
                order = orderStateMachineService.markAsReturned(orderId, tenantId);
                break;
            case REFUNDED:
                order = orderStateMachineService.refundOrder(orderId, tenantId);
                break;
            default:
                throw new RuntimeException("Unsupported status transition to " + newStatus);
        }
        
        log.info("Order status updated: {} from {} to {}", order.getOrderNumber(), currentStatus, newStatus);
        
        return order;
    }
    
    /**
     * Get order statistics for customer
     */
    public OrderStatistics getCustomerOrderStatistics(Long customerId, String tenantId) {
        Long totalOrders = orderRepository.countByCustomerIdAndTenantId(customerId, Long.parseLong(tenantId));
        Long completedOrders = orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, Long.parseLong(tenantId), Orders.OrderStatus.DELIVERED);
        
        // Calculate total spent by summing order totals
        Page<Orders> customerOrdersPage = orderRepository.findByCustomerIdAndTenantIdOrderByCreatedAtDesc(
                customerId, Long.parseLong(tenantId), Pageable.unpaged());
        List<Orders> customerOrders = customerOrdersPage.getContent();
        BigDecimal totalSpent = customerOrders.stream()
                .map(Orders::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .totalSpent(totalSpent)
                .build();
    }
    
    /**
     * Get recent orders for customer
     */
    public List<Orders> getRecentOrders(Long customerId, String tenantId, int limit) {
        Page<Orders> page = orderRepository.findByCustomerIdAndTenantIdOrderByCreatedAtDesc(customerId, Long.parseLong(tenantId), PageRequest.of(0, limit));
        return page.getContent();
    }
    
    
    /**
     * Check if customer has completed orders
     */
    public boolean hasCompletedOrders(Long customerId, String tenantId) {
        return orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, Long.parseLong(tenantId), Orders.OrderStatus.DELIVERED) > 0;
    }
    
    /**
     * Get pending payment orders older than specified minutes
     */
    public List<Orders> getPendingPaymentOrders(String tenantId, int olderThanMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(olderThanMinutes);
        return orderRepository.findByTenantIdAndPaymentStatusAndCreatedAtBefore(Long.parseLong(tenantId), Orders.PaymentStatus.PENDING, cutoffTime);
    }
    
    // Private helper methods
    
    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simple tax calculation - 18% GST
        BigDecimal taxRate = BigDecimal.valueOf(0.18);
        return subtotal.multiply(taxRate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping for orders above 500
        if (subtotal.compareTo(BigDecimal.valueOf(500)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(50); // Flat shipping rate
    }
    
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + Long.toHexString(System.nanoTime()).substring(0, 8).toUpperCase();
    }
    
    /**
     * @deprecated Use OrderStateMachineService for status transitions instead
     */
    @Deprecated
    private boolean isValidStatusTransition(Orders.OrderStatus from, Orders.OrderStatus to) {
        // Delegate to the OrderStatus enum's canTransitionTo method
        return from.canTransitionTo(to);
    }
    
    // Inner class for order statistics
    public static class OrderStatistics {
        private Long totalOrders;
        private Long completedOrders;
        private BigDecimal totalSpent;
        
        public static OrderStatisticsBuilder builder() {
            return new OrderStatisticsBuilder();
        }
        
        public static class OrderStatisticsBuilder {
            private Long totalOrders;
            private Long completedOrders;
            private BigDecimal totalSpent;
            
            public OrderStatisticsBuilder totalOrders(Long totalOrders) {
                this.totalOrders = totalOrders;
                return this;
            }
            
            public OrderStatisticsBuilder completedOrders(Long completedOrders) {
                this.completedOrders = completedOrders;
                return this;
            }
            
            public OrderStatisticsBuilder totalSpent(BigDecimal totalSpent) {
                this.totalSpent = totalSpent;
                return this;
            }
            
            public OrderStatistics build() {
                OrderStatistics stats = new OrderStatistics();
                stats.totalOrders = this.totalOrders;
                stats.completedOrders = this.completedOrders;
                stats.totalSpent = this.totalSpent;
                return stats;
            }
        }
        
        // Getters
        public Long getTotalOrders() { return totalOrders; }
        public Long getCompletedOrders() { return completedOrders; }
        public BigDecimal getTotalSpent() { return totalSpent; }
    }
}
package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.CustomerAddress;
import com.bharatshop.shared.entity.Payment;
import com.bharatshop.storefront.model.Product;
import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.entity.Reservation;
import com.bharatshop.shared.repository.CustomerAddressRepository;
import com.bharatshop.storefront.repository.StorefrontProductRepository;
import com.bharatshop.shared.repository.ProductVariantRepository;
import com.bharatshop.shared.service.FeatureFlagService;
import com.bharatshop.shared.service.ReservationService;
import com.bharatshop.storefront.entity.*;
import com.bharatshop.shared.entity.Cart;
import com.bharatshop.storefront.repository.StorefrontOrderItemRepository;
import com.bharatshop.storefront.repository.StorefrontOrderRepository;
import com.bharatshop.shared.entity.CartItem;
import com.bharatshop.shared.entity.OrderItem;
import com.bharatshop.storefront.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {
    
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
    
    /**
     * Create order from cart (checkout)
     */
    public Order createOrderFromCart(Long customerId, Long tenantId, Long addressId, String notes) {
        // Check order limit before creating
        UUID tenantUuid = UUID.fromString(tenantId.toString());
        int currentOrderCount = orderRepository.countByTenantId(tenantId).intValue();
        featureFlagService.enforceOrderLimit(tenantUuid, currentOrderCount);
        
        // Get customer's cart
        Cart cart = cartService.getOrCreateCart(customerId, tenantId);
        
        if (cart.isEmpty()) {
            throw new RuntimeException("Cannot create order from empty cart");
        }
        
        // Validate cart before checkout
        cartService.validateCartForCheckout(customerId, tenantId);
        
        // Calculate order totals
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal taxAmount = calculateTax(subtotal);
        BigDecimal shippingAmount = calculateShipping(subtotal);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount);
        
        // Get and capture shipping address details
        CustomerAddress shippingAddress = null;
        if (addressId != null) {
            shippingAddress = customerAddressRepository.findByIdAndCustomerIdAndTenantId(addressId, customerId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Shipping address not found or inactive"));
        }
        
        // Create order
        Order order = Order.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .paymentStatus(Order.PaymentStatus.PENDING)
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
                        tenantUuid,
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
                    reservationService.releaseReservation(reservation.getId(), tenantUuid);
                } catch (Exception releaseException) {
                    log.error("Failed to release reservation {} during rollback", reservation.getId(), releaseException);
                }
            }
            throw e;
        }
        
        // Create order items from cart items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            // Create OrderItem manually since fromCartItem expects shared.entity.Order
            com.bharatshop.shared.entity.Product product = cartItem.getProduct();
            OrderItem orderItem = OrderItem.builder()
                    .order(null) // Will be set after converting to shared.entity.Order
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getUnitPrice())
                    .productName(product.getName())
                    .productSku(product.getSlug())
                    .productImageUrl(product.getImages() != null && !product.getImages().isEmpty() ? 
                        product.getImages().get(0) : null)
                    .variantId(cartItem.getVariantId())
                    .build();
            orderItems.add(orderItem);
        }
        
        // Create a shared.entity.Order for OrderItem relationships
        com.bharatshop.shared.entity.Order sharedOrder = com.bharatshop.shared.entity.Order.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .customerId(order.getCustomerId())
                .orderNumber(order.getOrderNumber())
                .status(com.bharatshop.shared.entity.Order.OrderStatus.valueOf(order.getStatus().name()))
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .paymentStatus(com.bharatshop.shared.entity.Order.PaymentStatus.valueOf(order.getPaymentStatus().name()))
                .shippingAddressId(order.getShippingAddressId())
                .build();
        
        // Set the shared order reference in each OrderItem
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(sharedOrder);
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
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
    public Order processPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order payment already completed");
        }
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot process payment for cancelled order");
        }
        
        try {
            // Verify payment with Razorpay through PaymentService
            Payment payment = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            
            // Update order with payment information
            order.setPaymentGatewayOrderId(razorpayOrderId);
            order.setPaymentGatewayPaymentId(razorpayPaymentId);
            order.setPaymentGatewaySignature(razorpaySignature);
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            
            // Commit reservations - convert to actual stock decrements
            UUID tenantUuid = UUID.fromString(order.getTenantId().toString());
            try {
                reservationService.commitReservations(tenantUuid, order.getId());
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
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderRepository.save(order);
            
            // Release reservations since payment failed
            UUID tenantUuid = UUID.fromString(order.getTenantId().toString());
            try {
                reservationService.releaseOrderReservations(tenantUuid, order.getId());
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
    public Optional<Order> getOrderById(Long orderId, Long customerId, Long tenantId) {
        return orderRepository.findByIdAndCustomerIdAndTenantId(orderId, customerId, tenantId);
    }
    
    /**
     * Get order by ID (for payment processing)
     */
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
    
    /**
     * Confirm order after successful payment
     */
    public Order confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
            log.info("Order already confirmed: {}", order.getOrderNumber());
            return order;
        }
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot confirm cancelled order: " + order.getOrderNumber());
        }
        
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        
        log.info("Order confirmed after payment: {}", order.getOrderNumber());
        
        return orderRepository.save(order);
    }
    
    /**
     * Get customer orders with pagination
     */
    public Page<Order> getCustomerOrders(Long customerId, Long tenantId, Pageable pageable) {
        return orderRepository.findByCustomerIdAndTenantIdOrderByCreatedAtDesc(customerId, tenantId, pageable);
    }
    
    /**
     * Get customer orders by status
     */
    public List<Order> getCustomerOrdersByStatus(Long customerId, Long tenantId, Order.OrderStatus status) {
        return orderRepository.findByCustomerIdAndTenantIdAndStatusOrderByCreatedAtDesc(customerId, tenantId, status);
    }
    
    /**
     * Cancel order
     */
    public Order cancelOrder(Long orderId, Long customerId, Long tenantId, String reason) {
        Order order = orderRepository.findByIdAndCustomerIdAndTenantId(orderId, customerId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.canBeCancelled()) {
            throw new RuntimeException("Order cannot be cancelled in current status: " + order.getStatus());
        }
        
        // Release reservations for cancelled order
        UUID tenantUuid = UUID.fromString(tenantId.toString());
        try {
            reservationService.releaseOrderReservations(tenantUuid, order.getId());
            log.info("Reservations released for cancelled order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to release reservations for cancelled order: {}", order.getOrderNumber(), e);
            // Continue with cancellation even if reservation release fails
        }
        
        order.markAsCancelled();
        order.setNotes(order.getNotes() + "\nCancellation reason: " + reason);
        
        log.info("Order cancelled: {} for customer: {}", order.getOrderNumber(), customerId);
        
        return orderRepository.save(order);
    }
    
    /**
     * Mark order as packed
     */
    public Order markOrderAsPacked(Long orderId, Long tenantId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.canBePacked()) {
            throw new RuntimeException("Order cannot be packed in current status: " + order.getStatus());
        }
        
        order.markAsPacked();
        
        log.info("Order marked as packed: {}", order.getOrderNumber());
        
        return orderRepository.save(order);
    }
    
    /**
     * Mark order as shipped
     */
    public Order markOrderAsShipped(Long orderId, Long tenantId, String trackingNumber, String courierPartner) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.canBeShipped()) {
            throw new RuntimeException("Order cannot be shipped in current status: " + order.getStatus());
        }
        
        order.markAsShipped(trackingNumber, courierPartner);
        
        log.info("Order marked as shipped: {} with tracking: {}", order.getOrderNumber(), trackingNumber);
        
        return orderRepository.save(order);
    }
    
    /**
     * Mark order as delivered
     */
    public Order markOrderAsDelivered(Long orderId, Long tenantId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != Order.OrderStatus.SHIPPED) {
            throw new RuntimeException("Order must be in SHIPPED status to mark as delivered");
        }
        
        order.markAsDelivered();
        
        log.info("Order marked as delivered: {}", order.getOrderNumber());
        
        return orderRepository.save(order);
    }
    
    /**
     * Update order status
     */
    public Order updateOrderStatus(Long orderId, Long tenantId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Order.OrderStatus currentStatus = order.getStatus();
        
        // Validate status transition
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
        
        order.setStatus(newStatus);
        
        // Set appropriate timestamps based on status
        switch (newStatus) {
            case PACKED:
                order.setPackedAt(LocalDateTime.now());
                break;
            case SHIPPED:
                order.setShippedAt(LocalDateTime.now());
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                break;
        }
        
        log.info("Order status updated: {} from {} to {}", order.getOrderNumber(), currentStatus, newStatus);
        
        return orderRepository.save(order);
    }
    
    /**
     * Get order statistics for customer
     */
    public OrderStatistics getCustomerOrderStatistics(Long customerId, Long tenantId) {
        Long totalOrders = orderRepository.countByCustomerIdAndTenantId(customerId, tenantId);
        Long completedOrders = orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, tenantId, Order.OrderStatus.DELIVERED);
        BigDecimal totalSpent = orderRepository.getTotalOrderValueByCustomer(customerId, tenantId);
        
        return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .build();
    }
    
    /**
     * Get recent orders for customer
     */
    public List<Order> getRecentOrders(Long customerId, Long tenantId, int limit) {
        return orderRepository.findRecentOrdersByCustomer(customerId, tenantId, PageRequest.of(0, limit));
    }
    
    /**
     * Check if customer has completed orders
     */
    public boolean hasCompletedOrders(Long customerId, Long tenantId) {
        return orderRepository.hasCompletedOrdersByCustomer(customerId, tenantId);
    }
    
    /**
     * Get pending payment orders older than specified minutes
     */
    public List<Order> getPendingPaymentOrders(Long tenantId, int olderThanMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(olderThanMinutes);
        return orderRepository.findPendingPaymentOrdersOlderThan(tenantId, cutoffTime);
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
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private boolean isValidStatusTransition(Order.OrderStatus from, Order.OrderStatus to) {
        switch (from) {
            case PENDING:
                return to == Order.OrderStatus.CONFIRMED || to == Order.OrderStatus.CANCELLED;
            case CONFIRMED:
                return to == Order.OrderStatus.PACKED || to == Order.OrderStatus.CANCELLED;
            case PACKED:
                return to == Order.OrderStatus.SHIPPED || to == Order.OrderStatus.CANCELLED;
            case SHIPPED:
                return to == Order.OrderStatus.DELIVERED;
            case DELIVERED:
            case CANCELLED:
                return false; // Terminal states
            default:
                return false;
        }
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
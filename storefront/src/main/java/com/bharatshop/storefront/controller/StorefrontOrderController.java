package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.shared.ApiResponse;
import com.bharatshop.storefront.dto.CheckoutRequest;
import com.bharatshop.storefront.dto.OrderResponse;
import com.bharatshop.storefront.dto.PaymentRequest;
import com.bharatshop.storefront.entity.Order;

import com.bharatshop.storefront.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Slf4j
public class StorefrontOrderController {
    
    private final OrderService orderService;
    
    /**
     * Create order from cart (checkout)
     * POST /store/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Order order = orderService.createOrderFromCart(
                    customerId, 
                    tenantId, 
                    request.getAddressId(), 
                    request.getNotes()
            );
            
            OrderResponse response = OrderResponse.fromEntity(order);
            
            log.info("Order created successfully: {} for customer: {}", 
                    order.getOrderNumber(), customerId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Order created successfully")
            );
            
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Create payment order (Razorpay order)
     * POST /store/orders/{id}/payment
     */
    @PostMapping("/orders/{id}/payment")
    public ResponseEntity<ApiResponse<String>> createPaymentOrder(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            // Verify that the order belongs to the customer
            Optional<Order> existingOrder = orderService.getOrderById(id, customerId, tenantId);
            
            if (existingOrder.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Order not found or access denied")
                );
            }
            
            String razorpayOrderId = orderService.createPaymentOrder(id);
            
            log.info("Payment order created for order: {} by customer: {}", id, customerId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(razorpayOrderId, "Payment order created successfully")
            );
            
        } catch (Exception e) {
            log.error("Error creating payment order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Process payment for order
     * POST /store/checkout/pay
     */
    @PostMapping("/checkout/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            // Verify that the order belongs to the customer
            Optional<Order> existingOrder = orderService.getOrderById(
                    request.getOrderId(), customerId, tenantId);
            
            if (existingOrder.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Order not found or access denied")
                );
            }
            
            Order order = orderService.processPayment(
                    request.getOrderId(),
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );
            
            OrderResponse response = OrderResponse.fromEntity(order);
            
            log.info("Payment processed successfully for order: {} by customer: {}", 
                    order.getOrderNumber(), customerId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Payment processed successfully")
            );
            
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get customer orders with pagination
     * GET /store/orders
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orders;
            
            if (status != null) {
                List<Order> orderList = orderService.getCustomerOrdersByStatus(customerId, tenantId, status);
                // Convert List to Page for consistent response
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), orderList.size());
                List<Order> pageContent = orderList.subList(start, end);
                orders = new PageImpl<>(pageContent, pageable, orderList.size());
            } else {
                orders = orderService.getCustomerOrders(customerId, tenantId, pageable);
            }
            
            Page<OrderResponse> response = orders.map(OrderResponse::fromEntity);
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Orders retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get specific order by ID
     * GET /store/orders/{id}
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Optional<Order> order = orderService.getOrderById(id, customerId, tenantId);
            
            if (order.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            OrderResponse response = OrderResponse.fromEntity(order.get());
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Order retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Cancel order
     * POST /store/orders/{id}/cancel
     */
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            String cancellationReason = reason != null ? reason : "Cancelled by customer";
            
            Order order = orderService.cancelOrder(id, customerId, tenantId, cancellationReason);
            OrderResponse response = OrderResponse.fromEntity(order);
            
            log.info("Order cancelled: {} by customer: {}", order.getOrderNumber(), customerId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Order cancelled successfully")
            );
            
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get recent orders for customer
     * GET /store/orders/recent
     */
    @GetMapping("/orders/recent")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            List<Order> orders = orderService.getRecentOrders(customerId, tenantId, limit);
            List<OrderResponse> response = orders.stream()
                    .map(OrderResponse::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Recent orders retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving recent orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get order statistics for customer
     * GET /store/orders/statistics
     */
    @GetMapping("/orders/statistics")
    public ResponseEntity<ApiResponse<OrderService.OrderStatistics>> getOrderStatistics(
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            OrderService.OrderStatistics statistics = orderService.getCustomerOrderStatistics(customerId, tenantId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(statistics, "Order statistics retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving order statistics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Check if customer has completed orders
     * GET /store/orders/has-completed
     */
    @GetMapping("/orders/has-completed")
    public ResponseEntity<ApiResponse<Boolean>> hasCompletedOrders(
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            boolean hasCompleted = orderService.hasCompletedOrders(customerId, tenantId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(hasCompleted, "Completed orders check retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error checking completed orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    // Helper methods to extract customer and tenant information
    // These would typically extract from JWT token or session
    
    private Long extractCustomerId(HttpServletRequest request) {
        // TODO: Extract from JWT token or session
        // For now, using header for testing
        String customerIdHeader = request.getHeader("X-Customer-Id");
        if (customerIdHeader != null) {
            return Long.parseLong(customerIdHeader);
        }
        throw new RuntimeException("Customer ID not found in request");
    }
    
    private Long extractTenantId(HttpServletRequest request) {
        // TODO: Extract from JWT token or session
        // For now, using header for testing
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        if (tenantIdHeader != null) {
            return Long.parseLong(tenantIdHeader);
        }
        throw new RuntimeException("Tenant ID not found in request");
    }
}
package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("storefrontOrderRepository")
public interface StorefrontOrderRepository extends JpaRepository<Orders, Long> {
    
    /**
     * Find order by ID and tenant ID
     */
    Optional<Orders> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find order by ID, customer ID and tenant ID
     */
    Optional<Orders> findByIdAndCustomerIdAndTenantId(Long id, Long customerId, Long tenantId);
    
    /**
     * Find order with items by ID and tenant ID
     * Note: JPA method names don't support JOIN FETCH, this will be handled in service layer
     */
    // This method will be implemented in the service layer using findByIdAndTenantId
    
    /**
     * Find customer orders with pagination
     */
    Page<Orders> findByCustomerIdAndTenantIdOrderByCreatedAtDesc(Long customerId, Long tenantId, Pageable pageable);
    
    /**
     * Find customer orders by status
     */
    List<Orders> findByCustomerIdAndTenantIdAndStatusOrderByCreatedAtDesc(Long customerId, Long tenantId, Orders.OrderStatus status);
    
    /**
     * Find orders by payment status
     */
    List<Orders> findByTenantIdAndPaymentStatusOrderByCreatedAtDesc(Long tenantId, Orders.PaymentStatus paymentStatus);
    
    /**
     * Find orders by status and tenant
     */
    List<Orders> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, Orders.OrderStatus status);
    
    /**
     * Find orders by payment gateway order ID
     */
    Optional<Orders> findByPaymentGatewayOrderIdAndTenantId(String paymentGatewayOrderId, Long tenantId);
    
    /**
     * Find orders by payment gateway payment ID
     */
    Optional<Orders> findByPaymentGatewayPaymentIdAndTenantId(String paymentGatewayPaymentId, Long tenantId);
    
    /**
     * Find orders within date range
     */
    List<Orders> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long tenantId,
                                                                       LocalDateTime startDate,
                                                                       LocalDateTime endDate);
    
    /**
     * Find pending payment orders older than specified time
     */
    List<Orders> findByTenantIdAndPaymentStatusAndCreatedAtBefore(Long tenantId, 
                                                                  Orders.PaymentStatus paymentStatus,
                                                                  LocalDateTime cutoffTime);
    
    /**
     * Count orders by tenant
     */
    Long countByTenantId(Long tenantId);
    
    /**
     * Count orders by customer and tenant
     */
    Long countByCustomerIdAndTenantId(Long customerId, Long tenantId);
    
    /**
     * Count orders by customer, tenant and status
     */
    Long countByCustomerIdAndTenantIdAndStatus(Long customerId, Long tenantId, Orders.OrderStatus status);
    
    /**
     * Count orders by status and tenant
     */
    Long countByTenantIdAndStatus(Long tenantId, Orders.OrderStatus status);
    
    /**
     * Get customer's total order value
     * Note: This requires service layer implementation for SUM operations
     */
    List<Orders> findByCustomerIdAndTenantIdAndPaymentStatus(Long customerId, 
                                                            Long tenantId,
                                                            Orders.PaymentStatus paymentStatus);
    
    /**
     * Get tenant's total revenue
     * Note: This requires service layer implementation for SUM operations
     */
    List<Orders> findByTenantIdAndPaymentStatus(Long tenantId, Orders.PaymentStatus paymentStatus);
    
    /**
     * Get revenue within date range
     * Note: This requires service layer implementation for SUM operations
     */
    List<Orders> findByTenantIdAndPaymentStatusAndCreatedAtBetween(Long tenantId,
                                                                   Orders.PaymentStatus paymentStatus,
                                                                   LocalDateTime startDate,
                                                                   LocalDateTime endDate);
    
    /**
     * Check if customer has any completed orders
     */
    boolean existsByCustomerIdAndTenantIdAndPaymentStatus(Long customerId, 
                                                         Long tenantId,
                                                         Orders.PaymentStatus paymentStatus);
    
    /**
     * Get order statistics for tenant
     * Note: This complex aggregation requires service layer implementation
     */
    // This method will be implemented in the service layer using multiple repository calls
}
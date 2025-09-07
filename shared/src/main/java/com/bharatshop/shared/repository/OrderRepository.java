package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
    
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
     */
    @Query(value = "SELECT o.* FROM orders o " +
           "LEFT JOIN order_items oi ON o.id = oi.order_id " +
           "LEFT JOIN products p ON oi.product_id = p.id " +
           "WHERE o.id = :id AND o.tenant_id = :tenantId", nativeQuery = true)
    Optional<Orders> findByIdAndTenantIdWithItems(@Param("id") Long id, @Param("tenantId") Long tenantId);
    
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
    @Query(value = "SELECT * FROM orders WHERE tenant_id = :tenantId " +
           "AND created_at BETWEEN :startDate AND :endDate " +
           "ORDER BY created_at DESC", nativeQuery = true)
    List<Orders> findOrdersByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find pending payment orders older than specified time
     */
    @Query(value = "SELECT * FROM orders WHERE tenant_id = :tenantId " +
           "AND payment_status = 'PENDING' " +
           "AND created_at < :cutoffTime", nativeQuery = true)
    List<Orders> findPendingPaymentOrdersOlderThan(@Param("tenantId") Long tenantId, 
                                                  @Param("cutoffTime") LocalDateTime cutoffTime);
    
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
     */
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE customer_id = :customerId AND tenant_id = :tenantId " +
           "AND payment_status = 'COMPLETED'", nativeQuery = true)
    BigDecimal getTotalOrderValueByCustomer(@Param("customerId") Long customerId, 
                                           @Param("tenantId") Long tenantId);
    
    /**
     * Get tenant's total revenue
     */
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE tenant_id = :tenantId AND payment_status = 'COMPLETED'", nativeQuery = true)
    BigDecimal getTotalRevenueByTenant(@Param("tenantId") Long tenantId);
    
    /**
     * Get revenue within date range
     */
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE tenant_id = :tenantId " +
           "AND payment_status = 'COMPLETED' " +
           "AND created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal getRevenueByTenantAndDateRange(@Param("tenantId") Long tenantId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find customer's recent orders
     */
    @Query(value = "SELECT * FROM orders WHERE customer_id = :customerId AND tenant_id = :tenantId " +
           "ORDER BY created_at DESC", nativeQuery = true)
    List<Orders> findRecentOrdersByCustomer(@Param("customerId") Long customerId, 
                                          @Param("tenantId") Long tenantId, 
                                          Pageable pageable);
    
    /**
     * Check if customer has any completed orders
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM orders " +
           "WHERE customer_id = :customerId AND tenant_id = :tenantId " +
           "AND payment_status = 'COMPLETED'", nativeQuery = true)
    boolean hasCompletedOrdersByCustomer(@Param("customerId") Long customerId, 
                                        @Param("tenantId") Long tenantId);
    
    /**
     * Get order statistics for tenant
     */
    @Query(value = "SELECT COUNT(*), " +
           "COUNT(CASE WHEN payment_status = 'COMPLETED' THEN 1 END), " +
           "AVG(total_amount), " +
           "SUM(CASE WHEN payment_status = 'COMPLETED' THEN total_amount ELSE 0 END) " +
           "FROM orders WHERE tenant_id = :tenantId", nativeQuery = true)
    Object[] getOrderStatsByTenantId(@Param("tenantId") Long tenantId);
}
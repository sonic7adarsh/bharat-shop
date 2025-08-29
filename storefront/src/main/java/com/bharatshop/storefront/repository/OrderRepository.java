package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.entity.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find order by ID and tenant ID
     */
    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find order by ID, customer ID and tenant ID
     */
    Optional<Order> findByIdAndCustomerIdAndTenantId(Long id, Long customerId, Long tenantId);
    
    /**
     * Find order with items by ID and tenant ID
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items oi LEFT JOIN FETCH oi.product " +
           "WHERE o.id = :id AND o.tenantId = :tenantId")
    Optional<Order> findByIdAndTenantIdWithItems(@Param("id") Long id, @Param("tenantId") Long tenantId);
    
    /**
     * Find customer orders with pagination
     */
    Page<Order> findByCustomerIdAndTenantIdOrderByCreatedAtDesc(Long customerId, Long tenantId, Pageable pageable);
    
    /**
     * Find customer orders by status
     */
    List<Order> findByCustomerIdAndTenantIdAndStatusOrderByCreatedAtDesc(Long customerId, Long tenantId, Order.OrderStatus status);
    
    /**
     * Find orders by payment status
     */
    List<Order> findByTenantIdAndPaymentStatusOrderByCreatedAtDesc(Long tenantId, Order.PaymentStatus paymentStatus);
    
    /**
     * Find orders by status and tenant
     */
    List<Order> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, Order.OrderStatus status);
    
    /**
     * Find orders by payment gateway order ID
     */
    Optional<Order> findByPaymentGatewayOrderIdAndTenantId(String paymentGatewayOrderId, Long tenantId);
    
    /**
     * Find orders by payment gateway payment ID
     */
    Optional<Order> findByPaymentGatewayPaymentIdAndTenantId(String paymentGatewayPaymentId, Long tenantId);
    
    /**
     * Find orders within date range
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY o.createdAt DESC")
    List<Order> findOrdersByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find pending payment orders older than specified time
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.paymentStatus = 'PENDING' " +
           "AND o.createdAt < :cutoffTime")
    List<Order> findPendingPaymentOrdersOlderThan(@Param("tenantId") Long tenantId, 
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
    Long countByCustomerIdAndTenantIdAndStatus(Long customerId, Long tenantId, Order.OrderStatus status);
    
    /**
     * Count orders by status and tenant
     */
    Long countByTenantIdAndStatus(Long tenantId, Order.OrderStatus status);
    
    /**
     * Get customer's total order value
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.customerId = :customerId AND o.tenantId = :tenantId " +
           "AND o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalOrderValueByCustomer(@Param("customerId") Long customerId, 
                                           @Param("tenantId") Long tenantId);
    
    /**
     * Get tenant's total revenue
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.tenantId = :tenantId AND o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenueByTenant(@Param("tenantId") Long tenantId);
    
    /**
     * Get revenue within date range
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.tenantId = :tenantId " +
           "AND o.paymentStatus = 'COMPLETED' " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueByTenantAndDateRange(@Param("tenantId") Long tenantId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find customer's recent orders
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.tenantId = :tenantId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByCustomer(@Param("customerId") Long customerId, 
                                          @Param("tenantId") Long tenantId, 
                                          Pageable pageable);
    
    /**
     * Check if customer has any completed orders
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o " +
           "WHERE o.customerId = :customerId AND o.tenantId = :tenantId " +
           "AND o.paymentStatus = 'COMPLETED'")
    boolean hasCompletedOrdersByCustomer(@Param("customerId") Long customerId, 
                                        @Param("tenantId") Long tenantId);
    
    /**
     * Get order statistics for tenant
     */
    @Query("SELECT COUNT(o), " +
           "COUNT(CASE WHEN o.paymentStatus = 'COMPLETED' THEN 1 END), " +
           "AVG(o.totalAmount), " +
           "SUM(CASE WHEN o.paymentStatus = 'COMPLETED' THEN o.totalAmount ELSE 0 END) " +
           "FROM Order o WHERE o.tenantId = :tenantId")
    Object[] getOrderStatsByTenantId(@Param("tenantId") Long tenantId);
}
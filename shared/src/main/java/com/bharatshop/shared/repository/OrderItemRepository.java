package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find order items by order ID
     */
    @Query(value = "SELECT oi.* FROM order_items oi " +
           "WHERE oi.order_id = :orderId ORDER BY oi.created_at ASC", nativeQuery = true)
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);
    
    /**
     * Find order items by product ID and tenant (through order relationship)
     */
    @Query(value = "SELECT oi.* FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE oi.product_id = :productId AND o.tenant_id = :tenantId " +
           "ORDER BY oi.created_at DESC", nativeQuery = true)
    List<OrderItem> findByProductIdAndTenantId(@Param("productId") Long productId, 
                                              @Param("tenantId") Long tenantId);
    
    /**
     * Find order items by customer and tenant (through order relationship)
     */
    @Query(value = "SELECT oi.* FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.customer_id = :customerId AND o.tenant_id = :tenantId " +
           "ORDER BY oi.created_at DESC", nativeQuery = true)
    List<OrderItem> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                               @Param("tenantId") Long tenantId);
    
    /**
     * Get total quantity sold for a product
     */
    @Query(value = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE oi.product_id = :productId AND o.tenant_id = :tenantId " +
           "AND o.payment_status = 'COMPLETED'", nativeQuery = true)
    Long getTotalQuantitySoldByProduct(@Param("productId") Long productId, 
                                       @Param("tenantId") Long tenantId);
    
    /**
     * Get total revenue for a product
     */
    @Query(value = "SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE oi.product_id = :productId AND o.tenant_id = :tenantId " +
           "AND o.payment_status = 'COMPLETED'", nativeQuery = true)
    BigDecimal getTotalRevenueByProduct(@Param("productId") Long productId, 
                                       @Param("tenantId") Long tenantId);
    
    /**
     * Find top selling products by quantity
     */
    @Query(value = "SELECT oi.product_id, oi.product_name, SUM(oi.quantity) as totalQuantity " +
           "FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.tenant_id = :tenantId AND o.payment_status = 'COMPLETED' " +
           "GROUP BY oi.product_id, oi.product_name " +
           "ORDER BY totalQuantity DESC", nativeQuery = true)
    List<Object[]> findTopSellingProductsByQuantity(@Param("tenantId") Long tenantId);
    
    /**
     * Find top selling products by revenue
     */
    @Query(value = "SELECT oi.product_id, oi.product_name, SUM(oi.price * oi.quantity) as totalRevenue " +
           "FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.tenant_id = :tenantId AND o.payment_status = 'COMPLETED' " +
           "GROUP BY oi.product_id, oi.product_name " +
           "ORDER BY totalRevenue DESC", nativeQuery = true)
    List<Object[]> findTopSellingProductsByRevenue(@Param("tenantId") Long tenantId);
    
    /**
     * Find order items within date range
     */
    @Query(value = "SELECT oi.* FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.tenant_id = :tenantId " +
           "AND o.created_at BETWEEN :startDate AND :endDate " +
           "AND o.payment_status = 'COMPLETED' " +
           "ORDER BY oi.created_at DESC", nativeQuery = true)
    List<OrderItem> findOrderItemsByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count total items in an order
     */
    @Query(value = "SELECT COUNT(oi.id) FROM order_items oi WHERE oi.order_id = :orderId", nativeQuery = true)
    Long countByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Sum total quantity in an order
     */
    @Query(value = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.order_id = :orderId", nativeQuery = true)
    Integer sumQuantityByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Get customer's purchase history for a specific product
     */
    @Query(value = "SELECT oi.* FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.customer_id = :customerId AND o.tenant_id = :tenantId " +
           "AND oi.product_id = :productId " +
           "AND o.payment_status = 'COMPLETED' " +
           "ORDER BY oi.created_at DESC", nativeQuery = true)
    List<OrderItem> findCustomerPurchaseHistoryByProduct(@Param("customerId") Long customerId,
                                                         @Param("tenantId") Long tenantId,
                                                         @Param("productId") Long productId);
    
    /**
     * Check if customer has purchased a specific product
     */
    @Query(value = "SELECT COUNT(oi.id) FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.customer_id = :customerId AND o.tenant_id = :tenantId " +
           "AND oi.product_id = :productId " +
           "AND o.payment_status = 'COMPLETED'", nativeQuery = true)
    Long countCustomerPurchasesForProduct(@Param("customerId") Long customerId,
                                          @Param("tenantId") Long tenantId,
                                          @Param("productId") Long productId);
    
    default boolean hasCustomerPurchasedProduct(Long customerId, Long tenantId, Long productId) {
        return countCustomerPurchasesForProduct(customerId, tenantId, productId) > 0;
    }
}
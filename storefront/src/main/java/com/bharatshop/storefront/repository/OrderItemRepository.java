package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.entity.OrderItem;
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
    @Query("SELECT oi FROM OrderItem oi LEFT JOIN FETCH oi.product " +
           "WHERE oi.order.id = :orderId ORDER BY oi.createdAt ASC")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);
    
    /**
     * Find order items by product ID and tenant (through order relationship)
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE oi.product.id = :productId AND o.tenantId = :tenantId " +
           "ORDER BY oi.createdAt DESC")
    List<OrderItem> findByProductIdAndTenantId(@Param("productId") Long productId, 
                                              @Param("tenantId") Long tenantId);
    
    /**
     * Find order items by customer and tenant (through order relationship)
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN oi.order o " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.customerId = :customerId AND o.tenantId = :tenantId " +
           "ORDER BY oi.createdAt DESC")
    List<OrderItem> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                               @Param("tenantId") Long tenantId);
    
    /**
     * Get total quantity sold for a product
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE oi.product.id = :productId AND o.tenantId = :tenantId " +
           "AND o.paymentStatus = 'COMPLETED'")
    Long getTotalQuantitySoldByProduct(@Param("productId") Long productId, 
                                       @Param("tenantId") Long tenantId);
    
    /**
     * Get total revenue for a product
     */
    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE oi.product.id = :productId AND o.tenantId = :tenantId " +
           "AND o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenueByProduct(@Param("productId") Long productId, 
                                       @Param("tenantId") Long tenantId);
    
    /**
     * Find top selling products by quantity
     */
    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.tenantId = :tenantId AND o.paymentStatus = 'COMPLETED' " +
           "GROUP BY oi.product.id, oi.productName " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProductsByQuantity(@Param("tenantId") Long tenantId);
    
    /**
     * Find top selling products by revenue
     */
    @Query("SELECT oi.product.id, oi.productName, SUM(oi.price * oi.quantity) as totalRevenue " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.tenantId = :tenantId AND o.paymentStatus = 'COMPLETED' " +
           "GROUP BY oi.product.id, oi.productName " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> findTopSellingProductsByRevenue(@Param("tenantId") Long tenantId);
    
    /**
     * Find order items within date range
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.tenantId = :tenantId " +
           "AND o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.paymentStatus = 'COMPLETED' " +
           "ORDER BY oi.createdAt DESC")
    List<OrderItem> findOrderItemsByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count total items in an order
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Long countByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Sum total quantity in an order
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Integer sumQuantityByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Get customer's purchase history for a specific product
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.customerId = :customerId AND o.tenantId = :tenantId " +
           "AND oi.product.id = :productId " +
           "AND o.paymentStatus = 'COMPLETED' " +
           "ORDER BY oi.createdAt DESC")
    List<OrderItem> findCustomerPurchaseHistoryByProduct(@Param("customerId") Long customerId,
                                                         @Param("tenantId") Long tenantId,
                                                         @Param("productId") Long productId);
    
    /**
     * Check if customer has purchased a specific product
     */
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.customerId = :customerId AND o.tenantId = :tenantId " +
           "AND oi.product.id = :productId " +
           "AND o.paymentStatus = 'COMPLETED'")
    boolean hasCustomerPurchasedProduct(@Param("customerId") Long customerId,
                                        @Param("tenantId") Long tenantId,
                                        @Param("productId") Long productId);
}
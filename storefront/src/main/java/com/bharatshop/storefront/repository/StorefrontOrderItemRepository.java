package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("storefrontOrderItemRepository")
public interface StorefrontOrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find order items by order ID
     */
    List<OrderItem> findByOrder_IdOrderByCreatedAtAsc(Long orderId);
    
    /**
     * Find order items by product ID and tenant (through order relationship)
     */
    List<OrderItem> findByProduct_IdAndOrder_TenantIdOrderByCreatedAtDesc(Long productId, 
                                                                          Long tenantId);
    
    /**
     * Find order items by customer and tenant (through order relationship)
     */
    List<OrderItem> findByOrder_CustomerIdAndOrder_TenantIdOrderByCreatedAtDesc(Long customerId, 
                                                                               Long tenantId);
    
    /**
     * Get total quantity sold for a product
     * Note: This requires service layer implementation for SUM operations
     */
    List<OrderItem> findByProduct_IdAndOrder_TenantIdAndOrder_PaymentStatus(Long productId, 
                                                                            Long tenantId,
                                                                            com.bharatshop.shared.entity.Orders.PaymentStatus paymentStatus);
    
    /**
     * Get total revenue for a product
     * Note: This requires service layer implementation for calculation
     */
    // This method will be implemented in the service layer using findByProduct_IdAndOrder_TenantIdAndOrder_PaymentStatus
    
    /**
     * Find top selling products by quantity
     * Note: This complex aggregation requires service layer implementation
     */
    List<OrderItem> findByOrder_TenantIdAndOrder_PaymentStatusOrderByCreatedAtDesc(Long tenantId,
                                                                                   com.bharatshop.shared.entity.Orders.PaymentStatus paymentStatus);
    
    /**
     * Find top selling products by revenue
     * Note: This complex aggregation requires service layer implementation
     */
    // This method will be implemented in the service layer using findByOrder_TenantIdAndOrder_PaymentStatusOrderByCreatedAtDesc
    
    /**
     * Find order items within date range
     */
    List<OrderItem> findByOrder_TenantIdAndOrder_CreatedAtBetweenAndOrder_PaymentStatusOrderByCreatedAtDesc(Long tenantId,
                                                                                                              LocalDateTime startDate,
                                                                                                              LocalDateTime endDate,
                                                                                                              com.bharatshop.shared.entity.Orders.PaymentStatus paymentStatus);
    
    /**
     * Count total items in an order
     */
    Long countByOrder_Id(Long orderId);
    
    /**
     * Sum total quantity in an order
     * Note: This requires service layer implementation for SUM operations
     */
    default Integer sumQuantityByOrderId(Long orderId) {
        return findByOrder_IdOrderByCreatedAtAsc(orderId).stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
    
    /**
     * Get customer's purchase history for a specific product
     */
    List<OrderItem> findByOrder_CustomerIdAndOrder_TenantIdAndProduct_IdAndOrder_PaymentStatusOrderByCreatedAtDesc(Long customerId,
                                                                                                                     Long tenantId,
                                                                                                                     Long productId,
                                                                                                                     com.bharatshop.shared.entity.Orders.PaymentStatus paymentStatus);
    
    /**
     * Check if customer has purchased a specific product
     */
    Long countByOrder_CustomerIdAndOrder_TenantIdAndProduct_IdAndOrder_PaymentStatus(Long customerId,
                                                                                      Long tenantId,
                                                                                      Long productId,
                                                                                      com.bharatshop.shared.entity.Orders.PaymentStatus paymentStatus);
    
    default boolean hasCustomerPurchasedProduct(Long customerId, Long tenantId, Long productId) {
        return countByOrder_CustomerIdAndOrder_TenantIdAndProduct_IdAndOrder_PaymentStatus(
                customerId, tenantId, productId, com.bharatshop.shared.entity.Orders.PaymentStatus.COMPLETED) > 0;
    }
}
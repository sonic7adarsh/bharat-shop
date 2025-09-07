package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * Find cart by customer ID and tenant ID
     */
    Optional<Cart> findByCustomerIdAndTenantId(Long customerId, Long tenantId);
    
    /**
     * Find cart with items by customer ID and tenant ID
     */
    @Query(value = "SELECT c.* FROM carts c " +
           "LEFT JOIN cart_items ci ON c.id = ci.cart_id " +
           "LEFT JOIN products p ON ci.product_id = p.id " +
           "WHERE c.customer_id = :customerId AND c.tenant_id = :tenantId", nativeQuery = true)
    Optional<Cart> findByCustomerIdAndTenantIdWithItems(@Param("customerId") Long customerId, 
                                                        @Param("tenantId") Long tenantId);
    
    /**
     * Find all carts for a tenant
     */
    List<Cart> findByTenantId(Long tenantId);
    
    /**
     * Find carts by tenant ID with pagination
     */
    @Query(value = "SELECT * FROM carts WHERE tenant_id = :tenantId ORDER BY updated_at DESC", nativeQuery = true)
    List<Cart> findByTenantIdOrderByUpdatedAtDesc(@Param("tenantId") Long tenantId);
    
    /**
     * Find abandoned carts (not updated for specified days)
     */
    @Query(value = "SELECT c.* FROM carts c " +
           "WHERE c.tenant_id = :tenantId " +
           "AND c.updated_at < :cutoffDate " +
           "AND EXISTS (SELECT 1 FROM cart_items ci WHERE ci.cart_id = c.id)", nativeQuery = true)
    List<Cart> findAbandonedCarts(@Param("tenantId") Long tenantId, 
                                  @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count total items in cart
     */
    @Query(value = "SELECT COALESCE(SUM(ci.quantity), 0) FROM carts c " +
           "JOIN cart_items ci ON c.id = ci.cart_id " +
           "WHERE c.customer_id = :customerId AND c.tenant_id = :tenantId", nativeQuery = true)
    Integer countItemsInCart(@Param("customerId") Long customerId, 
                            @Param("tenantId") Long tenantId);
    
    /**
     * Check if cart exists and is not empty
     */
    @Query(value = "SELECT CASE WHEN COUNT(c.id) > 0 THEN true ELSE false END FROM carts c " +
           "WHERE c.customer_id = :customerId AND c.tenant_id = :tenantId " +
           "AND EXISTS (SELECT 1 FROM cart_items ci WHERE ci.cart_id = c.id)", nativeQuery = true)
    boolean existsNonEmptyCartByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                     @Param("tenantId") Long tenantId);
    
    /**
     * Delete empty carts for a tenant
     */
    @Query(value = "DELETE FROM carts WHERE tenant_id = :tenantId AND id NOT IN (SELECT DISTINCT cart_id FROM cart_items WHERE cart_id IS NOT NULL)", nativeQuery = true)
    void deleteEmptyCartsByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Find carts created within date range
     */
    @Query(value = "SELECT * FROM carts WHERE tenant_id = :tenantId " +
           "AND created_at BETWEEN :startDate AND :endDate " +
           "ORDER BY created_at DESC", nativeQuery = true)
    List<Cart> findCartsByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get cart statistics for a tenant
     */
    @Query(value = "SELECT COUNT(c.id), " +
           "COUNT(CASE WHEN EXISTS(SELECT 1 FROM cart_items ci WHERE ci.cart_id = c.id) THEN 1 END), " +
           "AVG((SELECT COUNT(*) FROM cart_items ci WHERE ci.cart_id = c.id)) " +
           "FROM carts c WHERE c.tenant_id = :tenantId", nativeQuery = true)
    Object[] getCartStatsByTenantId(@Param("tenantId") Long tenantId);
}
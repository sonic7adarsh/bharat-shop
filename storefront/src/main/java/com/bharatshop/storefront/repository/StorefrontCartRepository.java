package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("storefrontCartRepository")
public interface StorefrontCartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * Find cart by customer ID and tenant ID
     */
    Optional<Cart> findByCustomerIdAndTenantId(Long customerId, UUID tenantId);
    
    /**
     * Find cart with items by customer ID and tenant ID
     */
    @Query("SELECT c FROM StorefrontCart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId")
    Optional<Cart> findByCustomerIdAndTenantIdWithItems(@Param("customerId") Long customerId, 
                                                       @Param("tenantId") UUID tenantId);
    
    /**
     * Find all carts for a tenant
     */
    List<Cart> findByTenantId(UUID tenantId);
    
    /**
     * Find carts by tenant ID with pagination
     */
    @Query("SELECT c FROM StorefrontCart c WHERE c.tenantId = :tenantId ORDER BY c.updatedAt DESC")
    List<Cart> findByTenantIdOrderByUpdatedAtDesc(@Param("tenantId") UUID tenantId);
    
    /**
     * Find abandoned carts (not updated for specified days)
     */
    @Query("SELECT c FROM Cart c WHERE c.tenantId = :tenantId " +
           "AND c.updatedAt < :cutoffDate " +
           "AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCarts(@Param("tenantId") UUID tenantId, 
                                  @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count total items in cart
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM StorefrontCart c " +
           "JOIN c.items ci " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId")
    Integer countItemsInCart(@Param("customerId") Long customerId, 
                            @Param("tenantId") UUID tenantId);
    
    /**
     * Check if cart exists and is not empty
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM StorefrontCart c " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId " +
           "AND SIZE(c.items) > 0")
    boolean existsNonEmptyCartByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                     @Param("tenantId") UUID tenantId);
    
    /**
     * Delete empty carts for a tenant
     */
    @Query("DELETE FROM StorefrontCart c WHERE c.tenantId = :tenantId AND SIZE(c.items) = 0")
    void deleteEmptyCartsByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Find carts created within date range
     */
    @Query("SELECT c FROM StorefrontCart c WHERE c.tenantId = :tenantId " +
           "AND c.updatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.updatedAt DESC")
    List<Cart> findByTenantIdAndUpdatedAtBetween(@Param("tenantId") UUID tenantId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get cart statistics for a tenant
     */
    @Query("SELECT COUNT(c), " +
           "COUNT(CASE WHEN SIZE(c.items) > 0 THEN 1 END), " +
           "AVG(SIZE(c.items)) " +
           "FROM StorefrontCart c WHERE c.tenantId = :tenantId")
    Object[] getCartStatsByTenantId(@Param("tenantId") UUID tenantId);
}
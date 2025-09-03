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
    @Query("SELECT c FROM SharedCart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId")
    Optional<Cart> findByCustomerIdAndTenantIdWithItems(@Param("customerId") Long customerId, 
                                                        @Param("tenantId") Long tenantId);
    
    /**
     * Find all carts for a tenant
     */
    List<Cart> findByTenantId(Long tenantId);
    
    /**
     * Find carts by tenant ID with pagination
     */
    @Query("SELECT c FROM SharedCart c WHERE c.tenantId = :tenantId ORDER BY c.updatedAt DESC")
    List<Cart> findByTenantIdOrderByUpdatedAtDesc(@Param("tenantId") Long tenantId);
    
    /**
     * Find abandoned carts (not updated for specified days)
     */
    @Query("SELECT c FROM SharedCart c WHERE c.tenantId = :tenantId " +
           "AND c.updatedAt < :cutoffDate " +
           "AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCarts(@Param("tenantId") Long tenantId, 
                                  @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count total items in cart
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM SharedCart c " +
           "JOIN c.items ci " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId")
    Integer countItemsInCart(@Param("customerId") Long customerId, 
                            @Param("tenantId") Long tenantId);
    
    /**
     * Check if cart exists and is not empty
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM SharedCart c " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId " +
           "AND SIZE(c.items) > 0")
    boolean existsNonEmptyCartByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                     @Param("tenantId") Long tenantId);
    
    /**
     * Delete empty carts for a tenant
     */
    @Query("DELETE FROM SharedCart c WHERE c.tenantId = :tenantId AND SIZE(c.items) = 0")
    void deleteEmptyCartsByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Find carts created within date range
     */
    @Query("SELECT c FROM SharedCart c WHERE c.tenantId = :tenantId " +
           "AND c.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.createdAt DESC")
    List<Cart> findCartsByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get cart statistics for a tenant
     */
    @Query("SELECT COUNT(c), " +
           "COUNT(CASE WHEN SIZE(c.items) > 0 THEN 1 END), " +
           "AVG(SIZE(c.items)) " +
           "FROM SharedCart c WHERE c.tenantId = :tenantId")
    Object[] getCartStatsByTenantId(@Param("tenantId") Long tenantId);
}
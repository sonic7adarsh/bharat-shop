package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Wishlist entity operations.
 * Provides methods for managing customer wishlists with tenant isolation.
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    /**
     * Find all active wishlist items for a customer in a specific tenant
     */
    @Query(value = "SELECT * FROM wishlists w WHERE w.customer_id = :customerId AND w.tenant_id = :tenantId " +
           "AND w.is_active = true AND w.deleted_at IS NULL ORDER BY w.created_at DESC", nativeQuery = true)
    List<Wishlist> findActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                    @Param("tenantId") Long tenantId);
    
    /**
     * Find active wishlist items for a customer with pagination
     */
    @Query(value = "SELECT * FROM wishlists w WHERE w.customer_id = :customerId AND w.tenant_id = :tenantId " +
           "AND w.is_active = true AND w.deleted_at IS NULL ORDER BY w.created_at DESC", nativeQuery = true)
    Page<Wishlist> findActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                    @Param("tenantId") Long tenantId, 
                                                    Pageable pageable);
    
    /**
     * Find wishlist item by customer, product and tenant
     */
    Optional<Wishlist> findByCustomerIdAndProductIdAndTenantIdAndIsActiveTrueAndDeletedAtIsNull(Long customerId,
                                                              Long productId,
                                                              Long tenantId);
    
    /**
     * Check if a product is in customer's wishlist
     */
    boolean existsByCustomerIdAndProductIdAndTenantIdAndIsActiveTrueAndDeletedAtIsNull(Long customerId,
                                                      Long productId,
                                                      Long tenantId);
    
    /**
     * Count active wishlist items for a customer
     */
    Long countByCustomerIdAndTenantIdAndIsActiveTrueAndDeletedAtIsNull(Long customerId, 
                                           Long tenantId);
    
    /**
     * Find wishlist items by priority for a customer
     */
    @Query(value = "SELECT * FROM wishlists w WHERE w.customer_id = :customerId AND w.tenant_id = :tenantId " +
           "AND w.priority = :priority AND w.is_active = true AND w.deleted_at IS NULL " +
           "ORDER BY w.created_at DESC", nativeQuery = true)
    List<Wishlist> findByCustomerIdAndTenantIdAndPriority(@Param("customerId") Long customerId,
                                                         @Param("tenantId") Long tenantId,
                                                         @Param("priority") Integer priority);
    
    /**
     * Find wishlist items with price alerts enabled
     */
    @Query(value = "SELECT * FROM wishlists w WHERE w.customer_id = :customerId AND w.tenant_id = :tenantId " +
           "AND w.price_alert_enabled = true AND w.is_active = true AND w.deleted_at IS NULL " +
           "ORDER BY w.created_at DESC", nativeQuery = true)
    List<Wishlist> findWithPriceAlertsEnabledByCustomerIdAndTenantId(@Param("customerId") Long customerId,
                                                                   @Param("tenantId") Long tenantId);
    
    /**
     * Find wishlist items with stock alerts enabled
     */
    @Query(value = "SELECT * FROM wishlists w WHERE w.customer_id = :customerId AND w.tenant_id = :tenantId " +
           "AND w.stock_alert_enabled = true AND w.is_active = true AND w.deleted_at IS NULL " +
           "ORDER BY w.created_at DESC", nativeQuery = true)
    List<Wishlist> findWithStockAlertsEnabledByCustomerIdAndTenantId(@Param("customerId") Long customerId,
                                                                   @Param("tenantId") Long tenantId);
    
    /**
     * Find all wishlist items for a specific product across all customers in a tenant
     * (useful for analytics and notifications)
     */
    @Query(value = "SELECT * FROM wishlists w WHERE w.product_id = :productId AND w.tenant_id = :tenantId " +
           "AND w.is_active = true AND w.deleted_at IS NULL ORDER BY w.created_at DESC", nativeQuery = true)
    List<Wishlist> findByProductIdAndTenantId(@Param("productId") Long productId,
                                             @Param("tenantId") Long tenantId);
    
    /**
     * Find most popular products in wishlists for a tenant
     */
    @Query(value = "SELECT w.product_id, COUNT(w) as wishlistCount FROM wishlists w " +
           "WHERE w.tenant_id = :tenantId AND w.is_active = true AND w.deleted_at IS NULL " +
           "GROUP BY w.product_id ORDER BY wishlistCount DESC", nativeQuery = true)
    List<Object[]> findMostWishlistedProductsByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Delete all wishlist items for a customer (for GDPR compliance)
     */
    @Query(value = "UPDATE wishlists SET deleted_at = CURRENT_TIMESTAMP, is_active = false " +
           "WHERE customer_id = :customerId AND tenant_id = :tenantId", nativeQuery = true)
    void softDeleteAllByCustomerIdAndTenantId(@Param("customerId") Long customerId,
                                             @Param("tenantId") Long tenantId);
}
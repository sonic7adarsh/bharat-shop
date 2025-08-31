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
import java.util.UUID;

/**
 * Repository interface for Wishlist entity operations.
 * Provides methods for managing customer wishlists with tenant isolation.
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    
    /**
     * Find all active wishlist items for a customer in a specific tenant
     */
    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.tenantId = :tenantId " +
           "AND w.isActive = true AND w.deletedAt IS NULL ORDER BY w.createdAt DESC")
    List<Wishlist> findActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                    @Param("tenantId") UUID tenantId);
    
    /**
     * Find active wishlist items for a customer with pagination
     */
    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.tenantId = :tenantId " +
           "AND w.isActive = true AND w.deletedAt IS NULL ORDER BY w.createdAt DESC")
    Page<Wishlist> findActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                                    @Param("tenantId") UUID tenantId, 
                                                    Pageable pageable);
    
    /**
     * Find wishlist item by customer, product and tenant
     */
    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.productId = :productId " +
           "AND w.tenantId = :tenantId AND w.isActive = true AND w.deletedAt IS NULL")
    Optional<Wishlist> findByCustomerIdAndProductIdAndTenantId(@Param("customerId") Long customerId,
                                                              @Param("productId") Long productId,
                                                              @Param("tenantId") UUID tenantId);
    
    /**
     * Check if a product is in customer's wishlist
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Wishlist w " +
           "WHERE w.customerId = :customerId AND w.productId = :productId " +
           "AND w.tenantId = :tenantId AND w.isActive = true AND w.deletedAt IS NULL")
    boolean existsByCustomerIdAndProductIdAndTenantId(@Param("customerId") Long customerId,
                                                      @Param("productId") Long productId,
                                                      @Param("tenantId") UUID tenantId);
    
    /**
     * Count active wishlist items for a customer
     */
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.customerId = :customerId AND w.tenantId = :tenantId " +
           "AND w.isActive = true AND w.deletedAt IS NULL")
    Long countActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                           @Param("tenantId") UUID tenantId);
    
    /**
     * Find wishlist items by priority for a customer
     */
    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.tenantId = :tenantId " +
           "AND w.priority = :priority AND w.isActive = true AND w.deletedAt IS NULL " +
           "ORDER BY w.createdAt DESC")
    List<Wishlist> findByCustomerIdAndTenantIdAndPriority(@Param("customerId") Long customerId,
                                                         @Param("tenantId") UUID tenantId,
                                                         @Param("priority") Integer priority);
    
    /**
     * Find wishlist items with price alerts enabled
     */
    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.tenantId = :tenantId " +
           "AND w.priceAlertEnabled = true AND w.isActive = true AND w.deletedAt IS NULL " +
           "ORDER BY w.createdAt DESC")
    List<Wishlist> findWithPriceAlertsEnabledByCustomerIdAndTenantId(@Param("customerId") Long customerId,
                                                                   @Param("tenantId") UUID tenantId);
    
    /**
     * Find wishlist items with stock alerts enabled
     */
    @Query("SELECT w FROM Wishlist w WHERE w.customerId = :customerId AND w.tenantId = :tenantId " +
           "AND w.stockAlertEnabled = true AND w.isActive = true AND w.deletedAt IS NULL " +
           "ORDER BY w.createdAt DESC")
    List<Wishlist> findWithStockAlertsEnabledByCustomerIdAndTenantId(@Param("customerId") Long customerId,
                                                                   @Param("tenantId") UUID tenantId);
    
    /**
     * Find all wishlist items for a specific product across all customers in a tenant
     * (useful for analytics and notifications)
     */
    @Query("SELECT w FROM Wishlist w WHERE w.productId = :productId AND w.tenantId = :tenantId " +
           "AND w.isActive = true AND w.deletedAt IS NULL ORDER BY w.createdAt DESC")
    List<Wishlist> findByProductIdAndTenantId(@Param("productId") Long productId,
                                             @Param("tenantId") UUID tenantId);
    
    /**
     * Find most popular products in wishlists for a tenant
     */
    @Query("SELECT w.productId, COUNT(w) as wishlistCount FROM Wishlist w " +
           "WHERE w.tenantId = :tenantId AND w.isActive = true AND w.deletedAt IS NULL " +
           "GROUP BY w.productId ORDER BY wishlistCount DESC")
    List<Object[]> findMostWishlistedProductsByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Delete all wishlist items for a customer (for GDPR compliance)
     */
    @Query("UPDATE Wishlist w SET w.deletedAt = CURRENT_TIMESTAMP, w.isActive = false " +
           "WHERE w.customerId = :customerId AND w.tenantId = :tenantId")
    void softDeleteAllByCustomerIdAndTenantId(@Param("customerId") Long customerId,
                                             @Param("tenantId") UUID tenantId);
}
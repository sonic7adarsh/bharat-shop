package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("storefrontCartRepository")
public interface StorefrontCartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * Find cart by customer ID and tenant ID
     */
    Optional<Cart> findByCustomerIdAndTenantId(Long customerId, Long tenantId);
    
    /**
     * Find cart with items by customer ID and tenant ID
     * Note: JPA method names don't support JOIN FETCH, this will be handled in service layer
     */
    // This method will be implemented in the service layer using findByCustomerIdAndTenantId
    
    /**
     * Find all carts for a tenant
     */
    List<Cart> findByTenantId(Long tenantId);
    
    /**
     * Find carts by tenant ID with pagination
     */
    List<Cart> findByTenantIdOrderByUpdatedAtDesc(Long tenantId);
    
    /**
     * Find abandoned carts (not updated for specified days)
     * Note: SIZE function requires service layer implementation
     */
    List<Cart> findByTenantIdAndUpdatedAtBefore(Long tenantId, LocalDateTime cutoffDate);
    
    /**
     * Count total items in cart
     * Note: This requires service layer implementation for SUM operations
     */
    // This method will be implemented in the service layer using findByCustomerIdAndTenantId
    
    /**
     * Check if cart exists and is not empty
     * Note: SIZE function requires service layer implementation
     */
    // This method will be implemented in the service layer using findByCustomerIdAndTenantId
    
    /**
     * Delete empty carts for a tenant
     * Note: SIZE function requires service layer implementation
     */
    // This method will be implemented in the service layer
    
    /**
     * Find carts created within date range
     */
    List<Cart> findByTenantIdAndUpdatedAtBetweenOrderByUpdatedAtDesc(Long tenantId,
                                                                     LocalDateTime startDate,
                                                                     LocalDateTime endDate);
    
    /**
     * Get cart statistics for a tenant
     * Note: This complex aggregation requires service layer implementation
     */
    // This method will be implemented in the service layer using findByTenantId
}
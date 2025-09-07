package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository("storefrontCartItemRepository")
public interface StorefrontCartItemRepository extends JpaRepository<CartItem, Long> {
    
    /**
     * Find cart item by cart ID and product ID
     */
    Optional<CartItem> findByCart_IdAndProduct_Id(Long cartId, Long productId);
    
    /**
     * Find all cart items for a specific cart
     */
    List<CartItem> findByCart_IdOrderByCreatedAtAsc(Long cartId);
    
    /**
     * Find cart items by customer and tenant (through cart relationship)
     */
    List<CartItem> findByCart_CustomerIdAndCart_TenantIdOrderByCreatedAtAsc(Long customerId, Long tenantId);
    
    /**
     * Count items in a specific cart
     */
    Long countByCart_Id(Long cartId);
    
    /**
     * Sum total quantity in a cart
     */
    // Note: This requires a custom implementation or service layer logic
    // JPA doesn't directly support SUM operations in method names
    default Integer sumQuantityByCartId(Long cartId) {
        return findByCart_IdOrderByCreatedAtAsc(cartId).stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    
    /**
     * Delete cart item by cart ID and product ID
     */
    @Modifying
    @Transactional
    void deleteByCart_IdAndProduct_Id(Long cartId, Long productId);
    
    /**
     * Delete all cart items for a specific cart
     */
    @Modifying
    @Transactional
    void deleteByCart_Id(Long cartId);
    
    /**
     * Find cart items by product ID (useful for product updates)
     */
    List<CartItem> findByProduct_IdAndCart_TenantId(Long productId, Long tenantId);
    
    /**
     * Update cart item quantity
     * Note: This requires service layer implementation as JPA method names don't support updates
     */
    // This method will be implemented in the service layer
    
    /**
     * Check if cart item exists
     */
    boolean existsByCart_IdAndProduct_Id(Long cartId, Long productId);
}
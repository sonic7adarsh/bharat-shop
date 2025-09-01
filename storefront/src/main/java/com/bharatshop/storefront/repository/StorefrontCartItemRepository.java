package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("storefrontCartItemRepository")
public interface StorefrontCartItemRepository extends JpaRepository<CartItem, Long> {
    
    /**
     * Find cart item by cart ID and product ID
     */
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId, 
                                               @Param("productId") Long productId);
    
    /**
     * Find all cart items for a specific cart
     */
    @Query("SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.product " +
           "WHERE ci.cart.id = :cartId ORDER BY ci.createdAt ASC")
    List<CartItem> findByCartIdWithProduct(@Param("cartId") Long cartId);
    
    /**
     * Find cart items by customer and tenant (through cart relationship)
     */
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN ci.cart c " +
           "LEFT JOIN FETCH ci.product " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId " +
           "ORDER BY ci.createdAt ASC")
    List<CartItem> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                              @Param("tenantId") Long tenantId);
    
    /**
     * Count items in a specific cart
     */
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long countByCartId(@Param("cartId") Long cartId);
    
    /**
     * Sum total quantity in a cart
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") Long cartId);
    
    /**
     * Delete cart item by cart ID and product ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    /**
     * Delete all cart items for a specific cart
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
    
    /**
     * Find cart items by product ID (useful for product updates)
     */
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN ci.cart c " +
           "WHERE ci.product.id = :productId AND c.tenantId = :tenantId")
    List<CartItem> findByProductIdAndTenantId(@Param("productId") Long productId, 
                                             @Param("tenantId") Long tenantId);
    
    /**
     * Update cart item quantity
     */
    @Modifying
    @Query("UPDATE CartItem ci SET ci.quantity = :quantity, ci.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    int updateQuantityByCartIdAndProductId(@Param("cartId") Long cartId, 
                                          @Param("productId") Long productId, 
                                          @Param("quantity") Integer quantity);
    
    /**
     * Check if cart item exists
     */
    @Query("SELECT CASE WHEN COUNT(ci) > 0 THEN true ELSE false END FROM CartItem ci " +
           "WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    boolean existsByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}
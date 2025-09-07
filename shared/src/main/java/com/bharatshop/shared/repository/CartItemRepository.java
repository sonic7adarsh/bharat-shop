package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    /**
     * Find cart item by cart ID and product ID
     */
    @Query(value = "SELECT * FROM cart_items " +
           "WHERE cart_id = :cartId AND product_id = :productId", nativeQuery = true)
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId, 
                                               @Param("productId") Long productId);
    
    /**
     * Find all cart items for a specific cart
     */
    @Query(value = "SELECT ci.* FROM cart_items ci " +
           "LEFT JOIN products p ON ci.product_id = p.id " +
           "WHERE ci.cart_id = :cartId ORDER BY ci.created_at ASC", nativeQuery = true)
    List<CartItem> findByCartIdWithProduct(@Param("cartId") Long cartId);
    
    /**
     * Find cart items by customer and tenant (through cart relationship)
     */
    @Query(value = "SELECT ci.* FROM cart_items ci " +
           "JOIN carts c ON ci.cart_id = c.id " +
           "LEFT JOIN products p ON ci.product_id = p.id " +
           "WHERE c.customer_id = :customerId AND c.tenant_id = :tenantId " +
           "ORDER BY ci.created_at ASC", nativeQuery = true)
    List<CartItem> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, 
                                              @Param("tenantId") Long tenantId);
    
    /**
     * Count items in a specific cart
     */
    @Query(value = "SELECT COUNT(*) FROM cart_items WHERE cart_id = :cartId", nativeQuery = true)
    Long countByCartId(@Param("cartId") Long cartId);
    
    /**
     * Sum total quantity in a cart
     */
    @Query(value = "SELECT SUM(quantity) FROM cart_items WHERE cart_id = :cartId", nativeQuery = true)
    Long sumQuantityByCartId(@Param("cartId") Long cartId);
    
    /**
     * Delete cart item by cart ID and product ID
     */
    @Modifying
    @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId AND product_id = :productId", nativeQuery = true)
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    /**
     * Delete all cart items for a specific cart
     */
    @Modifying
    @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId", nativeQuery = true)
    void deleteByCartId(@Param("cartId") Long cartId);
    
    /**
     * Find cart items by product ID (useful for product updates)
     */
    @Query(value = "SELECT ci.* FROM cart_items ci " +
           "JOIN carts c ON ci.cart_id = c.id " +
           "WHERE ci.product_id = :productId AND c.tenant_id = :tenantId", nativeQuery = true)
    List<CartItem> findByProductIdAndTenantId(@Param("productId") Long productId,
                                             @Param("tenantId") Long tenantId);
    
    /**
     * Update cart item quantity
     */
    @Modifying
    @Query(value = "UPDATE cart_items SET quantity = :quantity, updated_at = CURRENT_TIMESTAMP " +
           "WHERE cart_id = :cartId AND product_id = :productId", nativeQuery = true)
    int updateQuantityByCartIdAndProductId(@Param("cartId") Long cartId, 
                                          @Param("productId") Long productId, 
                                          @Param("quantity") Integer quantity);
    
    /**
     * Check if cart item exists
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM cart_items " +
           "WHERE cart_id = :cartId AND product_id = :productId", nativeQuery = true)
    boolean existsByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}
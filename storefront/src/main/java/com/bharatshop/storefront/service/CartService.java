package com.bharatshop.storefront.service;

import com.bharatshop.storefront.repository.StorefrontProductRepository;
import com.bharatshop.shared.entity.Cart;
import com.bharatshop.shared.entity.CartItem;
import com.bharatshop.storefront.repository.StorefrontCartItemRepository;
import com.bharatshop.storefront.repository.StorefrontCartRepository;
import com.bharatshop.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bharatshop.shared.entity.Product.ProductStatus;
import com.bharatshop.shared.entity.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    
    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    
    @Qualifier("storefrontCartRepository")
    private final StorefrontCartRepository cartRepository;
    @Qualifier("storefrontCartItemRepository")
    private final StorefrontCartItemRepository cartItemRepository;
    @Qualifier("storefrontProductRepository")
    private final StorefrontProductRepository storefrontProductRepository;
    private final ProductRepository sharedProductRepository;
    
    /**
     * Get or create cart for customer
     */
    @Cacheable(value = "customerCart", key = "#customerId + '_' + #tenantId")
    public Cart getOrCreateCart(Long customerId, Long tenantId) {
        return cartRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .orElseGet(() -> createNewCart(customerId, tenantId));
    }
    
    /**
     * Add item to cart
     */
    @CacheEvict(value = "customerCart", key = "#customerId + '_' + #tenantId")
    public Cart addItemToCart(Long customerId, Long tenantId, Long productId, Integer quantity) {
        // Validate product
       Product product = storefrontProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        if (product.getStatus() != ProductStatus.ACTIVE || product.getStock() < quantity) {
            throw new RuntimeException("Product is not available or insufficient stock");
        }
        
        // Get or create cart
        Cart cart = getOrCreateCartForUpdate(customerId, tenantId);
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId);
        
        if (existingItem.isPresent()) {
            // Update existing item quantity
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            // Validate stock availability
            if (product.getStock() < newQuantity) {
                throw new RuntimeException("Insufficient stock. Available: " + product.getStock());
            }
            
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        } else {
            // Create new cart item
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .build();
            
            cartItemRepository.save(cartItem);
            
            // Add to cart's items list if not already present
            if (cart.getItems() == null) {
                cart.setItems(new ArrayList<>());
            }
            cart.getItems().add(cartItem);
        }
        
        return cartRepository.save(cart);
    }
    
    /**
     * Update cart item quantity
     */
    @CacheEvict(value = "customerCart", key = "#customerId + '_' + #tenantId")
    public Cart updateCartItemQuantity(Long customerId, Long tenantId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCartForUpdate(customerId, tenantId);
        
        CartItem cartItem = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            return removeItemFromCart(customerId, tenantId, productId);
        }
        
        // Validate stock availability
        Product product = storefrontProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStock());
        }
        
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        
        return cartRepository.save(cart);
    }
    
    /**
     * Remove item from cart
     */
    @CacheEvict(value = "customerCart", key = "#customerId + '_' + #tenantId")
    public Cart removeItemFromCart(Long customerId, Long tenantId, Long productId) {
        Cart cart = getOrCreateCartForUpdate(customerId, tenantId);
        
        CartItem cartItem = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        // Remove from cart's items list
        if (cart.getItems() != null) {
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        }
        
        cartItemRepository.delete(cartItem);
        
        return cartRepository.save(cart);
    }
    
    /**
     * Clear entire cart
     */
    @CacheEvict(value = "customerCart", key = "#customerId + '_' + #tenantId")
    public void clearCart(Long customerId, Long tenantId) {
        Cart cart = cartRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .orElse(null);
        
        if (cart != null) {
            cartItemRepository.deleteByCart_Id(cart.getId());
            cart.clearItems();
            cartRepository.save(cart);
        }
    }
    
    /**
     * Get cart total amount
     */
    public BigDecimal getCartTotal(Long customerId, Long tenantId) {
        Cart cart = getOrCreateCart(customerId, tenantId);
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Get cart item count
     */
    public Integer getCartItemCount(Long customerId, Long tenantId) {
        Cart cart = cartRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .orElse(null);
        return cart != null ? cart.getItems().size() : 0;
    }
    
    /**
     * Validate cart before checkout
     */
    public void validateCartForCheckout(Long customerId, Long tenantId) {
        Cart cart = getOrCreateCart(customerId, tenantId);
        
        if (cart.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // Validate each item
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new RuntimeException("Product '" + product.getName() + "' is no longer available");
            }
            
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for '" + product.getName() + 
                        "'. Available: " + product.getStock() + ", Requested: " + item.getQuantity());
            }
            
            // Check if price has changed significantly (more than 10%)
            BigDecimal currentPrice = product.getPrice();
            BigDecimal cartPrice = item.getUnitPrice();
            BigDecimal priceDifference = currentPrice.subtract(cartPrice).abs();
            BigDecimal priceChangePercentage = priceDifference.divide(cartPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            if (priceChangePercentage.compareTo(BigDecimal.valueOf(10)) > 0) {
                log.warn("Price change detected for product {}: cart price {}, current price {}", 
                        product.getName(), cartPrice, currentPrice);
                // Update the cart item with current price
                item.setUnitPrice(currentPrice);
                cartItemRepository.save(item);
            }
        }
    }
    
    /**
     * Check if cart exists and is not empty
     */
    public boolean hasNonEmptyCart(Long customerId, Long tenantId) {
        Cart cart = cartRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .orElse(null);
        return cart != null && !cart.getItems().isEmpty();
    }
    
    // Private helper methods
    
    private Cart createNewCart(Long customerId, Long tenantId) {
        Cart cart = Cart.builder()
                .customerId(customerId)
                .tenantId(tenantId)
                .items(new ArrayList<>())
                .build();
        
        return cartRepository.save(cart);
    }
    
    private Cart getOrCreateCartForUpdate(Long customerId, Long tenantId) {
        return cartRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .orElseGet(() -> createNewCart(customerId, tenantId));
    }
}
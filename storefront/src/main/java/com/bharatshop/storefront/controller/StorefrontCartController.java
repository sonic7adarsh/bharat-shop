package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.shared.ApiResponse;
import com.bharatshop.storefront.dto.AddToCartRequest;
import com.bharatshop.storefront.dto.CartResponse;
import com.bharatshop.storefront.dto.UpdateCartRequest;
import com.bharatshop.shared.entity.Cart;
import com.bharatshop.storefront.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/store/cart")
@RequiredArgsConstructor
@Slf4j
public class StorefrontCartController {
    
    private final CartService cartService;
    
    /**
     * Add item to cart
     * POST /store/cart/add
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract customer and tenant info from request headers or JWT
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Cart cart = cartService.addItemToCart(
                    customerId, 
                    tenantId, 
                    request.getProductId(), 
                    request.getQuantity()
            );
            
            CartResponse response = CartResponse.fromEntity(cart);
            
            log.info("Item added to cart successfully for customer: {}, product: {}, quantity: {}", 
                    customerId, request.getProductId(), request.getQuantity());
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Item added to cart successfully")
            );
            
        } catch (Exception e) {
            log.error("Error adding item to cart: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get customer's cart
     * GET /store/cart
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Cart cart = cartService.getOrCreateCart(customerId, tenantId);
            CartResponse response = CartResponse.fromEntity(cart);
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Cart retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving cart: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Update cart item quantity
     * PUT /store/cart/update
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @Valid @RequestBody UpdateCartRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Cart cart = cartService.updateCartItemQuantity(
                    customerId, 
                    tenantId, 
                    request.getProductId(), 
                    request.getQuantity()
            );
            
            CartResponse response = CartResponse.fromEntity(cart);
            
            log.info("Cart item updated successfully for customer: {}, product: {}, quantity: {}", 
                    customerId, request.getProductId(), request.getQuantity());
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Cart item updated successfully")
            );
            
        } catch (Exception e) {
            log.error("Error updating cart item: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Remove item from cart
     * DELETE /store/cart/remove
     */
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @RequestParam Long productId,
            HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Cart cart = cartService.removeItemFromCart(customerId, tenantId, productId);
            CartResponse response = CartResponse.fromEntity(cart);
            
            log.info("Item removed from cart successfully for customer: {}, product: {}", 
                    customerId, productId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Item removed from cart successfully")
            );
            
        } catch (Exception e) {
            log.error("Error removing item from cart: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Clear entire cart
     * DELETE /store/cart/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCart(HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            cartService.clearCart(customerId, tenantId);
            
            log.info("Cart cleared successfully for customer: {}", customerId);
            
            return ResponseEntity.ok(
                    ApiResponse.success("Cart cleared successfully", "Cart cleared successfully")
            );
            
        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get cart total
     * GET /store/cart/total
     */
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getCartTotal(HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            BigDecimal total = cartService.getCartTotal(customerId, tenantId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(total, "Cart total retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving cart total: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    /**
     * Get cart item count
     * GET /store/cart/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(HttpServletRequest httpRequest) {
        
        try {
            Long customerId = extractCustomerId(httpRequest);
            Long tenantId = extractTenantId(httpRequest);
            
            Integer count = cartService.getCartItemCount(customerId, tenantId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(count, "Cart item count retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving cart item count: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );
        }
    }
    
    // Helper methods to extract customer and tenant information
    // These would typically extract from JWT token or session
    
    private Long extractCustomerId(HttpServletRequest request) {
        // TODO: Extract from JWT token or session
        // For now, using header for testing
        String customerIdHeader = request.getHeader("X-Customer-Id");
        if (customerIdHeader != null) {
            return Long.parseLong(customerIdHeader);
        }
        throw new RuntimeException("Customer ID not found in request");
    }
    
    private Long extractTenantId(HttpServletRequest request) {
        // TODO: Extract from JWT token or session
        // For now, using header for testing
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        if (tenantIdHeader != null) {
            return Long.parseLong(tenantIdHeader);
        }
        throw new RuntimeException("Tenant ID not found in request");
    }
}
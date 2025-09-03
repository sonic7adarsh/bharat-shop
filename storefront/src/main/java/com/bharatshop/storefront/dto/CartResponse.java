package com.bharatshop.storefront.dto;

import com.bharatshop.storefront.entity.Cart;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CartResponse {
    
    private Long id;
    private Long customerId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private boolean isEmpty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public CartResponse() {}
    
    public CartResponse(Long id, Long customerId, List<CartItemResponse> items, Integer totalItems, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal shippingAmount, BigDecimal totalAmount, boolean isEmpty, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.totalItems = totalItems;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.shippingAmount = shippingAmount;
        this.totalAmount = totalAmount;
        this.isEmpty = isEmpty;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public static CartResponseBuilder builder() {
        return new CartResponseBuilder();
    }
    
    // Getters
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public List<CartItemResponse> getItems() { return items; }
    public Integer getTotalItems() { return totalItems; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public boolean isEmpty() { return isEmpty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Setters
    public void setId(Long id) { this.id = id; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setEmpty(boolean isEmpty) { this.isEmpty = isEmpty; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public static class CartResponseBuilder {
        private Long id;
        private Long customerId;
        private List<CartItemResponse> items;
        private Integer totalItems;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal shippingAmount;
        private BigDecimal totalAmount;
        private boolean isEmpty;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public CartResponseBuilder id(Long id) { this.id = id; return this; }
        public CartResponseBuilder customerId(Long customerId) { this.customerId = customerId; return this; }
        public CartResponseBuilder items(List<CartItemResponse> items) { this.items = items; return this; }
        public CartResponseBuilder totalItems(Integer totalItems) { this.totalItems = totalItems; return this; }
        public CartResponseBuilder subtotal(BigDecimal subtotal) { this.subtotal = subtotal; return this; }
        public CartResponseBuilder taxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; return this; }
        public CartResponseBuilder shippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; return this; }
        public CartResponseBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public CartResponseBuilder isEmpty(boolean isEmpty) { this.isEmpty = isEmpty; return this; }
        public CartResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CartResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public CartResponse build() {
            return new CartResponse(id, customerId, items, totalItems, subtotal, taxAmount, shippingAmount, totalAmount, isEmpty, createdAt, updatedAt);
        }
    }
    
    public static CartResponse fromEntity(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems() != null ? 
                cart.getItems().stream()
                        .map(CartItemResponse::fromEntity)
                        .collect(Collectors.toList()) : 
                List.of();
        
        BigDecimal subtotal = calculateSubtotal(itemResponses);
        BigDecimal taxAmount = calculateTax(subtotal);
        BigDecimal shippingAmount = calculateShipping(subtotal);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount);
        
        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .items(itemResponses)
                .totalItems(cart.getTotalItems())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .totalAmount(totalAmount)
                .isEmpty(cart.isEmpty())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
    
    private static BigDecimal calculateSubtotal(List<CartItemResponse> items) {
        return items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private static BigDecimal calculateTax(BigDecimal subtotal) {
        // 18% GST
        BigDecimal taxRate = BigDecimal.valueOf(0.18);
        return subtotal.multiply(taxRate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    private static BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping for orders above 500
        if (subtotal.compareTo(BigDecimal.valueOf(500)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(50); // Flat shipping rate
    }
}
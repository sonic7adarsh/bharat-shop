package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.CartItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CartItemResponse {
    
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer availableStock;
    private LocalDateTime addedAt;
    
    public CartItemResponse() {}
    
    public CartItemResponse(Long id, Long productId, String productName, String productSku, String productImageUrl, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice, Integer availableStock, LocalDateTime addedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.productImageUrl = productImageUrl;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.availableStock = availableStock;
        this.addedAt = addedAt;
    }
    
    public static CartItemResponseBuilder builder() {
        return new CartItemResponseBuilder();
    }
    
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public String getProductImageUrl() { return productImageUrl; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public Integer getAvailableStock() { return availableStock; }
    public LocalDateTime getAddedAt() { return addedAt; }
    
    public void setId(Long id) { this.id = id; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    
    public static class CartItemResponseBuilder {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private Integer availableStock;
        private LocalDateTime addedAt;
        
        public CartItemResponseBuilder id(Long id) { this.id = id; return this; }
        public CartItemResponseBuilder productId(Long productId) { this.productId = productId; return this; }
        public CartItemResponseBuilder productName(String productName) { this.productName = productName; return this; }
        public CartItemResponseBuilder productSku(String productSku) { this.productSku = productSku; return this; }
        public CartItemResponseBuilder productImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; return this; }
        public CartItemResponseBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public CartItemResponseBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public CartItemResponseBuilder totalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; return this; }
        public CartItemResponseBuilder availableStock(Integer availableStock) { this.availableStock = availableStock; return this; }
        public CartItemResponseBuilder addedAt(LocalDateTime addedAt) { this.addedAt = addedAt; return this; }
        
        public CartItemResponse build() {
            return new CartItemResponse(id, productId, productName, productSku, productImageUrl, quantity, unitPrice, totalPrice, availableStock, addedAt);
        }
    }
    
    public static CartItemResponse fromEntity(CartItem cartItem) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .productName(cartItem.getProduct().getName())
                .productSku(cartItem.getProduct().getSlug())
                .productImageUrl(cartItem.getProduct().getDescription())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getTotalPrice())
                .availableStock(cartItem.getProduct().getStock())
                .addedAt(cartItem.getCreatedAt())
                .build();
    }
}
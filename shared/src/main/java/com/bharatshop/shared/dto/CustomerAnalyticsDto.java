package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for customer analytics and dashboard data.
 * Contains order history, statistics, and wishlist information for customer dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsDto {
    
    // Order Statistics
    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private Long cancelledOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    
    // Recent Order History
    private List<OrderHistoryData> recentOrders;
    
    // Order Status Breakdown
    private Long draftOrders;
    private Long confirmedOrders;
    private Long packedOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long returnedOrders;
    
    // Customer Behavior Metrics
    private LocalDateTime lastOrderDate;
    private LocalDateTime firstOrderDate;
    private Integer daysSinceLastOrder;
    private Integer totalOrderDays;
    
    // Wishlist Data (optional future feature)
    private Long totalWishlistItems;
    private List<WishlistItemData> recentWishlistItems;
    
    // Favorite Products
    private List<FavoriteProductData> favoriteProducts;
    
    /**
     * Nested class for order history data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderHistoryData {
        private Long orderId;
        private String orderNumber;
        private String status;
        private String paymentStatus;
        private BigDecimal totalAmount;
        private LocalDateTime orderDate;
        private LocalDateTime deliveredDate;
        private Integer totalItems;
        private String trackingNumber;
        private String courierPartner;
        private List<OrderItemData> items;
    }
    
    /**
     * Nested class for order item data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
    }
    
    /**
     * Nested class for wishlist item data (future feature)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistItemData {
        private Long wishlistId;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private BigDecimal currentPrice;
        private BigDecimal originalPrice;
        private Boolean inStock;
        private LocalDateTime addedDate;
    }
    
    /**
     * Nested class for favorite product data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteProductData {
        private Long productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private Integer totalPurchases;
        private BigDecimal totalSpent;
        private LocalDateTime lastPurchaseDate;
        private BigDecimal averagePrice;
    }
}
package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for Vendor Analytics Dashboard.
 * Contains comprehensive analytics data for vendor dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorAnalyticsDto {
    
    // Basic metrics
    private Long totalProducts;
    private Long totalActiveProducts;
    private Long totalInactiveProducts;
    
    // Order statistics
    private Map<String, Long> ordersByStatus;
    private Long totalOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    
    // Revenue data
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal previousMonthRevenue;
    private BigDecimal revenueGrowthPercentage;
    private List<MonthlyRevenueData> monthlyRevenueHistory;
    
    // Top products
    private List<TopProductData> topProductsByRevenue;
    private List<TopProductData> topProductsByQuantity;
    
    // Additional metrics
    private Double averageOrderValue;
    private Long totalCustomers;
    private Long repeatCustomers;
    private Double customerRetentionRate;
    
    // Time period for the analytics
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private LocalDateTime generatedAt;
    
    /**
     * Monthly revenue data for charts and trends.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueData {
        private String month; // Format: "2024-01"
        private String monthName; // Format: "January 2024"
        private BigDecimal revenue;
        private Long orderCount;
        private Double averageOrderValue;
    }
    
    /**
     * Top product data for analytics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductData {
        private UUID productId;
        private String productName;
        private String productSku;
        private BigDecimal revenue;
        private Long quantitySold;
        private Long orderCount;
        private BigDecimal averagePrice;
        private String imageUrl;
    }
}
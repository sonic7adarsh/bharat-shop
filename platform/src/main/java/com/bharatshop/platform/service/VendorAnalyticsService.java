package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.VendorAnalyticsDto;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.repository.ProductRepository;
import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.repository.OrderRepository;
import com.bharatshop.shared.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating vendor analytics and dashboard metrics.
 * Provides comprehensive analytics data for vendor dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VendorAnalyticsService {
    
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    
    /**
     * Get comprehensive analytics data for vendor dashboard.
     */
    public VendorAnalyticsDto getVendorAnalytics(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Generating analytics for tenant: {} from {} to {}", tenantId, fromDate, toDate);
        
        return VendorAnalyticsDto.builder()
                .totalProducts(getTotalProducts(tenantId))
                .totalActiveProducts(getTotalActiveProducts(tenantId))
                .totalInactiveProducts(getTotalInactiveProducts(tenantId))
                .ordersByStatus(getOrdersByStatus(tenantId, fromDate, toDate))
                .totalOrders(getTotalOrders(tenantId, fromDate, toDate))
                .pendingOrders(getOrdersByStatus(tenantId, Orders.OrderStatus.PENDING_PAYMENT, fromDate, toDate))
                .processingOrders(getOrdersByStatus(tenantId, Orders.OrderStatus.CONFIRMED, fromDate, toDate))
                .shippedOrders(getOrdersByStatus(tenantId, Orders.OrderStatus.SHIPPED, fromDate, toDate))
                .deliveredOrders(getOrdersByStatus(tenantId, Orders.OrderStatus.DELIVERED, fromDate, toDate))
                .cancelledOrders(getOrdersByStatus(tenantId, Orders.OrderStatus.CANCELLED, fromDate, toDate))
                .totalRevenue(getTotalRevenue(tenantId, fromDate, toDate))
                .monthlyRevenue(getMonthlyRevenue(tenantId))
                .previousMonthRevenue(getPreviousMonthRevenue(tenantId))
                .revenueGrowthPercentage(calculateRevenueGrowth(tenantId))
                .monthlyRevenueHistory(getMonthlyRevenueHistory(tenantId, 12))
                .topProductsByRevenue(getTopProductsByRevenue(tenantId, fromDate, toDate, 10))
                .topProductsByQuantity(getTopProductsByQuantity(tenantId, fromDate, toDate, 10))
                .averageOrderValue(getAverageOrderValue(tenantId, fromDate, toDate))
                .totalCustomers(getTotalCustomers(tenantId, fromDate, toDate))
                .repeatCustomers(getRepeatCustomers(tenantId, fromDate, toDate))
                .customerRetentionRate(calculateCustomerRetentionRate(tenantId, fromDate, toDate))
                .fromDate(fromDate)
                .toDate(toDate)
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Get total products count for tenant.
     */
    private Long getTotalProducts(Long tenantId) {
        return productRepository.countByTenantId(tenantId);
    }
    
    /**
     * Get total active products count for tenant.
     */
    private Long getTotalActiveProducts(Long tenantId) {
        return productRepository.countByTenantIdAndStatus(tenantId, Product.ProductStatus.ACTIVE);
    }
    
    /**
     * Get total inactive products count for tenant.
     */
    private Long getTotalInactiveProducts(Long tenantId) {
        Long total = getTotalProducts(tenantId);
        Long active = getTotalActiveProducts(tenantId);
        return total - active;
    }
    
    /**
     * Get orders grouped by status.
     */
    private Map<String, Long> getOrdersByStatus(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (Orders.OrderStatus status : Orders.OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByTenantIdAndStatus(tenantId, status));
        }
        return ordersByStatus;
    }
    
    /**
     * Get total orders count.
     */
    private Long getTotalOrders(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        return orderRepository.countByTenantId(tenantId);
    }
    
    /**
     * Get orders count by specific status.
     */
    private Long getOrdersByStatus(Long tenantId, Orders.OrderStatus status, LocalDateTime fromDate, LocalDateTime toDate) {
        return orderRepository.countByTenantIdAndStatus(tenantId, status);
    }
    
    /**
     * Get total revenue for the period.
     */
    private BigDecimal getTotalRevenue(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        BigDecimal revenue = orderRepository.getRevenueByTenantAndDateRange(tenantId, fromDate, toDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    /**
     * Get current month revenue.
     */
    private BigDecimal getMonthlyRevenue(Long tenantId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        return getTotalRevenue(tenantId, startOfMonth, endOfMonth);
    }
    
    /**
     * Get previous month revenue.
     */
    private BigDecimal getPreviousMonthRevenue(Long tenantId) {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDateTime startOfMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);
        return getTotalRevenue(tenantId, startOfMonth, endOfMonth);
    }
    
    /**
     * Calculate revenue growth percentage.
     */
    private BigDecimal calculateRevenueGrowth(Long tenantId) {
        BigDecimal currentRevenue = getMonthlyRevenue(tenantId);
        BigDecimal previousRevenue = getPreviousMonthRevenue(tenantId);
        
        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100") : BigDecimal.ZERO;
        }
        
        return currentRevenue.subtract(previousRevenue)
                .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get monthly revenue history for the last N months.
     */
    private List<VendorAnalyticsDto.MonthlyRevenueData> getMonthlyRevenueHistory(Long tenantId, int months) {
        List<VendorAnalyticsDto.MonthlyRevenueData> history = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        
        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);
            
            BigDecimal revenue = getTotalRevenue(tenantId, startOfMonth, endOfMonth);
            Long orderCount = getTotalOrders(tenantId, startOfMonth, endOfMonth);
            Double avgOrderValue = orderCount > 0 ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;
            
            history.add(VendorAnalyticsDto.MonthlyRevenueData.builder()
                    .month(month.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .monthName(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .averageOrderValue(avgOrderValue)
                    .build());
        }
        
        return history;
    }
    
    /**
     * Get top products by revenue.
     */
    private List<VendorAnalyticsDto.TopProductData> getTopProductsByRevenue(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate, int limit) {
        List<Object[]> results = orderItemRepository.findTopSellingProductsByRevenue(tenantId);
        return results.stream()
                .limit(limit)
                .map(this::mapToTopProductData)
                .collect(Collectors.toList());
    }
    
    /**
     * Get top products by quantity sold.
     */
    private List<VendorAnalyticsDto.TopProductData> getTopProductsByQuantity(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate, int limit) {
        List<Object[]> results = orderItemRepository.findTopSellingProductsByQuantity(tenantId);
        return results.stream()
                .limit(limit)
                .map(this::mapToTopProductData)
                .collect(Collectors.toList());
    }
    
    /**
     * Map database result to TopProductData.
     */
    private VendorAnalyticsDto.TopProductData mapToTopProductData(Object[] result) {
        return VendorAnalyticsDto.TopProductData.builder()
                .productId((Long) result[0])
                .productName((String) result[1])
                .productSku((String) result[2])
                .revenue((BigDecimal) result[3])
                .quantitySold((Long) result[4])
                .orderCount((Long) result[5])
                .averagePrice((BigDecimal) result[6])
                .imageUrl((String) result[7])
                .build();
    }
    
    /**
     * Get average order value.
     */
    private Double getAverageOrderValue(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        BigDecimal totalRevenue = getTotalRevenue(tenantId, fromDate, toDate);
        Long totalOrders = getTotalOrders(tenantId, fromDate, toDate);
        
        if (totalOrders == 0) {
            return 0.0;
        }
        
        return totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue();
    }
    
    /**
     * Get total unique customers (simplified).
     */
    private Long getTotalCustomers(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        // Simplified - just return total orders for now
        return orderRepository.countByTenantId(tenantId);
    }
    
    /**
     * Get repeat customers count (simplified).
     */
    private Long getRepeatCustomers(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        // Simplified - return 0 for now
        return 0L;
    }
    
    /**
     * Calculate customer retention rate (simplified).
     */
    private Double calculateCustomerRetentionRate(Long tenantId, LocalDateTime fromDate, LocalDateTime toDate) {
        // Simplified - return 0 for now
        return 0.0;
    }
}
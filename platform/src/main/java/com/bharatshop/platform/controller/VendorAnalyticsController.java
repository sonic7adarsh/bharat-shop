package com.bharatshop.platform.controller;

import com.bharatshop.shared.dto.VendorAnalyticsDto;
import com.bharatshop.shared.service.FeatureFlagService;
import com.bharatshop.platform.service.VendorAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for vendor analytics and dashboard operations.
 * Provides analytics data for vendor dashboard including products, orders, and revenue metrics.
 */
@RestController
@RequestMapping("/api/vendors/analytics")
@RequiredArgsConstructor
@Slf4j
public class VendorAnalyticsController {
    
    private final VendorAnalyticsService vendorAnalyticsService;
    private final FeatureFlagService featureFlagService;
    
    /**
     * Get comprehensive vendor analytics for dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getVendorDashboard(Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            // Enforce analytics feature access
            featureFlagService.enforceFeatureAccess(UUID.fromString(tenantId.toString()), "analytics");
            
            LocalDateTime toDate = LocalDateTime.now();
            LocalDateTime fromDate = toDate.minusMonths(12); // Default to last 12 months
            VendorAnalyticsDto analytics = vendorAnalyticsService.getVendorAnalytics(tenantId, fromDate, toDate);
            
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            log.error("Error getting vendor analytics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve analytics data"));
        }
    }
    
    /**
     * Get product statistics only
     */
    @GetMapping("/products")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getProductAnalytics(Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            // Enforce analytics feature access
            featureFlagService.enforceFeatureAccess(UUID.fromString(tenantId.toString()), "analytics");
            
            LocalDateTime toDate = LocalDateTime.now();
            LocalDateTime fromDate = toDate.minusMonths(12); // Default to last 12 months
            VendorAnalyticsDto analytics = vendorAnalyticsService.getVendorAnalytics(tenantId, fromDate, toDate);
            
            // Return only product-related metrics
            Map<String, Object> productStats = Map.of(
                "totalProducts", analytics.getTotalProducts(),
                "activeProducts", analytics.getTotalActiveProducts(),
                "inactiveProducts", analytics.getTotalInactiveProducts()
            );
            
            return ResponseEntity.ok(productStats);
            
        } catch (Exception e) {
            log.error("Error getting product analytics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve product analytics"));
        }
    }
    
    /**
     * Get order statistics only
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getOrderAnalytics(Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            // Enforce analytics feature access
            featureFlagService.enforceFeatureAccess(UUID.fromString(tenantId.toString()), "analytics");
            
            LocalDateTime toDate = LocalDateTime.now();
            LocalDateTime fromDate = toDate.minusMonths(12); // Default to last 12 months
            VendorAnalyticsDto analytics = vendorAnalyticsService.getVendorAnalytics(tenantId, fromDate, toDate);
            
            // Return only order-related metrics
            Map<String, Object> orderStats = Map.of(
                "totalOrders", analytics.getTotalOrders(),
                "pendingOrders", analytics.getPendingOrders(),
                "processingOrders", analytics.getProcessingOrders(),
                "shippedOrders", analytics.getShippedOrders(),
                "deliveredOrders", analytics.getDeliveredOrders(),
                "cancelledOrders", analytics.getCancelledOrders()
            );
            
            return ResponseEntity.ok(orderStats);
            
        } catch (Exception e) {
            log.error("Error getting order analytics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve order analytics"));
        }
    }
    
    /**
     * Get revenue statistics only
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getRevenueAnalytics(Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            // Enforce analytics feature access
            featureFlagService.enforceFeatureAccess(UUID.fromString(tenantId.toString()), "analytics");
            
            LocalDateTime toDate = LocalDateTime.now();
            LocalDateTime fromDate = toDate.minusMonths(12); // Default to last 12 months
            VendorAnalyticsDto analytics = vendorAnalyticsService.getVendorAnalytics(tenantId, fromDate, toDate);
            
            // Return only revenue-related metrics
            Map<String, Object> revenueStats = Map.of(
                "totalRevenue", analytics.getTotalRevenue(),
                "monthlyRevenue", analytics.getMonthlyRevenue(),
                "previousMonthRevenue", analytics.getPreviousMonthRevenue(),
                "revenueGrowthPercentage", analytics.getRevenueGrowthPercentage(),
                "monthlyRevenueHistory", analytics.getMonthlyRevenueHistory(),
                "averageOrderValue", analytics.getAverageOrderValue()
            );
            
            return ResponseEntity.ok(revenueStats);
            
        } catch (Exception e) {
            log.error("Error getting revenue analytics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve revenue analytics"));
        }
    }
    
    /**
     * Get top products statistics only
     */
    @GetMapping("/top-products")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getTopProductsAnalytics(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            // Enforce analytics feature access
            featureFlagService.enforceFeatureAccess(UUID.fromString(tenantId.toString()), "analytics");
            
            LocalDateTime toDate = LocalDateTime.now();
            LocalDateTime fromDate = toDate.minusMonths(12); // Default to last 12 months
            VendorAnalyticsDto analytics = vendorAnalyticsService.getVendorAnalytics(tenantId, fromDate, toDate);
            
            // Return only top products metrics
            Map<String, Object> topProductsStats = Map.of(
                "topProductsByRevenue", analytics.getTopProductsByRevenue(),
                "topProductsByQuantity", analytics.getTopProductsByQuantity()
            );
            
            return ResponseEntity.ok(topProductsStats);
            
        } catch (Exception e) {
            log.error("Error getting top products analytics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve top products analytics"));
        }
    }
    
    /**
     * Get customer statistics only
     */
    @GetMapping("/customers")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getCustomerAnalytics(Authentication authentication) {
        try {
            Long tenantId = getTenantIdFromAuth(authentication);
            
            // Enforce analytics feature access
            featureFlagService.enforceFeatureAccess(UUID.fromString(tenantId.toString()), "analytics");
            
            LocalDateTime toDate = LocalDateTime.now();
            LocalDateTime fromDate = toDate.minusMonths(12); // Default to last 12 months
            VendorAnalyticsDto analytics = vendorAnalyticsService.getVendorAnalytics(tenantId, fromDate, toDate);
            
            // Return only customer-related metrics
            Map<String, Object> customerStats = Map.of(
                "totalCustomers", analytics.getTotalCustomers(),
                "repeatCustomers", analytics.getRepeatCustomers(),
                "customerRetentionRate", analytics.getCustomerRetentionRate()
            );
            
            return ResponseEntity.ok(customerStats);
            
        } catch (Exception e) {
            log.error("Error getting customer analytics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to retrieve customer analytics"));
        }
    }
    
    /**
     * Extract tenant ID from authentication context
     * This should be implemented based on your JWT structure
     */
    private Long getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return 1L; // For now, return a default tenant ID
    }
}
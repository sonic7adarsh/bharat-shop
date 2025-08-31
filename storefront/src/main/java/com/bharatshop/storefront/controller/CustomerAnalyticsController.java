package com.bharatshop.storefront.controller;

import com.bharatshop.shared.dto.CustomerAnalyticsDto;
import com.bharatshop.storefront.service.CustomerAnalyticsService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * REST controller for customer analytics and dashboard operations.
 * Provides analytics data for customer dashboard including order history, spending metrics, and behavior insights.
 */
@RestController
@RequestMapping("/api/customer/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Analytics", description = "Analytics APIs for customer dashboard")
public class CustomerAnalyticsController {
    
    private final CustomerAnalyticsService customerAnalyticsService;
    
    /**
     * Get comprehensive customer dashboard analytics
     * GET /api/customer/analytics/dashboard
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get customer dashboard analytics", description = "Retrieve comprehensive analytics for customer dashboard")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<CustomerAnalyticsDto>> getCustomerDashboard(
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long customerId = extractCustomerId(request);
            Long tenantId = extractTenantId(request);
            
            log.info("Getting customer dashboard analytics for customer: {}, tenant: {}", customerId, tenantId);
            
            CustomerAnalyticsDto analytics = customerAnalyticsService.getCustomerAnalytics(customerId, tenantId);
            
            return ResponseEntity.ok(
                ApiResponse.success(analytics, "Customer analytics retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error getting customer dashboard analytics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to retrieve customer analytics"));
        }
    }
    
    /**
     * Get order statistics only
     * GET /api/customer/analytics/orders
     */
    @GetMapping("/orders")
    @Operation(summary = "Get order statistics", description = "Retrieve customer order statistics and metrics")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderAnalytics(
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long customerId = extractCustomerId(request);
            Long tenantId = extractTenantId(request);
            
            log.info("Getting order analytics for customer: {}, tenant: {}", customerId, tenantId);
            
            CustomerAnalyticsDto analytics = customerAnalyticsService.getCustomerAnalytics(customerId, tenantId);
            
            // Return only order-related metrics
            Map<String, Object> orderStatusBreakdown = Map.of(
                "draft", analytics.getDraftOrders(),
                "confirmed", analytics.getConfirmedOrders(),
                "packed", analytics.getPackedOrders(),
                "shipped", analytics.getShippedOrders(),
                "delivered", analytics.getDeliveredOrders(),
                "returned", analytics.getReturnedOrders()
            );
            
            Map<String, Object> orderStats = Map.of(
                "totalOrders", analytics.getTotalOrders(),
                "completedOrders", analytics.getCompletedOrders(),
                "pendingOrders", analytics.getPendingOrders(),
                "cancelledOrders", analytics.getCancelledOrders(),
                "orderStatusBreakdown", orderStatusBreakdown
            );
            
            return ResponseEntity.ok(
                ApiResponse.success(orderStats, "Order analytics retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error getting order analytics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to retrieve order analytics"));
        }
    }
    
    /**
     * Get spending analytics
     * GET /api/customer/analytics/spending
     */
    @GetMapping("/spending")
    @Operation(summary = "Get spending analytics", description = "Retrieve customer spending metrics and patterns")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSpendingAnalytics(
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long customerId = extractCustomerId(request);
            Long tenantId = extractTenantId(request);
            
            log.info("Getting spending analytics for customer: {}, tenant: {}", customerId, tenantId);
            
            CustomerAnalyticsDto analytics = customerAnalyticsService.getCustomerAnalytics(customerId, tenantId);
            
            // Return only spending-related metrics
            Map<String, Object> spendingStats = Map.of(
                "totalSpent", analytics.getTotalSpent(),
                "averageOrderValue", analytics.getAverageOrderValue()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success(spendingStats, "Spending analytics retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error getting spending analytics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to retrieve spending analytics"));
        }
    }
    
    /**
     * Get recent order history
     * GET /api/customer/analytics/recent-orders
     */
    @GetMapping("/recent-orders")
    @Operation(summary = "Get recent order history", description = "Retrieve recent order history for customer")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentOrders(
            @Parameter(description = "Number of recent orders to retrieve")
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long customerId = extractCustomerId(request);
            Long tenantId = extractTenantId(request);
            
            log.info("Getting recent orders for customer: {}, tenant: {}, limit: {}", customerId, tenantId, limit);
            
            CustomerAnalyticsDto analytics = customerAnalyticsService.getCustomerAnalytics(customerId, tenantId);
            
            // Return recent orders (limited by the limit parameter)
            Map<String, Object> recentOrdersData = Map.of(
                "recentOrders", analytics.getRecentOrders().stream().limit(limit).toList()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success(recentOrdersData, "Recent orders retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error getting recent orders", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to retrieve recent orders"));
        }
    }
    
    /**
     * Get customer behavior metrics
     * GET /api/customer/analytics/behavior
     */
    @GetMapping("/behavior")
    @Operation(summary = "Get customer behavior metrics", description = "Retrieve customer behavior and engagement metrics")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBehaviorAnalytics(
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long customerId = extractCustomerId(request);
            Long tenantId = extractTenantId(request);
            
            log.info("Getting behavior analytics for customer: {}, tenant: {}", customerId, tenantId);
            
            CustomerAnalyticsDto analytics = customerAnalyticsService.getCustomerAnalytics(customerId, tenantId);
            
            // Return behavior-related metrics
            Map<String, Object> behaviorStats = Map.of(
                "lastOrderDate", analytics.getLastOrderDate(),
                "firstOrderDate", analytics.getFirstOrderDate(),
                "daysSinceLastOrder", analytics.getDaysSinceLastOrder(),
                "totalOrderDays", analytics.getTotalOrderDays()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success(behaviorStats, "Behavior analytics retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error getting behavior analytics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to retrieve behavior analytics"));
        }
    }
    
    // Helper methods to extract customer and tenant information
    // These follow the same pattern as StorefrontOrderController
    
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
        // TODO: Extract from tenant domain or session
        // For now, using header for testing
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        if (tenantIdHeader != null) {
            return Long.parseLong(tenantIdHeader);
        }
        throw new RuntimeException("Tenant ID not found in request");
    }
}
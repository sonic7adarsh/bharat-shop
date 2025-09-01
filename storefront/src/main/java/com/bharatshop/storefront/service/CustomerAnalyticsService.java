package com.bharatshop.storefront.service;

import com.bharatshop.shared.dto.CustomerAnalyticsDto;
import com.bharatshop.storefront.entity.Order;
import com.bharatshop.shared.entity.OrderItem;
import com.bharatshop.storefront.repository.StorefrontOrderRepository;
import com.bharatshop.storefront.repository.StorefrontOrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for customer analytics and dashboard operations.
 * Provides methods to calculate customer-specific metrics including order history,
 * statistics, and behavioral data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAnalyticsService {
    
    @Qualifier("storefrontOrderRepository")
    private final StorefrontOrderRepository orderRepository;
    @Qualifier("storefrontOrderItemRepository")
    private final StorefrontOrderItemRepository orderItemRepository;
    
    /**
     * Get comprehensive customer analytics for dashboard
     */
    public CustomerAnalyticsDto getCustomerAnalytics(Long customerId, Long tenantId) {
        log.debug("Calculating customer analytics for customer: {} in tenant: {}", customerId, tenantId);
        
        return CustomerAnalyticsDto.builder()
                .totalOrders(getTotalOrders(customerId, tenantId))
                .completedOrders(getCompletedOrders(customerId, tenantId))
                .pendingOrders(getPendingOrders(customerId, tenantId))
                .cancelledOrders(getCancelledOrders(customerId, tenantId))
                .totalSpent(getTotalSpent(customerId, tenantId))
                .averageOrderValue(getAverageOrderValue(customerId, tenantId))
                .recentOrders(getRecentOrders(customerId, tenantId, 10))
                .draftOrders(getOrdersByStatus(customerId, tenantId, Order.OrderStatus.DRAFT))
                .confirmedOrders(getOrdersByStatus(customerId, tenantId, Order.OrderStatus.CONFIRMED))
                .packedOrders(getOrdersByStatus(customerId, tenantId, Order.OrderStatus.PACKED))
                .shippedOrders(getOrdersByStatus(customerId, tenantId, Order.OrderStatus.SHIPPED))
                .deliveredOrders(getOrdersByStatus(customerId, tenantId, Order.OrderStatus.DELIVERED))
                .returnedOrders(getOrdersByStatus(customerId, tenantId, Order.OrderStatus.RETURNED))
                .lastOrderDate(getLastOrderDate(customerId, tenantId))
                .firstOrderDate(getFirstOrderDate(customerId, tenantId))
                .daysSinceLastOrder(getDaysSinceLastOrder(customerId, tenantId))
                .totalOrderDays(getTotalOrderDays(customerId, tenantId))
                .totalWishlistItems(0L) // Placeholder for future wishlist feature
                .recentWishlistItems(List.of()) // Placeholder for future wishlist feature
                .favoriteProducts(getFavoriteProducts(customerId, tenantId, 5))
                .build();
    }
    
    /**
     * Get total number of orders for customer
     */
    private Long getTotalOrders(Long customerId, Long tenantId) {
        return orderRepository.countByCustomerIdAndTenantId(customerId, tenantId);
    }
    
    /**
     * Get number of completed orders for customer
     */
    private Long getCompletedOrders(Long customerId, Long tenantId) {
        return orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, tenantId, Order.OrderStatus.DELIVERED);
    }
    
    /**
     * Get number of pending orders for customer
     */
    private Long getPendingOrders(Long customerId, Long tenantId) {
        return orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, tenantId, Order.OrderStatus.PENDING);
    }
    
    /**
     * Get number of cancelled orders for customer
     */
    private Long getCancelledOrders(Long customerId, Long tenantId) {
        return orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, tenantId, Order.OrderStatus.CANCELLED);
    }
    
    /**
     * Get total amount spent by customer
     */
    private BigDecimal getTotalSpent(Long customerId, Long tenantId) {
        BigDecimal total = orderRepository.getTotalOrderValueByCustomer(customerId, tenantId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Calculate average order value for customer
     */
    private BigDecimal getAverageOrderValue(Long customerId, Long tenantId) {
        BigDecimal totalSpent = getTotalSpent(customerId, tenantId);
        Long totalOrders = getTotalOrders(customerId, tenantId);
        
        if (totalOrders == 0 || totalSpent.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get recent orders for customer
     */
    private List<CustomerAnalyticsDto.OrderHistoryData> getRecentOrders(Long customerId, Long tenantId, int limit) {
        List<Order> orders = orderRepository.findRecentOrdersByCustomer(customerId, tenantId, PageRequest.of(0, limit));
        
        return orders.stream()
                .map(this::convertToOrderHistoryData)
                .collect(Collectors.toList());
    }
    
    /**
     * Get number of orders by status
     */
    private Long getOrdersByStatus(Long customerId, Long tenantId, Order.OrderStatus status) {
        return orderRepository.countByCustomerIdAndTenantIdAndStatus(customerId, tenantId, status);
    }
    
    /**
     * Get last order date for customer
     */
    private LocalDateTime getLastOrderDate(Long customerId, Long tenantId) {
        List<Order> recentOrders = orderRepository.findRecentOrdersByCustomer(customerId, tenantId, PageRequest.of(0, 1));
        return recentOrders.isEmpty() ? null : recentOrders.get(0).getCreatedAt();
    }
    
    /**
     * Get first order date for customer
     */
    private LocalDateTime getFirstOrderDate(Long customerId, Long tenantId) {
        // This would require a different query to get the oldest order
        // For now, returning null as placeholder
        return null;
    }
    
    /**
     * Calculate days since last order
     */
    private Integer getDaysSinceLastOrder(Long customerId, Long tenantId) {
        LocalDateTime lastOrderDate = getLastOrderDate(customerId, tenantId);
        if (lastOrderDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(lastOrderDate, LocalDateTime.now());
    }
    
    /**
     * Calculate total days as customer
     */
    private Integer getTotalOrderDays(Long customerId, Long tenantId) {
        LocalDateTime firstOrderDate = getFirstOrderDate(customerId, tenantId);
        if (firstOrderDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(firstOrderDate, LocalDateTime.now());
    }
    
    /**
     * Get favorite products based on purchase frequency and amount
     */
    private List<CustomerAnalyticsDto.FavoriteProductData> getFavoriteProducts(Long customerId, Long tenantId, int limit) {
        // This would require complex queries to analyze purchase patterns
        // For now, returning empty list as placeholder
        return List.of();
    }
    
    /**
     * Convert Order entity to OrderHistoryData DTO
     */
    private CustomerAnalyticsDto.OrderHistoryData convertToOrderHistoryData(Order order) {
        List<CustomerAnalyticsDto.OrderItemData> items = order.getItems() != null ?
                order.getItems().stream()
                        .map(this::convertToOrderItemData)
                        .collect(Collectors.toList()) :
                List.of();
        
        return CustomerAnalyticsDto.OrderHistoryData.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getCreatedAt())
                .deliveredDate(order.getDeliveredAt())
                .totalItems(items.size())
                .trackingNumber(order.getTrackingNumber())
                .courierPartner(order.getCourierPartner())
                .items(items)
                .build();
    }
    
    /**
     * Convert OrderItem entity to OrderItemData DTO
     */
    private CustomerAnalyticsDto.OrderItemData convertToOrderItemData(OrderItem orderItem) {
        BigDecimal totalAmount = orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        if (orderItem.getDiscountAmount() != null) {
            totalAmount = totalAmount.subtract(orderItem.getDiscountAmount());
        }
        
        return CustomerAnalyticsDto.OrderItemData.builder()
                .productId(Long.valueOf(orderItem.getProduct().getId().toString().hashCode()))
                .productName(orderItem.getProductName())
                .productSku(orderItem.getProductSku())
                .productImageUrl(orderItem.getProductImageUrl())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .discountAmount(orderItem.getDiscountAmount())
                .totalAmount(totalAmount)
                .build();
    }
}
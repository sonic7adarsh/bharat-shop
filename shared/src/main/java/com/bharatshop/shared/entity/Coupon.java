package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Coupon entity representing discount coupons for the e-commerce platform.
 * Supports percentage and fixed amount discounts with various constraints and eligibility rules.
 */
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_coupon_code", columnList = "code"),
        @Index(name = "idx_coupon_tenant_code", columnList = "tenant_id, code"),
        @Index(name = "idx_coupon_start_date", columnList = "start_date"),
        @Index(name = "idx_coupon_end_date", columnList = "end_date"),
        @Index(name = "idx_coupon_active", columnList = "is_active"),
        @Index(name = "idx_coupon_first_order", columnList = "first_order_only"),
        @Index(name = "idx_coupon_active_dates", columnList = "is_active, start_date, end_date"),
        @Index(name = "idx_coupon_tenant_active_dates", columnList = "tenant_id, is_active, start_date, end_date"),
        @Index(name = "idx_coupon_usage_limit", columnList = "usage_limit, usage_count"),
        @Index(name = "idx_coupon_eligible_categories", columnList = "eligible_categories"),
        @Index(name = "idx_coupon_eligible_products", columnList = "eligible_products")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_coupon_tenant_code", columnNames = {"tenant_id", "code"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Coupon extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_cart_amount", precision = 10, scale = 2)
    private BigDecimal minCartAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "per_customer_limit")
    private Integer perCustomerLimit;

    @Column(name = "first_order_only", nullable = false)
    private Boolean firstOrderOnly = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", length = 500)
    private String description;

    // Eligible categories (comma-separated category IDs)
    @Column(name = "eligible_categories", length = 1000)
    private String eligibleCategories;

    // Eligible products (comma-separated product IDs)
    @Column(name = "eligible_products", length = 2000)
    private String eligibleProducts;

    /**
     * Coupon type enumeration
     */
    public enum CouponType {
        PERCENT,  // Percentage discount
        FIXED     // Fixed amount discount
    }

    /**
     * Check if the coupon is currently valid (active and within date range)
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               !isDeleted() && 
               (startDate == null || !now.isBefore(startDate)) && 
               (endDate == null || !now.isAfter(endDate));
    }

    /**
     * Check if the coupon has reached its usage limit
     */
    public boolean hasReachedUsageLimit() {
        return usageLimit != null && usageCount >= usageLimit;
    }

    /**
     * Check if the coupon is applicable to a specific cart amount
     */
    public boolean isApplicableToCartAmount(BigDecimal cartAmount) {
        return minCartAmount == null || cartAmount.compareTo(minCartAmount) >= 0;
    }

    // Manual getters since Lombok is not working properly
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public CouponType getType() { return type; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getMinCartAmount() { return minCartAmount; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public Integer getUsageLimit() { return usageLimit; }
    public Integer getUsageCount() { return usageCount; }
    public Integer getPerCustomerLimit() { return perCustomerLimit; }
    public Boolean getFirstOrderOnly() { return firstOrderOnly; }
    public Boolean getIsActive() { return isActive; }
    public String getDescription() { return description; }
    public String getEligibleCategories() { return eligibleCategories; }
    public String getEligibleProducts() { return eligibleProducts; }
    
    // Manual getters for BaseEntity fields
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public java.time.LocalDateTime getDeletedAt() { return deletedAt; }

    // Manual setters since Lombok is not working properly
    public void setCode(String code) { this.code = code; }
    public void setType(CouponType type) { this.type = type; }
    public void setValue(BigDecimal value) { this.value = value; }
    public void setMinCartAmount(BigDecimal minCartAmount) { this.minCartAmount = minCartAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public void setPerCustomerLimit(Integer perCustomerLimit) { this.perCustomerLimit = perCustomerLimit; }
    public void setFirstOrderOnly(Boolean firstOrderOnly) { this.firstOrderOnly = firstOrderOnly; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setDescription(String description) { this.description = description; }
    public void setEligibleCategories(String eligibleCategories) { this.eligibleCategories = eligibleCategories; }
    public void setEligibleProducts(String eligibleProducts) { this.eligibleProducts = eligibleProducts; }

    /**
     * Calculate discount amount for a given cart total
     */
    public BigDecimal calculateDiscount(BigDecimal cartAmount) {
        if (!isApplicableToCartAmount(cartAmount)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discount;
        if (type == CouponType.PERCENT) {
            discount = cartAmount.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = value.setScale(2, RoundingMode.HALF_UP);
        }

        // Apply maximum discount limit if specified
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount.setScale(2, RoundingMode.HALF_UP);
        }

        return discount;
    }

    /**
     * Get eligible category IDs as a set
     */
    public Set<Long> getEligibleCategoryIds() {
        if (eligibleCategories == null || eligibleCategories.trim().isEmpty()) {
            return Set.of();
        }
        return Set.of(eligibleCategories.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get eligible product IDs as a set
     */
    public Set<Long> getEligibleProductIds() {
        if (eligibleProducts == null || eligibleProducts.trim().isEmpty()) {
            return Set.of();
        }
        return Set.of(eligibleProducts.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Check if coupon is applicable to all products/categories (no restrictions)
     */
    public boolean isApplicableToAll() {
        return (eligibleCategories == null || eligibleCategories.trim().isEmpty()) &&
               (eligibleProducts == null || eligibleProducts.trim().isEmpty());
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Coupon;
import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.exception.BusinessException;
import com.bharatshop.shared.repository.CouponRepository;
import com.bharatshop.shared.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service class for coupon operations.
 * Handles business logic for coupon validation, usage tracking, and discount calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CouponService {
    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    /**
     * Create a new coupon
     */
    public Coupon createCoupon(Coupon coupon) {
        log.debug("Creating coupon with code: {} for tenant: {}", coupon.getCode(), coupon.getTenantId());
        
        validateCouponForCreation(coupon);
        
        // Check if coupon code already exists for this tenant
        if (couponRepository.existsByCodeAndTenantId(coupon.getCode(), coupon.getTenantId())) {
            throw BusinessException.alreadyExists("Coupon", "code", coupon.getCode());
        }
        
        // Set default values
        if (coupon.getUsageCount() == null) {
            coupon.setUsageCount(0);
        }
        if (coupon.getIsActive() == null) {
            coupon.setIsActive(true);
        }
        if (coupon.getFirstOrderOnly() == null) {
            coupon.setFirstOrderOnly(false);
        }
        
        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Created coupon with ID: {} and code: {}", savedCoupon.getId(), savedCoupon.getCode());
        return savedCoupon;
    }

    /**
     * Update an existing coupon
     */
    public Coupon updateCoupon(Long couponId, Coupon updatedCoupon, Long tenantId) {
        log.debug("Updating coupon with ID: {} for tenant: {}", couponId, tenantId);
        
        Coupon existingCoupon = getCouponById(couponId, tenantId);
        
        // Validate the updated coupon
        validateCouponForUpdate(updatedCoupon, existingCoupon);
        
        // Check if code is being changed and if new code already exists
        if (!existingCoupon.getCode().equals(updatedCoupon.getCode()) &&
            couponRepository.existsByCodeAndTenantId(updatedCoupon.getCode(), tenantId)) {
            throw BusinessException.alreadyExists("Coupon", "code", updatedCoupon.getCode());
        }
        
        // Update fields (preserve usage count and creation audit fields)
        existingCoupon.setCode(updatedCoupon.getCode());
        existingCoupon.setType(updatedCoupon.getType());
        existingCoupon.setValue(updatedCoupon.getValue());
        existingCoupon.setMinCartAmount(updatedCoupon.getMinCartAmount());
        existingCoupon.setMaxDiscountAmount(updatedCoupon.getMaxDiscountAmount());
        existingCoupon.setStartDate(updatedCoupon.getStartDate());
        existingCoupon.setEndDate(updatedCoupon.getEndDate());
        existingCoupon.setUsageLimit(updatedCoupon.getUsageLimit());
        existingCoupon.setPerCustomerLimit(updatedCoupon.getPerCustomerLimit());
        existingCoupon.setFirstOrderOnly(updatedCoupon.getFirstOrderOnly());
        existingCoupon.setIsActive(updatedCoupon.getIsActive());
        existingCoupon.setDescription(updatedCoupon.getDescription());
        existingCoupon.setEligibleCategories(updatedCoupon.getEligibleCategories());
        existingCoupon.setEligibleProducts(updatedCoupon.getEligibleProducts());
        
        Coupon savedCoupon = couponRepository.save(existingCoupon);
        log.info("Updated coupon with ID: {}", savedCoupon.getId());
        return savedCoupon;
    }

    /**
     * Get coupon by ID and tenant
     */
    @Transactional(readOnly = true)
    public Coupon getCouponById(Long couponId, Long tenantId) {
        return couponRepository.findById(couponId)
                .filter(coupon -> coupon.getTenantId().equals(tenantId) && coupon.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Coupon", couponId));
    }

    /**
     * Get coupon by code and tenant
     */
    @Transactional(readOnly = true)
    public Optional<Coupon> getCouponByCode(String code, Long tenantId) {
        return couponRepository.findByCodeAndTenantId(code, tenantId);
    }

    /**
     * Get all active coupons for a tenant
     */
    @Transactional(readOnly = true)
    public List<Coupon> getActiveCoupons(Long tenantId) {
        return couponRepository.findActiveCouponsByTenantId(tenantId);
    }

    /**
     * Get all valid coupons (active and within date range) for a tenant
     */
    @Transactional(readOnly = true)
    public List<Coupon> getValidCoupons(Long tenantId) {
        return couponRepository.findValidCouponsByTenantId(tenantId, LocalDateTime.now());
    }

    /**
     * Validate and apply coupon to cart
     */
    public CouponValidationResult validateAndApplyCoupon(String couponCode, Long tenantId, Long customerId, 
                                                         BigDecimal cartAmount, Set<Long> categoryIds, 
                                                         Set<Long> productIds, boolean isFirstOrder) {
        log.debug("Validating coupon: {} for customer: {} with cart amount: {}", couponCode, customerId, cartAmount);
        
        // Find the coupon
        Optional<Coupon> couponOpt = couponRepository.findValidCouponByCodeAndTenantId(couponCode, tenantId, LocalDateTime.now());
        if (couponOpt.isEmpty()) {
            return CouponValidationResult.invalid("Coupon not found or expired");
        }
        
        Coupon coupon = couponOpt.get();
        
        // Validate coupon constraints
        CouponValidationResult validationResult = validateCouponConstraints(coupon, customerId, tenantId, cartAmount, new ArrayList<>(productIds), new ArrayList<>(categoryIds), isFirstOrder);
        if (!validationResult.isValid()) {
            return validationResult;
        }
        
        // Calculate discount
        BigDecimal discountAmount = coupon.calculateDiscount(cartAmount);
        
        return CouponValidationResult.valid(coupon, discountAmount);
    }

    /**
     * Apply coupon usage atomically with full validation
     * Returns true if successful, false if usage limit reached or coupon invalid
     */
    public boolean applyCouponUsage(Long couponId, Long tenantId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int rowsAffected = couponRepository.incrementUsageCountAtomically(couponId, tenantId, now);
            if (rowsAffected > 0) {
                log.info("Coupon usage applied atomically for coupon ID: {}", couponId);
                return true;
            }
            log.warn("Failed to apply coupon usage atomically - usage limit reached or coupon invalid for ID: {}", couponId);
            return false;
        } catch (Exception e) {
            log.error("Failed to apply coupon usage atomically for coupon ID: {}", couponId, e);
            return false;
        }
    }

    /**
     * Rollback coupon usage atomically
     * Returns true if successful, false if already at zero
     */
    public boolean rollbackCouponUsage(Long couponId, Long tenantId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int rowsAffected = couponRepository.decrementUsageCountAtomically(couponId, tenantId, now);
            if (rowsAffected > 0) {
                log.info("Coupon usage rolled back atomically for coupon ID: {}", couponId);
                return true;
            }
            log.warn("Failed to rollback coupon usage - already at zero for coupon ID: {}", couponId);
            return false;
        } catch (Exception e) {
            log.error("Failed to rollback coupon usage atomically for coupon ID: {}", couponId, e);
            return false;
        }
    }

    /**
     * Soft delete a coupon
     */
    public void deleteCoupon(Long couponId, Long tenantId) {
        log.debug("Deleting coupon with ID: {}", couponId);
        
        Coupon coupon = getCouponById(couponId, tenantId);
        coupon.softDelete();
        couponRepository.save(coupon);
        
        log.info("Deleted coupon with ID: {}", couponId);
    }

    /**
     * Get coupons applicable to specific categories
     */
    @Transactional(readOnly = true)
    public List<Coupon> getCouponsForCategory(Long tenantId, Long categoryId) {
        return couponRepository.findCouponsForCategory(tenantId, categoryId.toString(), LocalDateTime.now());
    }

    /**
     * Get coupons applicable to specific products
     */
    @Transactional(readOnly = true)
    public List<Coupon> getCouponsForProduct(Long tenantId, Long productId) {
        return couponRepository.findCouponsForProduct(tenantId, productId.toString(), LocalDateTime.now());
    }

    /**
     * Get first-order-only coupons
     */
    @Transactional(readOnly = true)
    public List<Coupon> getFirstOrderOnlyCoupons(Long tenantId) {
        return couponRepository.findFirstOrderOnlyCoupons(tenantId, LocalDateTime.now());
    }

    /**
     * Clean up expired coupons (batch operation)
     */
    public int cleanupExpiredCoupons(Long tenantId) {
        log.debug("Cleaning up expired coupons for tenant: {}", tenantId);
        
        int deletedCount = couponRepository.softDeleteExpiredCoupons(tenantId, LocalDateTime.now());
        
        log.info("Cleaned up {} expired coupons for tenant: {}", deletedCount, tenantId);
        return deletedCount;
    }

    // Private validation methods
    
    private void validateCouponForCreation(Coupon coupon) {
        if (!StringUtils.hasText(coupon.getCode())) {
            throw new BusinessException("Coupon code is required", "VALIDATION_ERROR");
        }
        
        if (coupon.getType() == null) {
            throw new BusinessException("Coupon type is required", "VALIDATION_ERROR");
        }
        
        if (coupon.getValue() == null || coupon.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Coupon value must be greater than zero", "VALIDATION_ERROR");
        }
        
        if (coupon.getType() == Coupon.CouponType.PERCENT && coupon.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Percentage discount cannot exceed 100%", "VALIDATION_ERROR");
        }
        
        if (coupon.getStartDate() != null && coupon.getEndDate() != null && 
            coupon.getStartDate().isAfter(coupon.getEndDate())) {
            throw new BusinessException("Start date cannot be after end date", "VALIDATION_ERROR");
        }
        
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() <= 0) {
            throw new BusinessException("Usage limit must be greater than zero", "VALIDATION_ERROR");
        }
        
        if (coupon.getPerCustomerLimit() != null && coupon.getPerCustomerLimit() <= 0) {
            throw new BusinessException("Per customer limit must be greater than zero", "VALIDATION_ERROR");
        }
    }
    
    private void validateCouponForUpdate(Coupon updatedCoupon, Coupon existingCoupon) {
        validateCouponForCreation(updatedCoupon);
        
        // Additional validation for updates
        if (existingCoupon.getUsageCount() > 0 && updatedCoupon.getUsageLimit() != null && 
            updatedCoupon.getUsageLimit() < existingCoupon.getUsageCount()) {
            throw new BusinessException("Cannot set usage limit below current usage count", "VALIDATION_ERROR");
        }
    }
    
    private CouponValidationResult validateCouponConstraints(Coupon coupon, Long customerId, Long tenantId, 
                                                            BigDecimal cartAmount, List<Long> productIds, 
                                                            List<Long> categoryIds, boolean isFirstOrder) {
        
        // Check if coupon has reached usage limit
        if (coupon.hasReachedUsageLimit()) {
            return CouponValidationResult.invalid("Coupon usage limit reached");
        }
        
        // Check minimum cart amount
        if (!coupon.isApplicableToCartAmount(cartAmount)) {
            return CouponValidationResult.invalid("Minimum cart amount not met");
        }
        
        // Check if it's first order only coupon
        if (coupon.getFirstOrderOnly() && !isFirstOrder) {
            return CouponValidationResult.invalid("Coupon is only valid for first orders");
        }
        
        // Check per customer usage limit
        if (coupon.getPerCustomerLimit() != null) {
            long customerUsageCount = getCustomerCouponUsageCount(customerId, coupon.getId(), tenantId);
            if (customerUsageCount >= coupon.getPerCustomerLimit()) {
                return CouponValidationResult.invalid("Per customer usage limit reached");
            }
        }
        
        // Check product/category eligibility
        if (!coupon.isApplicableToAll()) {
            Set<Long> eligibleProductIds = coupon.getEligibleProductIds();
            Set<Long> eligibleCategoryIds = coupon.getEligibleCategoryIds();
            
            boolean isEligible = false;
            
            // Check if any cart products are eligible
            if (!eligibleProductIds.isEmpty()) {
                isEligible = productIds.stream().anyMatch(eligibleProductIds::contains);
            }
            
            // Check if any cart categories are eligible
            if (!isEligible && !eligibleCategoryIds.isEmpty()) {
                isEligible = categoryIds.stream().anyMatch(eligibleCategoryIds::contains);
            }
            
            if (!isEligible) {
                return CouponValidationResult.invalid("Coupon not applicable to cart items");
            }
        }
        
        return CouponValidationResult.valid(coupon, BigDecimal.ZERO);
    }
    
    private boolean hasCustomerPlacedOrders(Long customerId, Long tenantId) {
        // Check if customer has any completed orders
        return orderRepository.hasCompletedOrdersByCustomer(customerId, tenantId);
    }
    
    private long getCustomerCouponUsageCount(Long customerId, Long couponId, Long tenantId) {
        return couponRepository.countCustomerUsage(couponId, customerId);
    }

    /**
     * Result class for coupon validation
     */
    public static class CouponValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Coupon coupon;
        private final BigDecimal discountAmount;
        
        private CouponValidationResult(boolean valid, String errorMessage, Coupon coupon, BigDecimal discountAmount) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.coupon = coupon;
            this.discountAmount = discountAmount;
        }
        
        public static CouponValidationResult valid(Coupon coupon, BigDecimal discountAmount) {
            return new CouponValidationResult(true, null, coupon, discountAmount);
        }
        
        public static CouponValidationResult invalid(String errorMessage) {
            return new CouponValidationResult(false, errorMessage, null, BigDecimal.ZERO);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public Coupon getCoupon() { return coupon; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
    }
}
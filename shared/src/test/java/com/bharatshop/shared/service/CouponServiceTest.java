package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Cart;
import com.bharatshop.shared.entity.Coupon;
import com.bharatshop.shared.entity.User;
import com.bharatshop.shared.repository.CouponRepository;
import com.bharatshop.shared.repository.SharedStorefrontUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CouponService
 * Tests coupon validation logic, discount calculations, and usage management
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private SharedStorefrontUserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    private Long tenantId;
    private Long customerId;
    private String couponCode;
    private Coupon validCoupon;
    private Coupon expiredCoupon;
    private Coupon usageLimitReachedCoupon;
    private Cart cart;
    private User customer;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        customerId = 100L;
        couponCode = "SAVE20";

        // Valid percentage coupon
        validCoupon = Coupon.builder()
                .id(1L)
                .tenantId(tenantId)
                .code(couponCode)
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("20"))
                .minCartAmount(new BigDecimal("100"))
                .maxDiscountAmount(new BigDecimal("50"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .usageCount(10)
                .perCustomerLimit(1)
                .firstOrderOnly(false)
                .isActive(true)
                .eligibleCategories("1,2,3")
                .eligibleProducts("10,20,30")
                .build();

        // Expired coupon
        expiredCoupon = Coupon.builder()
                .id(2L)
                .tenantId(tenantId)
                .code("EXPIRED")
                .type(Coupon.CouponType.FIXED)
                .value(new BigDecimal("10"))
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        // Usage limit reached coupon
        usageLimitReachedCoupon = Coupon.builder()
                .id(3L)
                .tenantId(tenantId)
                .code("MAXED")
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("15"))
                .usageLimit(50)
                .usageCount(50)
                .isActive(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        // Cart with items
        cart = Cart.builder()
                .id(1L)
                .tenantId(tenantId)
                .customerId(customerId)
                .build();

        // Customer
        customer = User.builder()
                .id(customerId)
                .tenantId(tenantId)
                .email("test@example.com")
                .build();
    }

    @Test
    @DisplayName("Should validate coupon successfully when all conditions are met")
    void shouldValidateCouponSuccessfully() {
        // Given
        BigDecimal cartAmount = new BigDecimal("150");
        when(couponRepository.findValidCouponByCodeAndTenantId(eq(couponCode), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validCoupon));
        when(couponRepository.countCustomerUsage(validCoupon.getId(), customerId))
                .thenReturn(0);

        // When
        var result = couponService.validateAndApplyCoupon(couponCode, tenantId, customerId, cartAmount, Set.of(1L, 2L), Set.of(10L, 20L), false);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getCoupon()).isEqualTo(validCoupon);
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("30.00")); // 20% of 150
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should fail validation when coupon not found")
    void shouldFailValidationWhenCouponNotFound() {
        // Given
        when(couponRepository.findValidCouponByCodeAndTenantId(eq(couponCode), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When
        var result = couponService.validateAndApplyCoupon(couponCode, tenantId, customerId, new BigDecimal("100"), Set.of(), Set.of(), false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Coupon not found or expired");
    }

    @Test
    @DisplayName("Should fail validation when coupon is expired")
    void shouldFailValidationWhenCouponExpired() {
        // Given
        when(couponRepository.findValidCouponByCodeAndTenantId(eq("EXPIRED"), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty()); // Expired coupon won't be returned by findValidCoupon

        // When
        var result = couponService.validateAndApplyCoupon("EXPIRED", tenantId, customerId, new BigDecimal("100"), Set.of(), Set.of(), false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Coupon not found or expired");
    }

    @Test
    @DisplayName("Should fail validation when usage limit reached")
    void shouldFailValidationWhenUsageLimitReached() {
        // Given
        when(couponRepository.findValidCouponByCodeAndTenantId(eq("MAXED"), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(usageLimitReachedCoupon));

        // When
        var result = couponService.validateAndApplyCoupon("MAXED", tenantId, customerId, new BigDecimal("100"), Set.of(), Set.of(), false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Coupon usage limit reached");
    }

    @Test
    @DisplayName("Should fail validation when cart amount below minimum")
    void shouldFailValidationWhenCartAmountBelowMinimum() {
        // Given
        BigDecimal cartAmount = new BigDecimal("50"); // Below minimum of 100
        when(couponRepository.findValidCouponByCodeAndTenantId(eq(couponCode), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validCoupon));

        // When
        var result = couponService.validateAndApplyCoupon(couponCode, tenantId, customerId, cartAmount, Set.of(), Set.of(), false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Minimum cart amount not met");
    }

    @Test
    @DisplayName("Should fail validation when customer usage limit exceeded")
    void shouldFailValidationWhenCustomerUsageLimitExceeded() {
        // Given
        BigDecimal cartAmount = new BigDecimal("150");
        when(couponRepository.findValidCouponByCodeAndTenantId(eq(couponCode), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validCoupon));
        when(couponRepository.countCustomerUsage(validCoupon.getId(), customerId))
                .thenReturn(1); // Already used once, limit is 1

        // When
        var result = couponService.validateAndApplyCoupon(couponCode, tenantId, customerId, cartAmount, Set.of(1L), Set.of(10L), false);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Per customer usage limit reached");
    }

    @Test
    @DisplayName("Should apply maximum discount limit for percentage coupons")
    void shouldApplyMaximumDiscountLimit() {
        // Given
        BigDecimal cartAmount = new BigDecimal("500"); // 20% would be 100, but max is 50
        when(couponRepository.findValidCouponByCodeAndTenantId(eq(couponCode), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(validCoupon));
        when(couponRepository.countCustomerUsage(validCoupon.getId(), customerId))
                .thenReturn(0);

        // When
        var result = couponService.validateAndApplyCoupon(couponCode, tenantId, customerId, cartAmount, Set.of(1L), Set.of(10L), false);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("50.00")); // Capped at max discount
    }

    @Test
    @DisplayName("Should apply coupon usage atomically")
    void shouldApplyCouponUsageAtomically() {
        // Given
        when(couponRepository.incrementUsageCountAtomically(eq(1L), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(1);

        // When
        boolean result = couponService.applyCouponUsage(1L, tenantId);

        // Then
        assertThat(result).isTrue();
        verify(couponRepository).incrementUsageCountAtomically(eq(1L), eq(tenantId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should fail to apply coupon usage when atomic operation fails")
    void shouldFailToApplyCouponUsageWhenAtomicOperationFails() {
        // Given
        when(couponRepository.incrementUsageCountAtomically(eq(1L), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(0); // No rows affected

        // When
        boolean result = couponService.applyCouponUsage(1L, tenantId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should rollback coupon usage atomically")
    void shouldRollbackCouponUsageAtomically() {
        // Given
        when(couponRepository.decrementUsageCountAtomically(eq(1L), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(1);

        // When
        boolean result = couponService.rollbackCouponUsage(1L, tenantId);

        // Then
        assertThat(result).isTrue();
        verify(couponRepository).decrementUsageCountAtomically(eq(1L), eq(tenantId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle exception during coupon usage application")
    void shouldHandleExceptionDuringCouponUsageApplication() {
        // Given
        when(couponRepository.incrementUsageCountAtomically(eq(1L), eq(tenantId), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = couponService.applyCouponUsage(1L, tenantId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should calculate fixed discount correctly")
    void shouldCalculateFixedDiscountCorrectly() {
        // Given
        Coupon fixedCoupon = Coupon.builder()
                .type(Coupon.CouponType.FIXED)
                .value(new BigDecimal("25"))
                .build();
        BigDecimal cartAmount = new BigDecimal("100");

        // When
        BigDecimal discount = fixedCoupon.calculateDiscount(cartAmount);

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Should calculate percentage discount correctly")
    void shouldCalculatePercentageDiscountCorrectly() {
        // Given
        Coupon percentCoupon = Coupon.builder()
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("15"))
                .build();
        BigDecimal cartAmount = new BigDecimal("200");

        // When
        BigDecimal discount = percentCoupon.calculateDiscount(cartAmount);

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("30.00")); // 15% of 200
    }
}
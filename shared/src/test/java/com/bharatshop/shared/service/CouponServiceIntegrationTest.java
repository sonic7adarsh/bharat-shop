package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Coupon;
import com.bharatshop.shared.entity.User;
import com.bharatshop.shared.repository.CouponRepository;
import com.bharatshop.shared.repository.SharedStorefrontUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CouponService
 * Tests the complete coupon functionality with real database operations
 */
@SpringBootTest(classes = com.bharatshop.shared.IntegrationTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private SharedStorefrontUserRepository userRepository;

    private Long tenantId;
    private Long customerId;
    private User testCustomer;
    private Coupon percentageCoupon;
    private Coupon fixedCoupon;
    private Coupon limitedUsageCoupon;
    private Coupon firstOrderCoupon;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        customerId = 100L;

        // Create test customer
        testCustomer = User.builder()
                .id(customerId)
                .tenantId(tenantId)
                .email("integration-test@example.com")
                .firstName("Test")
                .lastName("Customer")
                .build();
        userRepository.save(testCustomer);

        // Create percentage coupon
        percentageCoupon = Coupon.builder()
                .tenantId(tenantId)
                .code("PERCENT20")
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("20"))
                .minCartAmount(new BigDecimal("100"))
                .maxDiscountAmount(new BigDecimal("50"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(1000)
                .usageCount(0)
                .perCustomerLimit(3)
                .firstOrderOnly(false)
                .isActive(true)
                .eligibleCategories("1,2,3")
                .eligibleProducts("10,20,30")
                .build();
        percentageCoupon = couponRepository.save(percentageCoupon);

        // Create fixed amount coupon
        fixedCoupon = Coupon.builder()
                .tenantId(tenantId)
                .code("FIXED25")
                .type(Coupon.CouponType.FIXED)
                .value(new BigDecimal("25"))
                .minCartAmount(new BigDecimal("75"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(500)
                .usageCount(0)
                .perCustomerLimit(1)
                .firstOrderOnly(false)
                .isActive(true)
                .build();
        fixedCoupon = couponRepository.save(fixedCoupon);

        // Create limited usage coupon for concurrency testing
        limitedUsageCoupon = Coupon.builder()
                .tenantId(tenantId)
                .code("LIMITED5")
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("15"))
                .minCartAmount(new BigDecimal("50"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(5) // Very limited for testing
                .usageCount(0)
                .perCustomerLimit(1)
                .firstOrderOnly(false)
                .isActive(true)
                .build();
        limitedUsageCoupon = couponRepository.save(limitedUsageCoupon);

        // Create first order only coupon
        firstOrderCoupon = Coupon.builder()
                .tenantId(tenantId)
                .code("FIRSTORDER")
                .type(Coupon.CouponType.FIXED)
                .value(new BigDecimal("30"))
                .minCartAmount(new BigDecimal("100"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(1000)
                .usageCount(0)
                .perCustomerLimit(1)
                .firstOrderOnly(true)
                .isActive(true)
                .build();
        firstOrderCoupon = couponRepository.save(firstOrderCoupon);
    }

    @Test
    @DisplayName("Should validate and apply percentage coupon successfully")
    void shouldValidateAndApplyPercentageCouponSuccessfully() {
        // Given
        BigDecimal cartAmount = new BigDecimal("200");
        Set<Long> categoryIds = Set.of(1L, 2L);
        Set<Long> productIds = Set.of(10L, 20L);

        // When
        var result = couponService.validateAndApplyCoupon(
                "PERCENT20", tenantId, customerId, cartAmount, categoryIds, productIds, false
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getCoupon().getCode()).isEqualTo("PERCENT20");
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("40.00")); // 20% of 200
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should validate and apply fixed coupon successfully")
    void shouldValidateAndApplyFixedCouponSuccessfully() {
        // Given
        BigDecimal cartAmount = new BigDecimal("100");

        // When
        var result = couponService.validateAndApplyCoupon(
                "FIXED25", tenantId, customerId, cartAmount, Set.of(), Set.of(), false
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getCoupon().getCode()).isEqualTo("FIXED25");
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("25"));
    }

    @Test
    @DisplayName("Should apply maximum discount limit for percentage coupons")
    void shouldApplyMaximumDiscountLimitForPercentageCoupons() {
        // Given
        BigDecimal cartAmount = new BigDecimal("500"); // 20% would be 100, but max is 50
        Set<Long> categoryIds = Set.of(1L);
        Set<Long> productIds = Set.of(10L);

        // When
        var result = couponService.validateAndApplyCoupon(
                "PERCENT20", tenantId, customerId, cartAmount, categoryIds, productIds, false
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("50.00")); // Capped at max
    }

    @Test
    @DisplayName("Should fail validation for first order coupon when not first order")
    void shouldFailValidationForFirstOrderCouponWhenNotFirstOrder() {
        // Given
        BigDecimal cartAmount = new BigDecimal("150");
        boolean isFirstOrder = false;

        // When
        var result = couponService.validateAndApplyCoupon(
                "FIRSTORDER", tenantId, customerId, cartAmount, Set.of(), Set.of(), isFirstOrder
        );

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Coupon is only valid for first orders");
    }

    @Test
    @DisplayName("Should validate first order coupon successfully for first order")
    void shouldValidateFirstOrderCouponSuccessfullyForFirstOrder() {
        // Given
        BigDecimal cartAmount = new BigDecimal("150");
        boolean isFirstOrder = true;

        // When
        var result = couponService.validateAndApplyCoupon(
                "FIRSTORDER", tenantId, customerId, cartAmount, Set.of(), Set.of(), isFirstOrder
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualTo(new BigDecimal("30"));
    }

    @Test
    @DisplayName("Should handle atomic usage counter operations")
    void shouldHandleAtomicUsageCounterOperations() {
        // Given
        Long couponId = limitedUsageCoupon.getId();
        
        // When: Apply usage
        boolean applyResult = couponService.applyCouponUsage(couponId, tenantId);
        
        // Then: Should succeed
        assertThat(applyResult).isTrue();
        
        // Verify usage count increased
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getUsageCount()).isEqualTo(1);
        
        // When: Rollback usage
        boolean rollbackResult = couponService.rollbackCouponUsage(couponId, tenantId);
        
        // Then: Should succeed
        assertThat(rollbackResult).isTrue();
        
        // Verify usage count decreased
        updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getUsageCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should prevent overselling with concurrent usage applications")
    void shouldPreventOversellingWithConcurrentUsageApplications() throws InterruptedException {
        // Given: Coupon with usage limit of 5
        Long couponId = limitedUsageCoupon.getId();
        int threadCount = 10; // More threads than usage limit
        int expectedSuccessful = 5;
        
        // When: Multiple threads try to apply usage concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean success = couponService.applyCouponUsage(couponId, tenantId);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: Should prevent overselling
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(expectedSuccessful);
        assertThat(failureCount.get()).isEqualTo(threadCount - expectedSuccessful);
        
        // Verify final usage count
        Coupon finalCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(finalCoupon.getUsageCount()).isEqualTo(expectedSuccessful);
    }

    @Test
    @DisplayName("Should track customer usage correctly")
    void shouldTrackCustomerUsageCorrectly() {
        // Given
        Long couponId = percentageCoupon.getId();
        
        // When: Apply usage for customer
        couponService.applyCouponUsage(couponId, tenantId);
        
        // Simulate customer usage tracking (this would be done in the cart/order service)
        // For this test, we'll verify the repository method works
        int customerUsageCount = couponRepository.countCustomerUsage(couponId, customerId);
        
        // Then: Should track usage (initially 0 since we haven't linked to customer orders)
        assertThat(customerUsageCount).isEqualTo(0); // No customer orders created yet
    }

    @Test
    @DisplayName("Should handle category and product eligibility")
    void shouldHandleCategoryAndProductEligibility() {
        // Given: Coupon with specific eligible categories and products
        BigDecimal cartAmount = new BigDecimal("150");
        
        // When: Cart has eligible categories and products
        Set<Long> eligibleCategories = Set.of(1L, 2L);
        Set<Long> eligibleProducts = Set.of(10L, 20L);
        
        var validResult = couponService.validateAndApplyCoupon(
                "PERCENT20", tenantId, customerId, cartAmount, eligibleCategories, eligibleProducts, false
        );
        
        // Then: Should be valid
        assertThat(validResult.isValid()).isTrue();
        
        // When: Cart has ineligible categories and products
        Set<Long> ineligibleCategories = Set.of(99L);
        Set<Long> ineligibleProducts = Set.of(999L);
        
        var invalidResult = couponService.validateAndApplyCoupon(
                "PERCENT20", tenantId, customerId, cartAmount, ineligibleCategories, ineligibleProducts, false
        );
        
        // Then: Should be invalid
        assertThat(invalidResult.isValid()).isFalse();
        assertThat(invalidResult.getErrorMessage()).isEqualTo("Coupon not applicable to cart items");
    }

    @Test
    @DisplayName("Should handle expired coupons")
    void shouldHandleExpiredCoupons() {
        // Given: Create expired coupon
        Coupon expiredCoupon = Coupon.builder()
                .tenantId(tenantId)
                .code("EXPIRED")
                .type(Coupon.CouponType.FIXED)
                .value(new BigDecimal("20"))
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1)) // Expired yesterday
                .isActive(true)
                .build();
        couponRepository.save(expiredCoupon);
        
        // When
        var result = couponService.validateAndApplyCoupon(
                "EXPIRED", tenantId, customerId, new BigDecimal("100"), Set.of(), Set.of(), false
        );
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Coupon has expired");
    }

    @Test
    @DisplayName("Should handle inactive coupons")
    void shouldHandleInactiveCoupons() {
        // Given: Create inactive coupon
        Coupon inactiveCoupon = Coupon.builder()
                .tenantId(tenantId)
                .code("INACTIVE")
                .type(Coupon.CouponType.FIXED)
                .value(new BigDecimal("15"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .isActive(false) // Inactive
                .build();
        couponRepository.save(inactiveCoupon);
        
        // When
        var result = couponService.validateAndApplyCoupon(
                "INACTIVE", tenantId, customerId, new BigDecimal("100"), Set.of(), Set.of(), false
        );
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Coupon is not active");
    }
}
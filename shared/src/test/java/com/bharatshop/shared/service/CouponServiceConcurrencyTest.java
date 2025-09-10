package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Coupon;
import com.bharatshop.shared.entity.User;
import com.bharatshop.shared.repository.CouponRepository;
import com.bharatshop.shared.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive concurrency tests for CouponService
 * Tests atomic usage counter operations and race condition prevention
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceConcurrencyTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderRepository orderRepository;

    private CouponService couponService;

    private Long tenantId;
    private Long couponId;
    private Coupon testCoupon;
    private User testCustomer;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository, orderRepository);
        
        tenantId = 1L;
        couponId = 100L;
        
        testCoupon = Coupon.builder()
                .id(couponId)
                .tenantId(tenantId)
                .code("CONCURRENT20")
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("20"))
                .minCartAmount(new BigDecimal("50"))
                .maxDiscountAmount(new BigDecimal("100"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(10) // Limited usage for concurrency testing
                .usageCount(0)
                .perCustomerLimit(1)
                .firstOrderOnly(false)
                .isActive(true)
                .build();

        testCustomer = User.builder()
                .id(1L)
                .tenantId(tenantId)
                .email("test@example.com")
                .build();
    }

    @Test
    @DisplayName("Should handle concurrent coupon usage applications without overselling")
    void shouldHandleConcurrentCouponUsageApplications() throws InterruptedException {
        // Given: Coupon with usage limit of 10
        int threadCount = 20; // More threads than usage limit
        int successfulApplications = 10; // Expected successful applications
        
        // Mock atomic increment to succeed only for first 10 calls
        AtomicInteger callCount = new AtomicInteger(0);
        when(couponRepository.incrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    int count = callCount.incrementAndGet();
                    return count <= successfulApplications ? 1 : 0; // Success for first 10, fail for rest
                });

        // When: Multiple threads try to apply coupon usage concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
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

        startLatch.countDown(); // Start all threads
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: Verify atomic behavior
        assertTrue(completed, "All operations should complete within timeout");
        assertEquals(successfulApplications, successCount.get(), "Should have exactly 10 successful applications");
        assertEquals(threadCount - successfulApplications, failureCount.get(), "Remaining applications should fail");
        
        // Verify atomic increment was called for each thread
        verify(couponRepository, times(threadCount)).incrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle concurrent coupon validation without race conditions")
    void shouldHandleConcurrentCouponValidation() throws InterruptedException {
        // Given: Multiple customers trying to validate the same coupon
        int threadCount = 15;
        String couponCode = "CONCURRENT20";
        BigDecimal cartAmount = new BigDecimal("100");
        
        when(couponRepository.findByCodeAndTenantId(couponCode, tenantId))
                .thenReturn(Optional.of(testCoupon));
        // Note: CouponService uses OrderRepository, not UserRepository for validation
        when(couponRepository.countCustomerUsage(eq(couponId), any()))
                .thenReturn(0); // No previous usage

        // When: Multiple threads validate coupon concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger validationCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long customerId = i + 1L; // Different customer for each thread
            executor.submit(() -> {
                try {
                    startLatch.await();
                    var result = couponService.validateAndApplyCoupon(
                            couponCode, tenantId, customerId, cartAmount, 
                            Set.of(1L, 2L), Set.of(10L, 20L), false
                    );
                    
                    if (result.isValid()) {
                        validationCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: All validations should complete successfully
        assertTrue(completed, "All validations should complete within timeout");
        assertEquals(threadCount, validationCount.get(), "All validations should succeed");
        assertEquals(0, errorCount.get(), "No validation errors should occur");
    }

    @Test
    @DisplayName("Should handle concurrent apply and rollback operations")
    void shouldHandleConcurrentApplyAndRollbackOperations() throws InterruptedException {
        // Given: Mixed apply and rollback operations
        int applyThreads = 10;
        int rollbackThreads = 5;
        int totalThreads = applyThreads + rollbackThreads;
        
        // Mock apply operations to succeed
        when(couponRepository.incrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(1);
        
        // Mock rollback operations to succeed
        when(couponRepository.decrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(1);

        // When: Concurrent apply and rollback operations
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalThreads);
        AtomicInteger applySuccessCount = new AtomicInteger(0);
        AtomicInteger rollbackSuccessCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Submit apply operations
        for (int i = 0; i < applyThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean success = couponService.applyCouponUsage(couponId, tenantId);
                    if (success) {
                        applySuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Submit rollback operations
        for (int i = 0; i < rollbackThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean success = couponService.rollbackCouponUsage(couponId, tenantId);
                    if (success) {
                        rollbackSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: All operations should complete successfully
        assertTrue(completed, "All operations should complete within timeout");
        assertEquals(applyThreads, applySuccessCount.get(), "All apply operations should succeed");
        assertEquals(rollbackThreads, rollbackSuccessCount.get(), "All rollback operations should succeed");
        assertEquals(0, errorCount.get(), "No errors should occur");
        
        // Verify atomic operations were called
        verify(couponRepository, times(applyThreads)).incrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class));
        verify(couponRepository, times(rollbackThreads)).decrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle database exceptions during concurrent operations")
    void shouldHandleDatabaseExceptionsDuringConcurrentOperations() throws InterruptedException {
        // Given: Database operations that throw exceptions
        int threadCount = 10;
        
        when(couponRepository.incrementUsageCountAtomically(eq(couponId), eq(tenantId), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When: Multiple threads encounter database exceptions
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean success = couponService.applyCouponUsage(couponId, tenantId);
                    if (!success) {
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

        // Then: All operations should handle exceptions gracefully
        assertTrue(completed, "All operations should complete within timeout");
        assertEquals(threadCount, failureCount.get(), "All operations should fail gracefully");
    }

    @Test
    @DisplayName("Should prevent deadlocks during high concurrency")
    void shouldPreventDeadlocksDuringHighConcurrency() throws InterruptedException {
        // Given: High concurrency scenario with multiple coupon operations
        int threadCount = 50;
        
        when(couponRepository.incrementUsageCountAtomically(any(), any(), any()))
                .thenReturn(1);
        when(couponRepository.decrementUsageCountAtomically(any(), any(), any()))
                .thenReturn(1);

        // When: High concurrency operations
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger completedOperations = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int operationType = i % 3; // Mix of different operations
            executor.submit(() -> {
                try {
                    switch (operationType) {
                        case 0 -> {
                            couponService.applyCouponUsage(couponId, tenantId);
                            completedOperations.incrementAndGet();
                        }
                        case 1 -> {
                            couponService.rollbackCouponUsage(couponId, tenantId);
                            completedOperations.incrementAndGet();
                        }
                        case 2 -> {
                            // Simulate validation operation
                            completedOperations.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Expected when operations fail, but not due to deadlock
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Wait for completion - should not deadlock
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: No deadlock should occur
        assertTrue(completed, "All operations should complete without deadlock");
        assertTrue(completedOperations.get() > 0, "Some operations should succeed");
    }
}
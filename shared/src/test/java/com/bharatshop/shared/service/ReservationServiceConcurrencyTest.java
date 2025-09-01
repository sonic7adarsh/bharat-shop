package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.entity.Reservation;
import com.bharatshop.shared.repository.ProductVariantRepository;
import com.bharatshop.shared.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for concurrent cart scenarios and oversell prevention
 * Tests the ReservationService under high concurrency to ensure stock integrity
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class ReservationServiceConcurrencyTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private ReservationService reservationService;

    private UUID tenantId;
    private UUID productVariantId;
    private ProductVariant productVariant;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, productVariantRepository);
        tenantId = UUID.randomUUID();
        productVariantId = UUID.randomUUID();
        
        productVariant = ProductVariant.builder()
                .id(productVariantId)
                .tenantId(tenantId)
                .stock(10) // Start with 10 items in stock
                .reservedStock(0)
                .build();
    }

    @Test
    void testConcurrentReservationsPreventsOversell() throws InterruptedException {
        // Setup: Product has 10 items in stock
        when(productVariantRepository.findByIdWithLock(productVariantId))
                .thenReturn(Optional.of(productVariant));
        
        // Mock repository to simulate concurrent access
        AtomicInteger totalReserved = new AtomicInteger(0);
        when(reservationRepository.getTotalReservedQuantity(eq(tenantId), eq(productVariantId), any()))
                .thenAnswer(invocation -> totalReserved.get());
        
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    totalReserved.addAndGet(reservation.getQuantity());
                    return reservation;
                });

        // Test: 20 concurrent threads trying to reserve 1 item each
        int numberOfThreads = 20;
        int quantityPerReservation = 1;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        // Submit concurrent reservation tasks
        for (int i = 0; i < numberOfThreads; i++) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    Reservation reservation = reservationService.reserveStock(
                            tenantId, productVariantId, quantityPerReservation, 15);
                    
                    successfulReservations.incrementAndGet();
                    return true;
                    
                } catch (IllegalArgumentException e) {
                    // Expected when stock is insufficient
                    failedReservations.incrementAndGet();
                    return false;
                } catch (Exception e) {
                    failedReservations.incrementAndGet();
                    return false;
                } finally {
                    completionLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");
        
        executor.shutdown();

        // Verify: Only 10 reservations should succeed (matching available stock)
        assertEquals(10, successfulReservations.get(), 
                "Should have exactly 10 successful reservations matching available stock");
        assertEquals(10, failedReservations.get(), 
                "Should have exactly 10 failed reservations due to insufficient stock");
        
        // Verify total reserved quantity doesn't exceed available stock
        assertTrue(totalReserved.get() <= productVariant.getStock(), 
                "Total reserved quantity should not exceed available stock");
    }

    @Test
    void testConcurrentReservationAndRelease() throws InterruptedException {
        // Setup: Product has 5 items in stock
        productVariant.setStock(5);
        when(productVariantRepository.findByIdWithLock(productVariantId))
                .thenReturn(Optional.of(productVariant));
        
        AtomicInteger totalReserved = new AtomicInteger(0);
        List<Reservation> activeReservations = new ArrayList<>();
        
        when(reservationRepository.getTotalReservedQuantity(eq(tenantId), eq(productVariantId), any()))
                .thenAnswer(invocation -> totalReserved.get());
        
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    if (reservation.getStatus() == Reservation.ReservationStatus.ACTIVE) {
                        totalReserved.addAndGet(reservation.getQuantity());
                        synchronized (activeReservations) {
                            activeReservations.add(reservation);
                        }
                    }
                    return reservation;
                });
        
        when(reservationRepository.findByOrderIdAndTenantId(anyLong(), eq(tenantId)))
                .thenAnswer(invocation -> {
                    Long orderId = invocation.getArgument(0);
                    synchronized (activeReservations) {
                        return activeReservations.stream()
                                .filter(r -> orderId.equals(r.getOrderId()))
                                .toList();
                    }
                });

        // Test: Concurrent reservations and releases
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch completionLatch = new CountDownLatch(10);
        AtomicInteger operationCounter = new AtomicInteger(0);

        // Submit mixed reservation and release operations
        for (int i = 0; i < 10; i++) {
            final int operationId = i;
            executor.submit(() -> {
                try {
                    if (operationId % 2 == 0) {
                        // Even operations: Reserve stock
                        try {
                            Reservation reservation = reservationService.reserveStock(
                                    tenantId, productVariantId, 1, 15);
                            reservation.setOrderId((long) operationId);
                            operationCounter.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            // Expected when no stock available
                        }
                    } else {
                        // Odd operations: Release previous reservations
                        Thread.sleep(100); // Small delay to allow some reservations to be made
                        try {
                            reservationService.releaseOrderReservations((long) (operationId - 1), tenantId);
                            // Simulate releasing reserved stock
                            synchronized (activeReservations) {
                                activeReservations.removeIf(r -> Long.valueOf(operationId - 1).equals(r.getOrderId()));
                                totalReserved.decrementAndGet();
                            }
                        } catch (Exception e) {
                            // May fail if no reservation to release
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Wait for completion
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete within timeout");
        
        executor.shutdown();

        // Verify: System remains consistent
        assertTrue(totalReserved.get() >= 0, "Total reserved should never be negative");
        assertTrue(totalReserved.get() <= productVariant.getStock(), 
                "Total reserved should not exceed available stock");
    }

    @Test
    void testReservationExpiryUnderConcurrency() throws InterruptedException {
        // Setup: Product with limited stock
        productVariant.setStock(3);
        when(productVariantRepository.findByIdWithLock(productVariantId))
                .thenReturn(Optional.of(productVariant));
        
        List<Reservation> expiredReservations = new ArrayList<>();
        AtomicInteger totalReserved = new AtomicInteger(0);
        
        when(reservationRepository.getTotalReservedQuantity(eq(tenantId), eq(productVariantId), any()))
                .thenAnswer(invocation -> totalReserved.get());
        
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    totalReserved.addAndGet(reservation.getQuantity());
                    return reservation;
                });
        
        when(reservationRepository.findExpiredReservations(any()))
                .thenReturn(expiredReservations);
        
        when(reservationRepository.releaseExpiredReservations(any()))
                .thenAnswer(invocation -> {
                    int releasedCount = expiredReservations.size();
                    totalReserved.addAndGet(-releasedCount);
                    expiredReservations.clear();
                    return releasedCount;
                });

        // Test: Create reservations that will expire
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch reservationLatch = new CountDownLatch(3);
        
        // Make 3 reservations (using all available stock)
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    // Create reservation with very short expiry (1 second)
                    Reservation reservation = reservationService.reserveStock(
                            tenantId, productVariantId, 1, 0); // 0 minutes = immediate expiry for testing
                    
                    // Simulate expired reservation
                    reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                    expiredReservations.add(reservation);
                    
                } catch (Exception e) {
                    // Handle any reservation failures
                } finally {
                    reservationLatch.countDown();
                }
            });
        }
        
        reservationLatch.await(10, TimeUnit.SECONDS);
        
        // Simulate cleanup of expired reservations
        int releasedCount = reservationService.cleanupExpiredReservations();
        
        // Now try to make new reservations after cleanup
        CountDownLatch newReservationLatch = new CountDownLatch(2);
        AtomicInteger newReservationSuccess = new AtomicInteger(0);
        
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    reservationService.reserveStock(tenantId, productVariantId, 1, 15);
                    newReservationSuccess.incrementAndGet();
                } catch (Exception e) {
                    // May fail if cleanup didn't work properly
                } finally {
                    newReservationLatch.countDown();
                }
            });
        }
        
        newReservationLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify: Expired reservations were cleaned up and new ones could be made
        assertEquals(3, releasedCount, "Should have released 3 expired reservations");
        assertTrue(newReservationSuccess.get() > 0, 
                "Should be able to make new reservations after cleanup");
    }

    @Test
    void testHighConcurrencyStressTest() throws InterruptedException {
        // Setup: Product with moderate stock
        productVariant.setStock(50);
        when(productVariantRepository.findByIdWithLock(productVariantId))
                .thenReturn(Optional.of(productVariant));
        
        AtomicInteger totalReserved = new AtomicInteger(0);
        when(reservationRepository.getTotalReservedQuantity(eq(tenantId), eq(productVariantId), any()))
                .thenAnswer(invocation -> totalReserved.get());
        
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    totalReserved.addAndGet(reservation.getQuantity());
                    return reservation;
                });

        // Test: High concurrency with 100 threads
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Random quantity between 1-3
                    int quantity = (int) (Math.random() * 3) + 1;
                    
                    reservationService.reserveStock(tenantId, productVariantId, quantity, 15);
                    successfulOperations.incrementAndGet();
                    
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    totalOperations.incrementAndGet();
                    completionLatch.countDown();
                }
            });
        }

        // Start stress test
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();

        // Verify results
        assertTrue(completed, "Stress test should complete within timeout");
        assertEquals(numberOfThreads, totalOperations.get(), "All operations should be accounted for");
        
        // Performance verification
        long duration = endTime - startTime;
        assertTrue(duration < 30000, "Stress test should complete within 30 seconds");
        
        // Data integrity verification
        assertTrue(totalReserved.get() <= productVariant.getStock(), 
                "Total reserved should never exceed available stock");
        assertTrue(successfulOperations.get() > 0, "Some operations should succeed");
        
        System.out.printf("Stress Test Results: %d successful, %d failed, %d total in %dms%n", 
                successfulOperations.get(), failedOperations.get(), totalOperations.get(), duration);
    }

    @Test
    void testDeadlockPrevention() throws InterruptedException {
        // Setup: Two products to test cross-product reservations
        UUID productVariantId2 = UUID.randomUUID();
        ProductVariant productVariant2 = ProductVariant.builder()
                .id(productVariantId2)
                .tenantId(tenantId)
                .stock(5)
                .reservedStock(0)
                .build();
        
        when(productVariantRepository.findByIdWithLock(productVariantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantRepository.findByIdWithLock(productVariantId2))
                .thenReturn(Optional.of(productVariant2));
        
        AtomicInteger totalReserved1 = new AtomicInteger(0);
        AtomicInteger totalReserved2 = new AtomicInteger(0);
        
        when(reservationRepository.getTotalReservedQuantity(eq(tenantId), eq(productVariantId), any()))
                .thenAnswer(invocation -> totalReserved1.get());
        when(reservationRepository.getTotalReservedQuantity(eq(tenantId), eq(productVariantId2), any()))
                .thenAnswer(invocation -> totalReserved2.get());
        
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    if (reservation.getProductVariantId().equals(productVariantId)) {
                        totalReserved1.addAndGet(reservation.getQuantity());
                    } else {
                        totalReserved2.addAndGet(reservation.getQuantity());
                    }
                    return reservation;
                });

        // Test: Concurrent operations on different products to test deadlock prevention
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch completionLatch = new CountDownLatch(10);
        AtomicInteger completedOperations = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            final int operationId = i;
            executor.submit(() -> {
                try {
                    if (operationId % 2 == 0) {
                        // Reserve from product 1 then product 2
                        reservationService.reserveStock(tenantId, productVariantId, 1, 15);
                        Thread.sleep(10); // Small delay to increase chance of deadlock
                        reservationService.reserveStock(tenantId, productVariantId2, 1, 15);
                    } else {
                        // Reserve from product 2 then product 1 (reverse order)
                        reservationService.reserveStock(tenantId, productVariantId2, 1, 15);
                        Thread.sleep(10);
                        reservationService.reserveStock(tenantId, productVariantId, 1, 15);
                    }
                    completedOperations.incrementAndGet();
                } catch (Exception e) {
                    // Expected when stock runs out, but not due to deadlock
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Wait for completion - should not deadlock
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify: No deadlock occurred (all operations completed)
        assertTrue(completed, "All operations should complete without deadlock");
        assertTrue(completedOperations.get() > 0, "Some operations should succeed");
    }
}
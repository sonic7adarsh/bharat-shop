package com.bharatshop.shared.performance;

import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.entity.Reservation;
import com.bharatshop.shared.repository.ProductVariantRepository;
import com.bharatshop.shared.repository.ReservationRepository;
import com.bharatshop.shared.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the reservation system
 * These tests measure throughput, latency, and system behavior under load
 * 
 * Run with: -Dperformance.tests.enabled=true
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "performance.tests.enabled", matches = "true")
class ReservationPerformanceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private UUID tenantId;
    private List<ProductVariant> testVariants;
    private static final int NUM_PRODUCTS = 100;
    private static final int STOCK_PER_PRODUCT = 1000;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        testVariants = new ArrayList<>();
        
        // Create multiple product variants for testing
        for (int i = 0; i < NUM_PRODUCTS; i++) {
            ProductVariant variant = ProductVariant.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .productId(UUID.randomUUID())
                    .sku("PERF-TEST-" + i)
                    .stock(STOCK_PER_PRODUCT)
                    .reservedStock(0)
                    .price(BigDecimal.valueOf(99.99))
                    .build();
            
            productVariantRepository.save(variant);
            testVariants.add(variant);
        }
    }

    @Test
    void testReservationThroughput() throws InterruptedException {
        // Test: Measure reservation throughput under concurrent load
        int numberOfThreads = 50;
        int operationsPerThread = 100;
        int totalOperations = numberOfThreads * operationsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        long startTime = System.nanoTime();
                        
                        try {
                            // Random product and quantity
                            ProductVariant variant = testVariants.get(
                                    (int) (Math.random() * testVariants.size()));
                            int quantity = (int) (Math.random() * 5) + 1;
                            
                            reservationService.reserveStock(
                                    tenantId, variant.getId(), quantity, 15);
                            
                            successfulReservations.incrementAndGet();
                            
                        } catch (Exception e) {
                            failedReservations.incrementAndGet();
                        } finally {
                            long endTime = System.nanoTime();
                            long latency = endTime - startTime;
                            totalLatency.addAndGet(latency);
                            latencies.add(latency);
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start performance test
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();
        
        executor.shutdown();

        // Calculate performance metrics
        assertTrue(completed, "Performance test should complete within timeout");
        
        long testDuration = testEndTime - testStartTime;
        double throughput = (double) totalOperations / (testDuration / 1000.0);
        double averageLatencyMs = (totalLatency.get() / (double) totalOperations) / 1_000_000.0;
        
        // Calculate percentiles
        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) (latencies.size() * 0.5)) / 1_000_000;
        long p95 = latencies.get((int) (latencies.size() * 0.95)) / 1_000_000;
        long p99 = latencies.get((int) (latencies.size() * 0.99)) / 1_000_000;
        
        // Performance assertions
        assertTrue(throughput > 100, "Throughput should be > 100 ops/sec, was: " + throughput);
        assertTrue(averageLatencyMs < 100, "Average latency should be < 100ms, was: " + averageLatencyMs);
        assertTrue(p95 < 200, "95th percentile latency should be < 200ms, was: " + p95);
        
        // Print performance report
        System.out.println("\n=== Reservation Performance Test Results ===");
        System.out.printf("Total Operations: %d\n", totalOperations);
        System.out.printf("Successful: %d (%.1f%%)\n", 
                successfulReservations.get(), 
                (successfulReservations.get() * 100.0) / totalOperations);
        System.out.printf("Failed: %d (%.1f%%)\n", 
                failedReservations.get(), 
                (failedReservations.get() * 100.0) / totalOperations);
        System.out.printf("Test Duration: %d ms\n", testDuration);
        System.out.printf("Throughput: %.2f ops/sec\n", throughput);
        System.out.printf("Average Latency: %.2f ms\n", averageLatencyMs);
        System.out.printf("Latency P50: %d ms\n", p50);
        System.out.printf("Latency P95: %d ms\n", p95);
        System.out.printf("Latency P99: %d ms\n", p99);
        System.out.println("============================================\n");
    }

    @Test
    void testReservationCleanupPerformance() throws InterruptedException {
        // Setup: Create many expired reservations
        int numExpiredReservations = 10000;
        List<Reservation> expiredReservations = new ArrayList<>();
        
        for (int i = 0; i < numExpiredReservations; i++) {
            ProductVariant variant = testVariants.get(i % testVariants.size());
            
            Reservation reservation = Reservation.builder()
                    .tenantId(tenantId)
                    .productVariantId(variant.getId())
                    .quantity(1)
                    .expiresAt(LocalDateTime.now().minusHours(1)) // Expired
                    .status(Reservation.ReservationStatus.ACTIVE)
                    .build();
            
            reservationRepository.save(reservation);
            expiredReservations.add(reservation);
        }

        // Test: Measure cleanup performance
        long startTime = System.currentTimeMillis();
        int cleanedUp = reservationService.cleanupExpiredReservations();
        long endTime = System.currentTimeMillis();
        
        long cleanupDuration = endTime - startTime;
        double cleanupRate = (double) cleanedUp / (cleanupDuration / 1000.0);
        
        // Performance assertions
        assertEquals(numExpiredReservations, cleanedUp, "Should cleanup all expired reservations");
        assertTrue(cleanupDuration < 5000, "Cleanup should complete within 5 seconds, took: " + cleanupDuration + "ms");
        assertTrue(cleanupRate > 1000, "Cleanup rate should be > 1000 reservations/sec, was: " + cleanupRate);
        
        System.out.println("\n=== Cleanup Performance Test Results ===");
        System.out.printf("Reservations Cleaned: %d\n", cleanedUp);
        System.out.printf("Cleanup Duration: %d ms\n", cleanupDuration);
        System.out.printf("Cleanup Rate: %.2f reservations/sec\n", cleanupRate);
        System.out.println("==========================================\n");
    }

    @Test
    void testConcurrentReadWritePerformance() throws InterruptedException {
        // Test: Mixed read/write operations under load
        int numberOfThreads = 30;
        int operationsPerThread = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger readOperations = new AtomicInteger(0);
        AtomicInteger writeOperations = new AtomicInteger(0);
        AtomicLong totalReadLatency = new AtomicLong(0);
        AtomicLong totalWriteLatency = new AtomicLong(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        ProductVariant variant = testVariants.get(
                                (int) (Math.random() * testVariants.size()));
                        
                        if (threadId % 3 == 0) {
                            // Read operation: Check available stock
                            long startTime = System.nanoTime();
                            reservationService.getAvailableStock(tenantId, variant.getId());
                            long endTime = System.nanoTime();
                            
                            totalReadLatency.addAndGet(endTime - startTime);
                            readOperations.incrementAndGet();
                            
                        } else {
                            // Write operation: Make reservation
                            long startTime = System.nanoTime();
                            try {
                                reservationService.reserveStock(
                                        tenantId, variant.getId(), 1, 15);
                            } catch (Exception e) {
                                // Expected when stock runs out
                            }
                            long endTime = System.nanoTime();
                            
                            totalWriteLatency.addAndGet(endTime - startTime);
                            writeOperations.incrementAndGet();
                        }
                        
                        // Small random delay to simulate realistic usage
                        Thread.sleep((int) (Math.random() * 10));
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start mixed workload test
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();
        
        executor.shutdown();

        // Calculate performance metrics
        assertTrue(completed, "Mixed workload test should complete within timeout");
        
        long testDuration = testEndTime - testStartTime;
        double readLatencyMs = (totalReadLatency.get() / (double) readOperations.get()) / 1_000_000.0;
        double writeLatencyMs = (totalWriteLatency.get() / (double) writeOperations.get()) / 1_000_000.0;
        double totalThroughput = (double) (readOperations.get() + writeOperations.get()) / (testDuration / 1000.0);
        
        // Performance assertions
        assertTrue(readLatencyMs < 50, "Read latency should be < 50ms, was: " + readLatencyMs);
        assertTrue(writeLatencyMs < 150, "Write latency should be < 150ms, was: " + writeLatencyMs);
        assertTrue(totalThroughput > 50, "Total throughput should be > 50 ops/sec, was: " + totalThroughput);
        
        System.out.println("\n=== Mixed Workload Performance Test Results ===");
        System.out.printf("Read Operations: %d\n", readOperations.get());
        System.out.printf("Write Operations: %d\n", writeOperations.get());
        System.out.printf("Test Duration: %d ms\n", testDuration);
        System.out.printf("Read Latency: %.2f ms\n", readLatencyMs);
        System.out.printf("Write Latency: %.2f ms\n", writeLatencyMs);
        System.out.printf("Total Throughput: %.2f ops/sec\n", totalThroughput);
        System.out.println("===============================================\n");
    }

    @Test
    void testMemoryUsageUnderLoad() throws InterruptedException {
        // Test: Monitor memory usage during high reservation activity
        Runtime runtime = Runtime.getRuntime();
        
        // Measure baseline memory
        System.gc();
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many reservations
        int numberOfReservations = 5000;
        List<Reservation> reservations = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfReservations; i++) {
            ProductVariant variant = testVariants.get(i % testVariants.size());
            
            try {
                Reservation reservation = reservationService.reserveStock(
                        tenantId, variant.getId(), 1, 30);
                reservations.add(reservation);
            } catch (Exception e) {
                // Expected when stock runs out
            }
            
            // Periodic memory check
            if (i % 1000 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - baselineMemory;
                
                // Memory should not grow excessively
                assertTrue(memoryIncrease < 100 * 1024 * 1024, // 100MB limit
                        "Memory usage should not exceed 100MB increase, current: " + 
                        (memoryIncrease / 1024 / 1024) + "MB");
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        // Final memory check
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryIncrease = finalMemory - baselineMemory;
        
        // Cleanup reservations
        int cleanedUp = reservationService.cleanupExpiredReservations();
        
        System.out.println("\n=== Memory Usage Test Results ===");
        System.out.printf("Reservations Created: %d\n", reservations.size());
        System.out.printf("Test Duration: %d ms\n", endTime - startTime);
        System.out.printf("Baseline Memory: %.2f MB\n", baselineMemory / 1024.0 / 1024.0);
        System.out.printf("Final Memory: %.2f MB\n", finalMemory / 1024.0 / 1024.0);
        System.out.printf("Memory Increase: %.2f MB\n", totalMemoryIncrease / 1024.0 / 1024.0);
        System.out.printf("Reservations Cleaned: %d\n", cleanedUp);
        System.out.println("==================================\n");
        
        // Memory usage should be reasonable
        assertTrue(totalMemoryIncrease < 50 * 1024 * 1024, // 50MB limit
                "Total memory increase should be < 50MB, was: " + 
                (totalMemoryIncrease / 1024 / 1024) + "MB");
    }

    @Test
    void testDatabaseConnectionPoolUnderLoad() throws InterruptedException {
        // Test: Ensure database connections are properly managed under load
        int numberOfThreads = 100; // More threads than typical connection pool size
        int operationsPerThread = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger connectionErrors = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            ProductVariant variant = testVariants.get(
                                    (int) (Math.random() * testVariants.size()));
                            
                            // Mix of operations to stress connection pool
                            if (j % 3 == 0) {
                                reservationService.getAvailableStock(tenantId, variant.getId());
                            } else if (j % 3 == 1) {
                                reservationService.reserveStock(tenantId, variant.getId(), 1, 15);
                            } else {
                                reservationService.getActiveReservations(tenantId);
                            }
                            
                            successfulOperations.incrementAndGet();
                            
                        } catch (Exception e) {
                            if (e.getMessage() != null && 
                                (e.getMessage().contains("connection") || 
                                 e.getMessage().contains("pool"))) {
                                connectionErrors.incrementAndGet();
                            }
                            // Other exceptions are expected (stock limitations, etc.)
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start connection pool stress test
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(180, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();
        
        executor.shutdown();

        // Verify results
        assertTrue(completed, "Connection pool test should complete within timeout");
        assertEquals(0, connectionErrors.get(), "Should not have any connection pool errors");
        assertTrue(successfulOperations.get() > 0, "Should have some successful operations");
        
        long testDuration = testEndTime - testStartTime;
        double throughput = (double) successfulOperations.get() / (testDuration / 1000.0);
        
        System.out.println("\n=== Connection Pool Stress Test Results ===");
        System.out.printf("Threads: %d\n", numberOfThreads);
        System.out.printf("Successful Operations: %d\n", successfulOperations.get());
        System.out.printf("Connection Errors: %d\n", connectionErrors.get());
        System.out.printf("Test Duration: %d ms\n", testDuration);
        System.out.printf("Throughput: %.2f ops/sec\n", throughput);
        System.out.println("============================================\n");
    }
}
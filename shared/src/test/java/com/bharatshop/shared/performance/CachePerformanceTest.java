package com.bharatshop.shared.performance;

import com.bharatshop.shared.service.CacheService;
import com.bharatshop.shared.service.HttpCacheService;
import com.bharatshop.shared.service.ImageProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for caching functionality
 * Tests cache hit rates, response times, and concurrent access patterns
 */
@SpringBootTest
class CachePerformanceTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private HttpCacheService httpCacheService;

    @Autowired
    private ImageProcessingService imageProcessingService;

    private static final String TEST_CACHE_NAME = CacheService.PRODUCTS_CACHE;
    private static final int CONCURRENT_THREADS = 10;
    private static final int OPERATIONS_PER_THREAD = 100;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheService.clear(TEST_CACHE_NAME);
    }

    @Test
    @DisplayName("Should demonstrate cache hit performance improvement")
    void shouldDemonstrateCacheHitPerformanceImprovement() {
        // Given
        String cacheKey = "performance-test-key";
        String testData = "Large test data content that would be expensive to generate repeatedly";
        
        // Measure cache miss (first access)
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("cache-miss");
        
        Object result1 = cacheService.get(TEST_CACHE_NAME, cacheKey, Object.class);
        if (result1 == null) {
            // Simulate expensive operation
            simulateExpensiveOperation(50); // 50ms delay
            cacheService.put(TEST_CACHE_NAME, cacheKey, testData);
        }
        
        stopWatch.stop();
        long cacheMissTime = stopWatch.getLastTaskTimeMillis();
        
        // Measure cache hit (subsequent access)
        stopWatch.start("cache-hit");
        
        Object result2 = cacheService.get(TEST_CACHE_NAME, cacheKey, Object.class);
        
        stopWatch.stop();
        long cacheHitTime = stopWatch.getLastTaskTimeMillis();
        
        // Then
        assertThat(result2).isNotNull().isEqualTo(testData);
        assertThat(cacheHitTime).isLessThan(cacheMissTime);
        assertThat(cacheHitTime).isLessThan(10); // Cache hit should be very fast
        
        System.out.printf("Cache miss time: %dms, Cache hit time: %dms, Improvement: %.2fx%n", 
                cacheMissTime, cacheHitTime, (double) cacheMissTime / cacheHitTime);
    }

    @Test
    @DisplayName("Should handle concurrent cache access efficiently")
    void shouldHandleConcurrentCacheAccessEfficiently() throws InterruptedException, ExecutionException {
        // Given
        String cacheKey = "concurrent-test-key";
        String testData = "Concurrent access test data";
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        
        // When - Multiple threads accessing cache concurrently
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                StopWatch threadStopWatch = new StopWatch();
                threadStopWatch.start();
                
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    String key = cacheKey + "-" + j;
                    Object cached = cacheService.get(TEST_CACHE_NAME, key, Object.class);
                    if (cached == null) {
                        cacheService.put(TEST_CACHE_NAME, key, testData + "-" + j);
                    }
                }
                
                threadStopWatch.stop();
                return threadStopWatch.getTotalTimeMillis();
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all threads to complete
        List<Long> executionTimes = new ArrayList<>();
        for (CompletableFuture<Long> future : futures) {
            executionTimes.add(future.get());
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Then
        double averageTime = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minTime = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
        
        assertThat(averageTime).isLessThan(5000); // Should complete within 5 seconds on average
        assertThat(maxTime - minTime).isLessThan(2000); // Execution times should be relatively consistent
        
        System.out.printf("Concurrent cache access - Average: %.2fms, Min: %dms, Max: %dms%n", 
                averageTime, minTime, maxTime);
    }

    @Test
    @DisplayName("Should measure cache memory efficiency")
    void shouldMeasureCacheMemoryEfficiency() {
        // Given
        int numberOfEntries = 1000;
        String baseData = "Test data entry for memory efficiency measurement";
        
        // Measure memory before caching
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Add entries to cache
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("cache-population");
        
        for (int i = 0; i < numberOfEntries; i++) {
            String key = "memory-test-" + i;
            String data = baseData + " - Entry " + i;
            cacheService.put(TEST_CACHE_NAME, key, data);
        }
        
        stopWatch.stop();
        
        // Measure memory after caching
        runtime.gc(); // Suggest garbage collection
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // Then
        long memoryUsed = memoryAfter - memoryBefore;
        long averageMemoryPerEntry = memoryUsed / numberOfEntries;
        long populationTime = stopWatch.getTotalTimeMillis();
        
        assertThat(populationTime).isLessThan(5000); // Should populate quickly
        assertThat(averageMemoryPerEntry).isLessThan(1024); // Should be memory efficient
        
        System.out.printf("Cache memory usage - Total: %d bytes, Per entry: %d bytes, Population time: %dms%n", 
                memoryUsed, averageMemoryPerEntry, populationTime);
    }

    @Test
    @DisplayName("Should measure HTTP cache ETag generation performance")
    void shouldMeasureHttpCacheETagGenerationPerformance() {
        // Given
        List<String> testContents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            testContents.add("Test content for ETag generation - Entry " + i + 
                    " with some additional data to make it more realistic");
        }
        
        // When - Generate ETags
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("etag-generation");
        
        List<String> etags = new ArrayList<>();
        for (String content : testContents) {
            String etag = httpCacheService.generateETag(content);
            etags.add(etag);
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double averageTimePerETag = (double) totalTime / testContents.size();
        
        assertThat(etags).hasSize(testContents.size());
        assertThat(etags.stream().distinct().count()).isEqualTo(testContents.size()); // All ETags should be unique
        assertThat(averageTimePerETag).isLessThan(1.0); // Should be very fast
        
        System.out.printf("ETag generation - Total time: %dms, Average per ETag: %.3fms%n", 
                totalTime, averageTimePerETag);
    }

    @Test
    @DisplayName("Should measure cache eviction performance")
    void shouldMeasureCacheEvictionPerformance() {
        // Given
        int numberOfEntries = 500;
        String testData = "Test data for eviction performance";
        
        // Populate cache
        for (int i = 0; i < numberOfEntries; i++) {
            cacheService.put(TEST_CACHE_NAME, "eviction-test-" + i, testData + i);
        }
        
        // When - Measure individual evictions
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("individual-evictions");
        
        for (int i = 0; i < numberOfEntries / 2; i++) {
            cacheService.evict(TEST_CACHE_NAME, "eviction-test-" + i);
        }
        
        stopWatch.stop();
        long individualEvictionTime = stopWatch.getLastTaskTimeMillis();
        
        // Measure bulk eviction
        stopWatch.start("bulk-eviction");
        cacheService.clear(TEST_CACHE_NAME);
        stopWatch.stop();
        long bulkEvictionTime = stopWatch.getLastTaskTimeMillis();
        
        // Then
        assertThat(individualEvictionTime).isLessThan(1000);
        assertThat(bulkEvictionTime).isLessThan(100);
        
        System.out.printf("Cache eviction - Individual: %dms, Bulk: %dms%n", 
                individualEvictionTime, bulkEvictionTime);
    }

    @Test
    @DisplayName("Should measure cache hit ratio under load")
    void shouldMeasureCacheHitRatioUnderLoad() throws InterruptedException, ExecutionException {
        // Given
        int totalOperations = 10000;
        int uniqueKeys = 100; // This will create a high hit ratio
        String testData = "Cache hit ratio test data";
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<CacheStats>> futures = new ArrayList<>();
        
        // When - Simulate realistic cache access patterns
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            CompletableFuture<CacheStats> future = CompletableFuture.supplyAsync(() -> {
                CacheStats stats = new CacheStats();
                
                for (int j = 0; j < totalOperations / CONCURRENT_THREADS; j++) {
                    String key = "hit-ratio-test-" + (j % uniqueKeys);
                    Object cached = cacheService.get(TEST_CACHE_NAME, key, Object.class);
                    
                    if (cached == null) {
                        stats.misses++;
                        simulateExpensiveOperation(1); // 1ms delay for cache miss
                        cacheService.put(TEST_CACHE_NAME, key, testData + "-" + key);
                    } else {
                        stats.hits++;
                    }
                }
                
                return stats;
            }, executor);
            
            futures.add(future);
        }
        
        // Collect statistics
        CacheStats totalStats = new CacheStats();
        for (CompletableFuture<CacheStats> future : futures) {
            CacheStats stats = future.get();
            totalStats.hits += stats.hits;
            totalStats.misses += stats.misses;
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Then
        double hitRatio = (double) totalStats.hits / (totalStats.hits + totalStats.misses);
        
        assertThat(hitRatio).isGreaterThan(0.8); // Should have high hit ratio
        assertThat(totalStats.hits + totalStats.misses).isEqualTo(totalOperations);
        
        System.out.printf("Cache hit ratio - Hits: %d, Misses: %d, Ratio: %.2f%%%n", 
                totalStats.hits, totalStats.misses, hitRatio * 100);
    }

    private void simulateExpensiveOperation(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class CacheStats {
        long hits = 0;
        long misses = 0;
    }
}
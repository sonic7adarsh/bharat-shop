package com.bharatshop.shared.performance;

import com.bharatshop.shared.service.ImageProcessingService;
import com.bharatshop.shared.service.ImageProcessingService.ImageProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for image processing functionality
 * Tests thumbnail generation, srcset creation, and concurrent processing
 */
@SpringBootTest
class ImageProcessingPerformanceTest {

    @Autowired
    private ImageProcessingService imageProcessingService;

    private static final int CONCURRENT_THREADS = 5;
    private static final int IMAGES_PER_THREAD = 10;

    private MultipartFile smallTestImage;
    private MultipartFile mediumTestImage;
    private MultipartFile largeTestImage;

    @BeforeEach
    void setUp() throws IOException {
        // Create test images of different sizes
        smallTestImage = createTestImage("small-test.jpg", 800, 600);
        mediumTestImage = createTestImage("medium-test.jpg", 1920, 1080);
        largeTestImage = createTestImage("large-test.jpg", 4000, 3000);
    }

    @Test
    @DisplayName("Should measure thumbnail generation performance for different image sizes")
    void shouldMeasureThumbnailGenerationPerformance() throws Exception {
        // Test small image
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("small-image-processing");
        
        ImageProcessingResult smallResult = imageProcessingService.processImage(
                smallTestImage, "small").get();
        
        stopWatch.stop();
        long smallImageTime = stopWatch.getLastTaskTimeMillis();
        
        // Test medium image
        stopWatch.start("medium-image-processing");
        
        ImageProcessingResult mediumResult = imageProcessingService.processImage(
                mediumTestImage, "medium").get();
        
        stopWatch.stop();
        long mediumImageTime = stopWatch.getLastTaskTimeMillis();
        
        // Test large image
        stopWatch.start("large-image-processing");
        
        ImageProcessingResult largeResult = imageProcessingService.processImage(
                largeTestImage, "large").get();
        
        stopWatch.stop();
        long largeImageTime = stopWatch.getLastTaskTimeMillis();
        
        // Assertions
        assertThat(smallResult.getThumbnails()).hasSize(3); // 400, 800, 1200px
        assertThat(mediumResult.getThumbnails()).hasSize(3);
        assertThat(largeResult.getThumbnails()).hasSize(3);
        
        assertThat(imageProcessingService.generateSrcSet(smallResult)).isNotEmpty();
        assertThat(imageProcessingService.generateSrcSet(mediumResult)).isNotEmpty();
        assertThat(imageProcessingService.generateSrcSet(largeResult)).isNotEmpty();
        
        // Performance expectations
        assertThat(smallImageTime).isLessThan(2000); // Small images should process quickly
        assertThat(mediumImageTime).isLessThan(5000); // Medium images
        assertThat(largeImageTime).isLessThan(10000); // Large images may take longer
        
        System.out.printf("Image processing times - Small: %dms, Medium: %dms, Large: %dms%n", 
                smallImageTime, mediumImageTime, largeImageTime);
    }

    @Test
    @DisplayName("Should handle concurrent image processing efficiently")
    void shouldHandleConcurrentImageProcessingEfficiently() throws InterruptedException, ExecutionException {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<ProcessingStats>> futures = new ArrayList<>();
        
        // When - Process images concurrently
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            CompletableFuture<ProcessingStats> future = CompletableFuture.supplyAsync(() -> {
                ProcessingStats stats = new ProcessingStats();
                StopWatch threadStopWatch = new StopWatch();
                threadStopWatch.start();
                
                try {
                    for (int j = 0; j < IMAGES_PER_THREAD; j++) {
                        MultipartFile testImage = (j % 3 == 0) ? smallTestImage : 
                                                 (j % 3 == 1) ? mediumTestImage : largeTestImage;
                        
                        long startTime = System.currentTimeMillis();
                        ImageProcessingResult result = imageProcessingService.processImage(
                                testImage, "concurrent-test-" + threadId + "-" + j).get();
                        long processingTime = System.currentTimeMillis() - startTime;
                        
                        stats.totalProcessingTime += processingTime;
                        stats.processedImages++;
                        
                        if (result.getThumbnails().size() == 3) {
                            stats.successfulProcessing++;
                        }
                    }
                } catch (Exception e) {
                    stats.errors++;
                }
                
                threadStopWatch.stop();
                stats.totalThreadTime = threadStopWatch.getTotalTimeMillis();
                return stats;
            }, executor);
            
            futures.add(future);
        }
        
        // Collect results
        ProcessingStats totalStats = new ProcessingStats();
        for (CompletableFuture<ProcessingStats> future : futures) {
            ProcessingStats stats = future.get();
            totalStats.processedImages += stats.processedImages;
            totalStats.successfulProcessing += stats.successfulProcessing;
            totalStats.errors += stats.errors;
            totalStats.totalProcessingTime += stats.totalProcessingTime;
            totalStats.totalThreadTime = Math.max(totalStats.totalThreadTime, stats.totalThreadTime);
        }
        
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        
        // Then
        double averageProcessingTime = (double) totalStats.totalProcessingTime / totalStats.processedImages;
        double successRate = (double) totalStats.successfulProcessing / totalStats.processedImages;
        
        assertThat(totalStats.processedImages).isEqualTo(CONCURRENT_THREADS * IMAGES_PER_THREAD);
        assertThat(successRate).isGreaterThan(0.95); // 95% success rate
        assertThat(totalStats.errors).isLessThan(3); // Minimal errors
        assertThat(averageProcessingTime).isLessThan(8000); // Average processing time
        
        System.out.printf("Concurrent processing - Images: %d, Success rate: %.2f%%, " +
                "Average time: %.2fms, Total time: %dms%n", 
                totalStats.processedImages, successRate * 100, 
                averageProcessingTime, totalStats.totalThreadTime);
    }

    @Test
    @DisplayName("Should measure srcset generation performance")
    void shouldMeasureSrcsetGenerationPerformance() throws Exception {
        // Given
        int numberOfImages = 50;
        List<MultipartFile> testImages = new ArrayList<>();
        
        for (int i = 0; i < numberOfImages; i++) {
            testImages.add(createTestImage("srcset-test-" + i + ".jpg", 1920, 1080));
        }
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("srcset-generation");
        
        List<String> srcsets = new ArrayList<>();
        for (int i = 0; i < numberOfImages; i++) {
            ImageProcessingResult result = imageProcessingService.processImage(
                    testImages.get(i), "srcset-test-" + i).get();
            srcsets.add(imageProcessingService.generateSrcSet(result));
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double averageTimePerSrcset = (double) totalTime / numberOfImages;
        
        assertThat(srcsets).hasSize(numberOfImages);
        assertThat(srcsets.stream().allMatch(srcset -> srcset.contains("400w") && 
                srcset.contains("800w") && srcset.contains("1200w"))).isTrue();
        assertThat(averageTimePerSrcset).isLessThan(5000); // Should be reasonable
        
        System.out.printf("Srcset generation - Total time: %dms, Average per image: %.2fms%n", 
                totalTime, averageTimePerSrcset);
    }

    @Test
    @DisplayName("Should measure memory usage during image processing")
    void shouldMeasureMemoryUsageDuringImageProcessing() throws Exception {
        // Given
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Process multiple images
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("memory-test-processing");
        
        List<ImageProcessingResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            MultipartFile testImage = createTestImage("memory-test-" + i + ".jpg", 1920, 1080);
            ImageProcessingResult result = imageProcessingService.processImage(
                    testImage, "memory-test-" + i).get();
            results.add(result);
            
            // Check memory usage periodically
            if (i % 5 == 0) {
                runtime.gc();
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - memoryBefore;
                
                // Memory shouldn't grow excessively
                assertThat(memoryIncrease).isLessThan(500 * 1024 * 1024); // 500MB limit
            }
        }
        
        stopWatch.stop();
        
        // Final memory check
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryUsed = memoryAfter - memoryBefore;
        
        // Then
        assertThat(results).hasSize(20);
        assertThat(totalMemoryUsed).isLessThan(200 * 1024 * 1024); // 200MB limit
        
        System.out.printf("Memory usage - Total: %d MB, Processing time: %dms%n", 
                totalMemoryUsed / (1024 * 1024), stopWatch.getTotalTimeMillis());
    }

    @Test
    @DisplayName("Should measure async processing performance")
    void shouldMeasureAsyncProcessingPerformance() throws Exception {
        // Given
        int numberOfImages = 30;
        List<MultipartFile> testImages = new ArrayList<>();
        
        for (int i = 0; i < numberOfImages; i++) {
            testImages.add(createTestImage("async-test-" + i + ".jpg", 1920, 1080));
        }
        
        // When - Process images asynchronously
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("async-processing");
        
        List<CompletableFuture<ImageProcessingResult>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfImages; i++) {
            CompletableFuture<ImageProcessingResult> future = 
                    imageProcessingService.processImage(testImages.get(i), "test");
            futures.add(future);
        }
        
        // Wait for all to complete
        List<ImageProcessingResult> results = new ArrayList<>();
        for (CompletableFuture<ImageProcessingResult> future : futures) {
            results.add(future.get());
        }
        
        stopWatch.stop();
        
        // Then
        long totalTime = stopWatch.getTotalTimeMillis();
        double averageTimePerImage = (double) totalTime / numberOfImages;
        
        assertThat(results).hasSize(numberOfImages);
        assertThat(results.stream().allMatch(result -> 
                result.getThumbnails().size() == 3)).isTrue();
        
        // Async processing should be more efficient for multiple images
        assertThat(averageTimePerImage).isLessThan(3000);
        
        System.out.printf("Async processing - Total time: %dms, Average per image: %.2fms%n", 
                totalTime, averageTimePerImage);
    }

    private MultipartFile createTestImage(String filename, int width, int height) throws IOException {
        // Create a simple test image (this is a mock implementation)
        // In a real scenario, you would create actual image data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Create minimal JPEG header and data
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        baos.write(jpegHeader);
        
        // Add some dummy image data
        byte[] imageData = new byte[width * height / 10]; // Simplified size calculation
        for (int i = 0; i < imageData.length; i++) {
            imageData[i] = (byte) (i % 256);
        }
        baos.write(imageData);
        
        // JPEG end marker
        byte[] jpegEnd = {(byte) 0xFF, (byte) 0xD9};
        baos.write(jpegEnd);
        
        return new MockMultipartFile(
                "image", 
                filename, 
                "image/jpeg", 
                baos.toByteArray()
        );
    }

    private static class ProcessingStats {
        int processedImages = 0;
        int successfulProcessing = 0;
        int errors = 0;
        long totalProcessingTime = 0;
        long totalThreadTime = 0;
    }
}
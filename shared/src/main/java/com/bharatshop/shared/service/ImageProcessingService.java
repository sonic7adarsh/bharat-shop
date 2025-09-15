package com.bharatshop.shared.service;

import com.bharatshop.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for processing images, generating thumbnails, and managing image variants.
 * Supports multiple thumbnail sizes and async processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingService {

    private final CacheService cacheService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Value("${app.image.upload-dir:uploads/images}")
    private String uploadDir;

    @Value("${app.image.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.image.max-file-size:10485760}") // 10MB
    private long maxFileSize;

    // Standard thumbnail sizes
    private static final int[] THUMBNAIL_SIZES = {400, 800, 1200};
    private static final Set<String> SUPPORTED_FORMATS = Set.of("jpg", "jpeg", "png", "webp");

    /**
     * Process uploaded image and generate variants
     */
    public CompletableFuture<ImageProcessingResult> processImage(MultipartFile file, String category) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateImage(file);
                
                String originalFilename = file.getOriginalFilename();
                String fileExtension = getFileExtension(originalFilename);
                String uniqueFilename = generateUniqueFilename(fileExtension);
                
                // Create tenant-specific directory
                Path tenantDir = createTenantDirectory(category);
                
                // Save original image
                Path originalPath = tenantDir.resolve(uniqueFilename);
                file.transferTo(originalPath.toFile());
                
                // Load image for processing
                BufferedImage originalImage = ImageIO.read(originalPath.toFile());
                if (originalImage == null) {
                    throw new IllegalArgumentException("Invalid image format");
                }
                
                // Generate thumbnails
                Map<Integer, String> thumbnails = generateThumbnails(originalImage, tenantDir, uniqueFilename, fileExtension);
                
                // Create result
                ImageProcessingResult result = ImageProcessingResult.builder()
                    .originalUrl(buildUrl(category, uniqueFilename))
                    .originalWidth(originalImage.getWidth())
                    .originalHeight(originalImage.getHeight())
                    .thumbnails(thumbnails)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .filename(uniqueFilename)
                    .build();
                
                // Cache the result
                cacheImageVariants(uniqueFilename, result);
                
                System.out.println("Successfully processed image: " + uniqueFilename + " with " + thumbnails.size() + " thumbnails");
                return result;
                
            } catch (Exception e) {
                System.out.println("Error processing image: " + file.getOriginalFilename() + ", " + e.getMessage());
                throw new RuntimeException("Failed to process image", e);
            }
        }, executorService);
    }

    /**
     * Get image variants from cache or storage
     */
    public Optional<ImageProcessingResult> getImageVariants(String filename) {
        // Try cache first
        ImageProcessingResult cached = cacheService.get(
            CacheService.IMAGE_VARIANTS_CACHE, 
            filename, 
            ImageProcessingResult.class
        );
        
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Try to reconstruct from file system
        try {
            Path tenantDir = getTenantDirectory("images");
            Path originalPath = tenantDir.resolve(filename);
            
            if (!Files.exists(originalPath)) {
                return Optional.empty();
            }
            
            BufferedImage originalImage = ImageIO.read(originalPath.toFile());
            if (originalImage == null) {
                return Optional.empty();
            }
            
            // Check for existing thumbnails
            Map<Integer, String> thumbnails = new HashMap<>();
            String baseFilename = getBaseFilename(filename);
            String extension = getFileExtension(filename);
            
            for (int size : THUMBNAIL_SIZES) {
                String thumbnailName = baseFilename + "_" + size + "." + extension;
                Path thumbnailPath = tenantDir.resolve(thumbnailName);
                if (Files.exists(thumbnailPath)) {
                    thumbnails.put(size, buildUrl("images", thumbnailName));
                }
            }
            
            ImageProcessingResult result = ImageProcessingResult.builder()
                .originalUrl(buildUrl("images", filename))
                .originalWidth(originalImage.getWidth())
                .originalHeight(originalImage.getHeight())
                .thumbnails(thumbnails)
                .fileSize(Files.size(originalPath))
                .filename(filename)
                .build();
            
            // Cache for future use
            cacheImageVariants(filename, result);
            
            return Optional.of(result);
            
        } catch (Exception e) {
            System.out.println("Error retrieving image variants for: " + filename + ", " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete image and all its variants
     */
    public boolean deleteImage(String filename) {
        try {
            Path tenantDir = getTenantDirectory("images");
            String baseFilename = getBaseFilename(filename);
            String extension = getFileExtension(filename);
            
            // Delete original
            Files.deleteIfExists(tenantDir.resolve(filename));
            
            // Delete thumbnails
            for (int size : THUMBNAIL_SIZES) {
                String thumbnailName = baseFilename + "_" + size + "." + extension;
                Files.deleteIfExists(tenantDir.resolve(thumbnailName));
            }
            
            // Remove from cache
            cacheService.evict(CacheService.IMAGE_VARIANTS_CACHE, filename);
            
            System.out.println("Successfully deleted image and variants: " + filename);
            return true;
            
        } catch (Exception e) {
            System.out.println("Error deleting image: " + filename + ", " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate srcset string for responsive images
     */
    public String generateSrcSet(ImageProcessingResult result) {
        if (result.getThumbnails().isEmpty()) {
            return result.getOriginalUrl();
        }
        
        StringBuilder srcSet = new StringBuilder();
        
        // Add thumbnails
        result.getThumbnails().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (srcSet.length() > 0) {
                    srcSet.append(", ");
                }
                srcSet.append(entry.getValue()).append(" ").append(entry.getKey()).append("w");
            });
        
        // Add original as largest size
        if (srcSet.length() > 0) {
            srcSet.append(", ");
        }
        srcSet.append(result.getOriginalUrl()).append(" ").append(result.getOriginalWidth()).append("w");
        
        return srcSet.toString();
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file format: " + extension);
        }
    }

    private Map<Integer, String> generateThumbnails(BufferedImage originalImage, Path directory, 
                                                   String filename, String extension) throws IOException {
        Map<Integer, String> thumbnails = new HashMap<>();
        String baseFilename = getBaseFilename(filename);
        
        for (int size : THUMBNAIL_SIZES) {
            // Skip if original is smaller than thumbnail size
            if (originalImage.getWidth() <= size && originalImage.getHeight() <= size) {
                continue;
            }
            
            BufferedImage thumbnail = Scalr.resize(originalImage, 
                Scalr.Method.QUALITY, 
                Scalr.Mode.AUTOMATIC, 
                size, 
                size, 
                Scalr.OP_ANTIALIAS);
            
            String thumbnailName = baseFilename + "_" + size + "." + extension;
            Path thumbnailPath = directory.resolve(thumbnailName);
            
            ImageIO.write(thumbnail, extension.equals("jpg") ? "jpeg" : extension, thumbnailPath.toFile());
            
            thumbnails.put(size, buildUrl("images", thumbnailName));
        }
        
        return thumbnails;
    }

    private Path createTenantDirectory(String category) throws IOException {
        Long tenantId = TenantContext.getCurrentTenant();
        String tenantPath = tenantId != null ? "tenant-" + tenantId : "global";
        
        Path directory = Paths.get(uploadDir, tenantPath, category);
        Files.createDirectories(directory);
        return directory;
    }

    private Path getTenantDirectory(String category) {
        Long tenantId = TenantContext.getCurrentTenant();
        String tenantPath = tenantId != null ? "tenant-" + tenantId : "global";
        
        return Paths.get(uploadDir, tenantPath, category);
    }

    private String generateUniqueFilename(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private String getBaseFilename(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }

    private String buildUrl(String category, String filename) {
        Long tenantId = TenantContext.getCurrentTenant();
        String tenantPath = tenantId != null ? "tenant-" + tenantId : "global";
        
        return baseUrl + "/uploads/" + tenantPath + "/" + category + "/" + filename;
    }

    private void cacheImageVariants(String filename, ImageProcessingResult result) {
        cacheService.put(CacheService.IMAGE_VARIANTS_CACHE, filename, result);
    }

    /**
     * Result class for image processing operations
     */
    public static class ImageProcessingResult {
        private String originalUrl;
        private int originalWidth;
        private int originalHeight;
        private Map<Integer, String> thumbnails;
        private long fileSize;
        private String contentType;
        private String filename;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getOriginalUrl() { return originalUrl; }
        public int getOriginalWidth() { return originalWidth; }
        public int getOriginalHeight() { return originalHeight; }
        public Map<Integer, String> getThumbnails() { return thumbnails; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public String getFilename() { return filename; }

        public static class Builder {
            private ImageProcessingResult result = new ImageProcessingResult();

            public Builder originalUrl(String originalUrl) {
                result.originalUrl = originalUrl;
                return this;
            }

            public Builder originalWidth(int originalWidth) {
                result.originalWidth = originalWidth;
                return this;
            }

            public Builder originalHeight(int originalHeight) {
                result.originalHeight = originalHeight;
                return this;
            }

            public Builder thumbnails(Map<Integer, String> thumbnails) {
                result.thumbnails = thumbnails;
                return this;
            }

            public Builder fileSize(long fileSize) {
                result.fileSize = fileSize;
                return this;
            }

            public Builder contentType(String contentType) {
                result.contentType = contentType;
                return this;
            }

            public Builder filename(String filename) {
                result.filename = filename;
                return this;
            }

            public ImageProcessingResult build() {
                return result;
            }
        }
    }
}
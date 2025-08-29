package com.bharatshop.platform.service;

import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.ProductImage;
import com.bharatshop.shared.repository.ProductImageRepository;
import com.bharatshop.shared.repository.ProductRepository;
import com.bharatshop.shared.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final FeatureFlagService featureFlagService;
    
    // Configuration - these should be externalized to application.properties
    private static final String UPLOAD_DIR = "uploads/products";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * Get all images for a product
     */
    public List<ProductImage> getProductImages(UUID productId, UUID tenantId) {
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        return productImageRepository.findActiveByProductIdOrderBySortOrder(productId);
    }

    /**
     * Get primary image for a product
     */
    public Optional<ProductImage> getPrimaryImage(UUID productId, UUID tenantId) {
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        return productImageRepository.findActivePrimaryByProductId(productId);
    }

    /**
     * Upload and save product image
     */
    @Transactional
    public ProductImage uploadProductImage(
            UUID productId, 
            UUID tenantId, 
            MultipartFile file, 
            String altText, 
            Boolean isPrimary) throws IOException {
        
        // Verify product belongs to tenant
        Optional<Product> productOpt = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        Product product = productOpt.get();
        
        // Validate file
        validateImageFile(file);
        
        // Enforce storage limit
        long currentStorageUsed = getCurrentStorageUsage(tenantId);
        featureFlagService.enforceStorageLimit(tenantId, currentStorageUsed, file.getSize());
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = generateUniqueFilename(productId.toString(), extension);
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Save file to disk
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create ProductImage entity
        ProductImage productImage = new ProductImage();
        productImage.setProductId(productId);
        productImage.setImageUrl("/" + UPLOAD_DIR + "/" + filename);
        productImage.setAltText(altText != null ? altText : product.getName());
        productImage.setIsPrimary(isPrimary != null ? isPrimary : false);
        
        // Set sort order
        Integer maxSortOrder = productImageRepository.findMaxSortOrderByProductId(productId);
        productImage.setSortOrder(maxSortOrder != null ? maxSortOrder + 1 : 1);
        
        // If this is set as primary, clear other primary images
        if (Boolean.TRUE.equals(isPrimary)) {
            productImageRepository.clearPrimaryImageForProduct(productId);
        }
        
        // Save and return
        ProductImage savedImage = productImageRepository.save(productImage);
        
        log.info("Uploaded image for product {}: {}", productId, filename);
        return savedImage;
    }

    /**
     * Update product image details
     */
    @Transactional
    public ProductImage updateProductImage(
            UUID imageId, 
            UUID tenantId, 
            String altText, 
            Boolean isPrimary, 
            Integer sortOrder) {
        
        Optional<ProductImage> imageOpt = productImageRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("Product image not found");
        }
        
        ProductImage productImage = imageOpt.get();
        
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                productImage.getProductId(), tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        // Update fields
        if (altText != null) {
            productImage.setAltText(altText);
        }
        
        if (sortOrder != null) {
            productImage.setSortOrder(sortOrder);
        }
        
        // Handle primary image setting
        if (Boolean.TRUE.equals(isPrimary) && !productImage.getIsPrimary()) {
            productImageRepository.clearPrimaryImageForProduct(productImage.getProductId());
            productImage.setIsPrimary(true);
        } else if (Boolean.FALSE.equals(isPrimary)) {
            productImage.setIsPrimary(false);
        }
        
        return productImageRepository.save(productImage);
    }

    /**
     * Delete product image
     */
    @Transactional
    public void deleteProductImage(UUID imageId, UUID tenantId) {
        Optional<ProductImage> imageOpt = productImageRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("Product image not found");
        }
        
        ProductImage productImage = imageOpt.get();
        
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                productImage.getProductId(), tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        // Delete file from disk
        try {
            String imageUrl = productImage.getImageUrl();
            if (imageUrl.startsWith("/")) {
                imageUrl = imageUrl.substring(1); // Remove leading slash
            }
            Path filePath = Paths.get(imageUrl);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete image file: {}", productImage.getImageUrl(), e);
        }
        
        // Delete from database
        productImageRepository.delete(productImage);
        
        log.info("Deleted image {} for product {}", imageId, productImage.getProductId());
    }

    /**
     * Set primary image for a product
     */
    @Transactional
    public ProductImage setPrimaryImage(UUID imageId, UUID tenantId) {
        Optional<ProductImage> imageOpt = productImageRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("Product image not found");
        }
        
        ProductImage productImage = imageOpt.get();
        
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                productImage.getProductId(), tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        // Clear other primary images and set this one as primary
        productImageRepository.clearPrimaryImageForProduct(productImage.getProductId());
        productImage.setIsPrimary(true);
        
        return productImageRepository.save(productImage);
    }

    /**
     * Reorder product images
     */
    @Transactional
    public List<ProductImage> reorderProductImages(UUID productId, UUID tenantId, List<UUID> imageIds) {
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        // Get all images for the product
        List<ProductImage> images = productImageRepository.findActiveByProductIdOrderBySortOrder(productId);
        Map<UUID, ProductImage> imageMap = images.stream()
                .collect(Collectors.toMap(ProductImage::getId, img -> img));
        
        // Update sort orders based on the provided order
        List<ProductImage> reorderedImages = new ArrayList<>();
        for (int i = 0; i < imageIds.size(); i++) {
            UUID imageId = imageIds.get(i);
            ProductImage image = imageMap.get(imageId);
            if (image != null) {
                image.setSortOrder(i + 1);
                reorderedImages.add(productImageRepository.save(image));
            }
        }
        
        return reorderedImages;
    }

    /**
     * Delete all images for a product
     */
    @Transactional
    public void deleteAllProductImages(UUID productId, UUID tenantId) {
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        List<ProductImage> images = productImageRepository.findActiveByProductIdOrderBySortOrder(productId);
        
        // Delete files from disk
        for (ProductImage image : images) {
            try {
                String imageUrl = image.getImageUrl();
                if (imageUrl.startsWith("/")) {
                    imageUrl = imageUrl.substring(1);
                }
                Path filePath = Paths.get(imageUrl);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete image file: {}", image.getImageUrl(), e);
            }
        }
        
        // Delete from database
        productImageRepository.deleteByProductId(productId);
        
        log.info("Deleted all images for product {}", productId);
    }

    /**
     * Get image count for a product
     */
    public long getImageCount(UUID productId, UUID tenantId) {
        // Verify product belongs to tenant
        Optional<Product> product = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);
        if (product.isEmpty()) {
            throw new RuntimeException("Product not found or access denied");
        }
        
        return productImageRepository.countByProductId(productId);
    }
    
    /**
     * Calculate current storage usage for a tenant
     */
    private long getCurrentStorageUsage(UUID tenantId) {
        // Get all products for the tenant
        List<Product> products = productRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        
        long totalStorage = 0;
        for (Product product : products) {
            // Get all images for each product
            List<ProductImage> images = productImageRepository.findActiveByProductIdOrderBySortOrder(product.getId());
            
            for (ProductImage image : images) {
                try {
                    String imageUrl = image.getImageUrl();
                    if (imageUrl.startsWith("/")) {
                        imageUrl = imageUrl.substring(1);
                    }
                    Path filePath = Paths.get(imageUrl);
                    if (Files.exists(filePath)) {
                        totalStorage += Files.size(filePath);
                    }
                } catch (IOException e) {
                    log.warn("Failed to get file size for image: {}", image.getImageUrl(), e);
                }
            }
        }
        
        return totalStorage;
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_MIME_TYPES));
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is required");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension. Allowed extensions: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private String generateUniqueFilename(String productId, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        return String.format("product_%s_%s_%s.%s", productId, timestamp, random, extension);
    }
}
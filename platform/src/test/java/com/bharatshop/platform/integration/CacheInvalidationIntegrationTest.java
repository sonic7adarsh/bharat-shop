package com.bharatshop.platform.integration;

import com.bharatshop.platform.service.CategoryService;
import com.bharatshop.platform.service.PlatformProductService;
import com.bharatshop.platform.service.ProductImageService;
import com.bharatshop.shared.dto.ProductRequestDto;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.ProductImage;
import com.bharatshop.shared.service.CacheService;
import com.bharatshop.shared.service.PageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

/**
 * Integration tests for cache invalidation functionality
 * Tests that write operations properly invalidate related caches
 */
@Transactional
class CacheInvalidationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PlatformProductService platformProductService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductImageService productImageService;

    @Autowired
    private PageService pageService;

    @SpyBean
    private CacheService cacheService;

    private Long tenantId;
    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        
        // Create test product
        ProductRequestDto productRequest = ProductRequestDto.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .sku("TEST-SKU-001")
                .isActive(true)
                .build();
        
        testProduct = platformProductService.createProduct(productRequest, tenantId);
        
        // Create test category
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test Category Description");
        testCategory.setIsActive(true);
        testCategory.setSortOrder(1);
        testCategory = categoryService.createCategory(testCategory, tenantId);
        
        // Reset spy to clear setup calls
        reset(cacheService);
    }

    @Test
    @DisplayName("Should invalidate product caches when creating product")
    void shouldInvalidateProductCachesWhenCreatingProduct() {
        // Given
        ProductRequestDto newProductRequest = ProductRequestDto.builder()
                .name("New Test Product")
                .description("New Test Description")
                .price(BigDecimal.valueOf(149.99))
                .sku("NEW-TEST-SKU-001")
                .isActive(true)
                .build();
        
        // When
        platformProductService.createProduct(newProductRequest, tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateProductCaches();
    }

    @Test
    @DisplayName("Should invalidate product caches when updating product")
    void shouldInvalidateProductCachesWhenUpdatingProduct() {
        // Given
        ProductRequestDto updateRequest = ProductRequestDto.builder()
                .name("Updated Product Name")
                .description("Updated Description")
                .price(BigDecimal.valueOf(199.99))
                .sku(testProduct.getSku())
                .isActive(false)
                .build();
        
        // When
        platformProductService.updateProduct(testProduct.getId(), updateRequest, tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateProductCaches();
    }

    @Test
    @DisplayName("Should invalidate product caches when deleting product")
    void shouldInvalidateProductCachesWhenDeletingProduct() {
        // When
        platformProductService.deleteProduct(testProduct.getId(), tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateProductCaches();
    }

    @Test
    @DisplayName("Should invalidate category caches when creating category")
    void shouldInvalidateCategoryCachesWhenCreatingCategory() {
        // Given
        Category newCategory = new Category();
        newCategory.setName("New Test Category");
        newCategory.setDescription("New Test Category Description");
        newCategory.setIsActive(true);
        newCategory.setSortOrder(2);
        
        // When
        categoryService.createCategory(newCategory, tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateCategoryCaches();
    }

    @Test
    @DisplayName("Should invalidate category caches when updating category")
    void shouldInvalidateCategoryCachesWhenUpdatingCategory() {
        // Given
        Category updateCategory = new Category();
        updateCategory.setName("Updated Category Name");
        updateCategory.setDescription("Updated Category Description");
        updateCategory.setIsActive(false);
        
        // When
        categoryService.updateCategory(testCategory.getId(), updateCategory, tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateCategoryCaches();
    }

    @Test
    @DisplayName("Should invalidate category caches when deleting category")
    void shouldInvalidateCategoryCachesWhenDeletingCategory() {
        // When
        categoryService.deleteCategory(testCategory.getId(), tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateCategoryCaches();
    }

    @Test
    @DisplayName("Should invalidate category caches when updating category status")
    void shouldInvalidateCategoryCachesWhenUpdatingCategoryStatus() {
        // When
        categoryService.updateCategoryStatus(testCategory.getId(), false, tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateCategoryCaches();
    }

    @Test
    @DisplayName("Should invalidate product and image caches when uploading product image")
    void shouldInvalidateProductAndImageCachesWhenUploadingProductImage() throws Exception {
        // Given
        byte[] imageContent = createTestImageContent();
        MultipartFile mockFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                imageContent
        );
        
        // When
        productImageService.uploadProductImage(testProduct.getId(), mockFile, tenantId);
        
        // Then
        verify(cacheService, times(1)).invalidateProductCaches();
        verify(cacheService, times(1)).invalidateImageCaches();
    }

    @Test
    @DisplayName("Should handle multiple cache invalidations in transaction")
    void shouldHandleMultipleCacheInvalidationsInTransaction() {
        // Given
        ProductRequestDto productRequest1 = ProductRequestDto.builder()
                .name("Product 1")
                .description("Description 1")
                .price(BigDecimal.valueOf(99.99))
                .sku("SKU-001")
                .isActive(true)
                .build();
        
        ProductRequestDto productRequest2 = ProductRequestDto.builder()
                .name("Product 2")
                .description("Description 2")
                .price(BigDecimal.valueOf(149.99))
                .sku("SKU-002")
                .isActive(true)
                .build();
        
        Category category1 = new Category();
        category1.setName("Category 1");
        category1.setDescription("Category 1 Description");
        category1.setIsActive(true);
        category1.setSortOrder(1);
        
        Category category2 = new Category();
        category2.setName("Category 2");
        category2.setDescription("Category 2 Description");
        category2.setIsActive(true);
        category2.setSortOrder(2);
        
        // When
        platformProductService.createProduct(productRequest1, tenantId);
        platformProductService.createProduct(productRequest2, tenantId);
        categoryService.createCategory(category1, tenantId);
        categoryService.createCategory(category2, tenantId);
        
        // Then
        verify(cacheService, times(2)).invalidateProductCaches();
        verify(cacheService, times(2)).invalidateCategoryCaches();
    }

    @Test
    @DisplayName("Should not fail when cache service throws exception")
    void shouldNotFailWhenCacheServiceThrowsException() {
        // Given
        doThrow(new RuntimeException("Cache error")).when(cacheService).invalidateProductCaches();
        
        ProductRequestDto productRequest = ProductRequestDto.builder()
                .name("Test Product with Cache Error")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .sku("ERROR-SKU-001")
                .isActive(true)
                .build();
        
        // When & Then - should not throw exception
        Product createdProduct = platformProductService.createProduct(productRequest, tenantId);
        
        // Verify product was still created despite cache error
        assert createdProduct != null;
        assert createdProduct.getName().equals("Test Product with Cache Error");
    }

    private byte[] createTestImageContent() throws Exception {
        // Create a minimal JPEG image content for testing
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG header
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, // JFIF
                0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00, // Resolution
                (byte) 0xFF, (byte) 0xD9 // JPEG end
        };
    }
}
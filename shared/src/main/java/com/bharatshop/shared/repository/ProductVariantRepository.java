package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends TenantAwareRepository<ProductVariant> {
    
    /**
     * Find all active variants by product ID for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    List<ProductVariant> findActiveByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all active variants by product ID and tenant ID with pagination
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    Page<ProductVariant> findActiveByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId, Pageable pageable);
    
    /**
     * Find active variant by ID and tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findActiveByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    
    /**
     * Find default variant by product ID and tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.isDefault = true AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findDefaultByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all variants by product ID for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    List<ProductVariant> findByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find default variant by product ID for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.isDefault = true AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findDefaultByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variant by SKU for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.sku = :sku AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findBySku(@Param("sku") String sku, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variant by barcode for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findByBarcode(@Param("barcode") String barcode, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variants by product IDs for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId IN :productIds AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.productId ASC, pv.sortOrder ASC")
    List<ProductVariant> findActiveByProductIds(@Param("productIds") List<UUID> productIds, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variants by price range for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.price BETWEEN :minPrice AND :maxPrice AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.price ASC")
    List<ProductVariant> findActiveByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variants by price range and tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.price BETWEEN :minPrice AND :maxPrice AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.price ASC")
    List<ProductVariant> findActiveByPriceRangeAndTenantId(@Param("tenantId") UUID tenantId, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * Find variants with low stock for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock <= :threshold AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.stock ASC")
    List<ProductVariant> findActiveWithLowStock(@Param("threshold") Integer threshold, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variants with low stock by tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock <= :threshold AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.stock ASC")
    List<ProductVariant> findActiveWithLowStockByTenantId(@Param("tenantId") UUID tenantId, @Param("threshold") Integer threshold);
    
    /**
     * Find out of stock variants for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock = 0 AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.updatedAt DESC")
    List<ProductVariant> findActiveOutOfStock(@Param("tenantId") UUID tenantId);
    
    /**
     * Find out of stock variants by tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock = 0 AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.updatedAt DESC")
    List<ProductVariant> findActiveWithZeroStockByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Search variants by SKU or barcode for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL AND (LOWER(pv.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pv.barcode) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY pv.sku ASC")
    List<ProductVariant> searchActiveBySku(@Param("search") String search, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if SKU exists for current tenant (excluding current variant)
     */
    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.sku = :sku AND pv.tenantId = :tenantId AND pv.id != :excludeId AND pv.deletedAt IS NULL")
    boolean existsBySkuAndTenantIdAndIdNot(@Param("sku") String sku, @Param("excludeId") UUID excludeId, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if SKU exists for tenant
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM ProductVariant pv WHERE pv.sku = :sku AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    boolean existsBySkuAndTenantId(@Param("sku") String sku, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if barcode exists for current tenant (excluding current variant)
     */
    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.tenantId = :tenantId AND pv.id != :excludeId AND pv.deletedAt IS NULL")
    boolean existsByBarcodeAndTenantIdAndIdNot(@Param("barcode") String barcode, @Param("excludeId") UUID excludeId, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if barcode exists for tenant
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    boolean existsByBarcodeAndTenantId(@Param("barcode") String barcode, @Param("tenantId") UUID tenantId);
    
    /**
     * Count variants by product ID
     */
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    long countByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active variants by product ID
     */
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    long countActiveByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get total stock by product ID
     */
    @Query("SELECT COALESCE(SUM(pv.stock), 0) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Integer getTotalStockByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get minimum price by product ID
     */
    @Query("SELECT MIN(pv.price) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    BigDecimal getMinPriceByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get maximum price by product ID
     */
    @Query("SELECT MAX(pv.price) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    BigDecimal getMaxPriceByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Delete all variants by product ID (soft delete)
     */
    @Query("UPDATE ProductVariant pv SET pv.deletedAt = CURRENT_TIMESTAMP WHERE pv.productId = :productId AND pv.tenantId = :tenantId")
    void deleteByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variants with pagination
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Page<ProductVariant> findAllActive(Pageable pageable, @Param("tenantId") UUID tenantId);
    
    /**
     * Update stock for variant
     */
    @Query("UPDATE ProductVariant pv SET pv.stock = :stock, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.id = :id AND pv.tenantId = :tenantId")
    void updateStock(@Param("id") UUID id, @Param("stock") Integer stock, @Param("tenantId") UUID tenantId);
    
    /**
     * Increment stock quantity
     */
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock + :quantity, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.id = :id AND pv.tenantId = :tenantId")
    int incrementStock(@Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("quantity") Integer quantity);
    
    /**
     * Decrement stock quantity
     */
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :quantity, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.id = :id AND pv.tenantId = :tenantId AND pv.stock >= :quantity")
    int decrementStock(@Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("quantity") Integer quantity);
    
    /**
     * Clear default variant for product
     */
    @Query("UPDATE ProductVariant pv SET pv.isDefault = false, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.productId = :productId AND pv.tenantId = :tenantId")
    void clearDefaultForProduct(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active variants by product ID and tenant ID
     */
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    long countActiveByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active variants by tenant ID
     */
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Get total stock by product ID and tenant ID
     */
    @Query("SELECT COALESCE(SUM(pv.stock), 0) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Integer getTotalStockByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get minimum price by product ID and tenant ID
     */
    @Query("SELECT MIN(pv.price) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    BigDecimal getMinPriceByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get maximum price by product ID and tenant ID
     */
    @Query("SELECT MAX(pv.price) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    BigDecimal getMaxPriceByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all active variants by product ID and tenant ID (non-pageable)
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    List<ProductVariant> findActiveByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active variant by SKU and tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.sku = :sku AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findActiveBySkuAndTenantId(@Param("sku") String sku, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active variant by barcode and tenant ID
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.status = 'ACTIVE' AND pv.tenantId = :tenantId AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findActiveByBarcodeAndTenantId(@Param("barcode") String barcode, @Param("tenantId") UUID tenantId);
    
    /**
     * Soft delete all variants by product ID and tenant ID
     */
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.deletedAt = CURRENT_TIMESTAMP WHERE pv.productId = :productId AND pv.tenantId = :tenantId")
    void softDeleteByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variant by ID with pessimistic lock for atomic operations
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findByIdWithLock(@Param("id") UUID id);
}
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


@Repository
public interface ProductVariantRepository extends TenantAwareRepository<ProductVariant> {
    
    /**
     * Find all active variants by product ID for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductVariant> findActiveByProductId(Long productId, Long tenantId);
    
    /**
     * Find all active variants by product ID and tenant ID with pagination
     */
    @Query(value = "SELECT * FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    Page<ProductVariant> findActiveByProductIdAndTenantId(Long productId, Long tenantId, Pageable pageable);
    
    /**
     * Find active variant by ID and tenant ID
     */
    Optional<ProductVariant> findByIdAndStatusAndTenantIdAndDeletedAtIsNull(Long id, String status, Long tenantId);
    
    /**
     * Find active variant by ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_variants WHERE id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    Optional<ProductVariant> findActiveByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find default variant by product ID and tenant ID
     */
    Optional<ProductVariant> findByProductIdAndIsDefaultTrueAndTenantIdAndDeletedAtIsNull(Long productId, Long tenantId);
    
    /**
     * Find all variants by product ID for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE product_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductVariant> findByProductId(Long productId, Long tenantId);
    

    
    /**
     * Find default variant by product ID and tenant ID (alias method)
     */
    default Optional<ProductVariant> findDefaultByProductIdAndTenantId(Long productId, Long tenantId) {
        return findByProductIdAndIsDefaultTrueAndTenantIdAndDeletedAtIsNull(productId, tenantId);
    }
    
    /**
     * Find variant by SKU for current tenant
     */
    Optional<ProductVariant> findBySkuAndTenantIdAndDeletedAtIsNull(String sku, Long tenantId);
    
    /**
     * Find variant by barcode for current tenant
     */
    Optional<ProductVariant> findByBarcodeAndTenantIdAndDeletedAtIsNull(String barcode, Long tenantId);
    
    /**
     * Find variants by product IDs for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE product_id IN (?1) AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY product_id ASC, sort_order ASC", nativeQuery = true)
    List<ProductVariant> findActiveByProductIds(List<Long> productIds, Long tenantId);
    
    /**
     * Find variants by price range for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE price BETWEEN ?1 AND ?2 AND status = 'ACTIVE' AND tenant_id = ?3 AND deleted_at IS NULL ORDER BY price ASC", nativeQuery = true)
    List<ProductVariant> findActiveByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Long tenantId);
    
    /**
     * Find variants by price range and tenant ID
     */
    @Query(value = "SELECT * FROM product_variants WHERE price BETWEEN ?2 AND ?3 AND status = 'ACTIVE' AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY price ASC", nativeQuery = true)
    List<ProductVariant> findActiveByPriceRangeAndTenantId(Long tenantId, BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Find variants with low stock for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE stock <= ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY stock ASC", nativeQuery = true)
    List<ProductVariant> findActiveWithLowStock(Integer threshold, Long tenantId);
    
    /**
     * Find variants with low stock by tenant ID
     */
    @Query(value = "SELECT * FROM product_variants WHERE stock <= ?2 AND status = 'ACTIVE' AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY stock ASC", nativeQuery = true)
    List<ProductVariant> findActiveWithLowStockByTenantId(Long tenantId, Integer threshold);
    
    /**
     * Find out of stock variants for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE stock = 0 AND status = 'ACTIVE' AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY updated_at DESC", nativeQuery = true)
    List<ProductVariant> findActiveOutOfStock(Long tenantId);
    
    /**
     * Find out of stock variants by tenant ID
     */
    @Query(value = "SELECT * FROM product_variants WHERE stock = 0 AND status = 'ACTIVE' AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY updated_at DESC", nativeQuery = true)
    List<ProductVariant> findActiveWithZeroStockByTenantId(Long tenantId);
    
    /**
     * Search variants by SKU or barcode for current tenant
     */
    @Query(value = "SELECT * FROM product_variants WHERE status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL AND (LOWER(sku) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(barcode) LIKE LOWER(CONCAT('%', ?1, '%'))) ORDER BY sku ASC", nativeQuery = true)
    List<ProductVariant> searchActiveBySku(String search, Long tenantId);
    
    /**
     * Check if SKU exists for current tenant (excluding current variant)
     */
    boolean existsBySkuAndTenantIdAndIdNotAndDeletedAtIsNull(String sku, Long tenantId, Long excludeId);
    
    /**
     * Check if SKU exists for tenant
     */
    boolean existsBySkuAndTenantIdAndDeletedAtIsNull(String sku, Long tenantId);
    
    /**
     * Check if barcode exists for current tenant (excluding current variant)
     */
    boolean existsByBarcodeAndTenantIdAndIdNotAndDeletedAtIsNull(String barcode, Long tenantId, Long excludeId);
    
    /**
     * Check if barcode exists for tenant
     */
    boolean existsByBarcodeAndTenantIdAndDeletedAtIsNull(String barcode, Long tenantId);
    
    /**
     * Count variants by product ID
     */
    long countByProductIdAndTenantIdAndDeletedAtIsNull(Long productId, Long tenantId);
    
    /**
     * Count active variants by product ID
     */
    long countByProductIdAndStatusAndTenantIdAndDeletedAtIsNull(Long productId, String status, Long tenantId);
    
    /**
     * Get total stock by product ID
     */
    @Query(value = "SELECT COALESCE(SUM(stock), 0) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    Integer getTotalStockByProductId(Long productId, Long tenantId);
    
    /**
     * Get minimum price by product ID
     */
    @Query(value = "SELECT MIN(price) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    BigDecimal getMinPriceByProductId(Long productId, Long tenantId);
    
    /**
     * Get maximum price by product ID
     */
    @Query(value = "SELECT MAX(price) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    BigDecimal getMaxPriceByProductId(Long productId, Long tenantId);
    
    /**
     * Delete all variants by product ID (soft delete)
     */
    @Modifying
    @Query(value = "UPDATE product_variants SET deleted_at = CURRENT_TIMESTAMP WHERE product_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void deleteByProductId(Long productId, Long tenantId);
    
    /**
     * Find variants with pagination
     */
    @Query(value = "SELECT * FROM product_variants WHERE status = 'ACTIVE' AND tenant_id = ?1 AND deleted_at IS NULL", nativeQuery = true)
    Page<ProductVariant> findAllActive(Pageable pageable, Long tenantId);
    
    /**
     * Update stock for variant
     */
    @Modifying
    @Query(value = "UPDATE product_variants SET stock = ?2, updated_at = CURRENT_TIMESTAMP WHERE id = ?1 AND tenant_id = ?3", nativeQuery = true)
    void updateStock(Long id, Integer stock, Long tenantId);
    
    /**
     * Increment stock quantity
     */
    @Modifying
    @Query(value = "UPDATE product_variants SET stock = stock + ?3, updated_at = CURRENT_TIMESTAMP WHERE id = ?1 AND tenant_id = ?2", nativeQuery = true)
    int incrementStock(Long id, Long tenantId, Integer quantity);
    
    /**
     * Decrement stock quantity
     */
    @Modifying
    @Query(value = "UPDATE product_variants SET stock = stock - ?3, updated_at = CURRENT_TIMESTAMP WHERE id = ?1 AND tenant_id = ?2 AND stock >= ?3", nativeQuery = true)
    int decrementStock(Long id, Long tenantId, Integer quantity);
    
    /**
     * Clear default variant for product
     */
    @Modifying
    @Query(value = "UPDATE product_variants SET is_default = false, updated_at = CURRENT_TIMESTAMP WHERE product_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void clearDefaultForProduct(Long productId, Long tenantId);
    

    
    /**
     * Count active variants by tenant ID
     */
    long countByStatusAndTenantIdAndDeletedAtIsNull(String status, Long tenantId);
    
    /**
     * Get total stock by product ID and tenant ID
     */
    @Query(value = "SELECT COALESCE(SUM(stock), 0) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    Integer getTotalStockByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Get minimum price by product ID and tenant ID
     */
    @Query(value = "SELECT MIN(price) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    BigDecimal getMinPriceByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Get maximum price by product ID and tenant ID
     */
    @Query(value = "SELECT MAX(price) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    BigDecimal getMaxPriceByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find all active variants by product ID and tenant ID (non-pageable)
     */
    @Query(value = "SELECT * FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductVariant> findActiveByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find active variant by SKU and tenant ID
     */
    Optional<ProductVariant> findBySkuAndStatusAndTenantIdAndDeletedAtIsNull(String sku, String status, Long tenantId);
    
    /**
     * Find active variant by barcode and tenant ID
     */
    Optional<ProductVariant> findByBarcodeAndStatusAndTenantIdAndDeletedAtIsNull(String barcode, String status, Long tenantId);
    
    /**
     * Soft delete all variants by product ID and tenant ID
     */
    @Modifying
    @Query(value = "UPDATE product_variants SET deleted_at = CURRENT_TIMESTAMP WHERE product_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void softDeleteByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find variant by ID with pessimistic lock for atomic operations
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductVariant> findByIdAndDeletedAtIsNull(Long id);
    

    
    /**
     * Count active variants by tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM product_variants WHERE tenant_id = ?1 AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByTenantId(Long tenantId);
    
    /**
     * Count active variants by product ID and tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM product_variants WHERE product_id = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find active variant by SKU and tenant ID
     */
    @Query(value = "SELECT * FROM product_variants WHERE sku = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    Optional<ProductVariant> findActiveBySkuAndTenantId(String sku, Long tenantId);
    
    /**
     * Find active variant by barcode and tenant ID
     */
    @Query(value = "SELECT * FROM product_variants WHERE barcode = ?1 AND status = 'ACTIVE' AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    Optional<ProductVariant> findActiveByBarcodeAndTenantId(String barcode, Long tenantId);
    
    /**
     * Check if SKU exists for tenant
     */
    boolean existsBySkuAndTenantId(String sku, Long tenantId);
    
    /**
     * Check if barcode exists for tenant
     */
    boolean existsByBarcodeAndTenantId(String barcode, Long tenantId);
}
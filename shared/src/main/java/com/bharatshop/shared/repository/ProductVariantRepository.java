package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends TenantAwareRepository<ProductVariant> {
    
    /**
     * Find all active variants by product ID for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    List<ProductVariant> findActiveByProductId(@Param("productId") UUID productId);
    
    /**
     * Find all variants by product ID for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    List<ProductVariant> findByProductId(@Param("productId") UUID productId);
    
    /**
     * Find default variant by product ID for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId = :productId AND pv.isDefault = true AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findDefaultByProductId(@Param("productId") UUID productId);
    
    /**
     * Find variant by SKU for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.sku = :sku AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findBySku(@Param("sku") String sku);
    
    /**
     * Find variant by barcode for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    Optional<ProductVariant> findByBarcode(@Param("barcode") String barcode);
    
    /**
     * Find variants by product IDs for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.productId IN :productIds AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL ORDER BY pv.productId ASC, pv.sortOrder ASC")
    List<ProductVariant> findActiveByProductIds(@Param("productIds") List<UUID> productIds);
    
    /**
     * Find variants by price range for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.price BETWEEN :minPrice AND :maxPrice AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL ORDER BY pv.price ASC")
    List<ProductVariant> findActiveByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * Find variants with low stock for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock <= :threshold AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL ORDER BY pv.stock ASC")
    List<ProductVariant> findActiveWithLowStock(@Param("threshold") Integer threshold);
    
    /**
     * Find out of stock variants for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stock = 0 AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL ORDER BY pv.updatedAt DESC")
    List<ProductVariant> findActiveOutOfStock();
    
    /**
     * Search variants by SKU or barcode for current tenant
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL AND (LOWER(pv.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pv.barcode) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY pv.sku ASC")
    List<ProductVariant> searchActiveBySku(@Param("search") String search);
    
    /**
     * Check if SKU exists for current tenant (excluding current variant)
     */
    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.sku = :sku AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.id != :excludeId AND pv.deletedAt IS NULL")
    boolean existsBySkuAndTenantIdAndIdNot(@Param("sku") String sku, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if SKU exists for current tenant
     */
    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.sku = :sku AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    boolean existsBySkuAndTenantId(@Param("sku") String sku);
    
    /**
     * Check if barcode exists for current tenant (excluding current variant)
     */
    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.id != :excludeId AND pv.deletedAt IS NULL")
    boolean existsByBarcodeAndTenantIdAndIdNot(@Param("barcode") String barcode, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if barcode exists for current tenant
     */
    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.barcode = :barcode AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    boolean existsByBarcodeAndTenantId(@Param("barcode") String barcode);
    
    /**
     * Count variants by product ID
     */
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    long countByProductId(@Param("productId") UUID productId);
    
    /**
     * Count active variants by product ID
     */
    @Query("SELECT COUNT(pv) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    long countActiveByProductId(@Param("productId") UUID productId);
    
    /**
     * Get total stock by product ID
     */
    @Query("SELECT COALESCE(SUM(pv.stock), 0) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    Integer getTotalStockByProductId(@Param("productId") UUID productId);
    
    /**
     * Get minimum price by product ID
     */
    @Query("SELECT MIN(pv.price) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    BigDecimal getMinPriceByProductId(@Param("productId") UUID productId);
    
    /**
     * Get maximum price by product ID
     */
    @Query("SELECT MAX(pv.price) FROM ProductVariant pv WHERE pv.productId = :productId AND pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    BigDecimal getMaxPriceByProductId(@Param("productId") UUID productId);
    
    /**
     * Delete all variants by product ID (soft delete)
     */
    @Query("UPDATE ProductVariant pv SET pv.deletedAt = CURRENT_TIMESTAMP WHERE pv.productId = :productId AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByProductId(@Param("productId") UUID productId);
    
    /**
     * Find variants with pagination
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.status = 'ACTIVE' AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pv.deletedAt IS NULL")
    Page<ProductVariant> findAllActive(Pageable pageable);
    
    /**
     * Update stock for variant
     */
    @Query("UPDATE ProductVariant pv SET pv.stock = :stock, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.id = :id AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void updateStock(@Param("id") UUID id, @Param("stock") Integer stock);
    
    /**
     * Increment stock for variant
     */
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock + :quantity, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.id = :id AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void incrementStock(@Param("id") UUID id, @Param("quantity") Integer quantity);
    
    /**
     * Decrement stock for variant
     */
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :quantity, pv.updatedAt = CURRENT_TIMESTAMP WHERE pv.id = :id AND pv.stock >= :quantity AND pv.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    int decrementStock(@Param("id") UUID id, @Param("quantity") Integer quantity);
}
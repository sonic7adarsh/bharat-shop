package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProductOptionRepository extends TenantAwareRepository<ProductOption> {
    
    /**
     * Find all product options by product ID for current tenant
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findByProductId(Long productId, Long tenantId);
    
    /**
     * Find product option by product ID and option ID for current tenant
     */
    Optional<ProductOption> findByProductIdAndOptionIdAndTenantIdAndDeletedAtIsNull(Long productId, Long optionId, Long tenantId);
    
    /**
     * Find all product options by option ID for current tenant
     */
    @Query(value = "SELECT * FROM product_options WHERE option_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findByOptionId(Long optionId, Long tenantId);
    
    /**
     * Find required product options by product ID for current tenant
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id = ?1 AND is_required = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findRequiredByProductId(Long productId, Long tenantId);
    
    /**
     * Find product options by product IDs for current tenant
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id IN ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY product_id ASC, sort_order ASC", nativeQuery = true)
    List<ProductOption> findByProductIds(List<Long> productIds, Long tenantId);
    
    /**
     * Check if product option exists
     */
    boolean existsByProductIdAndOptionIdAndTenantIdAndDeletedAtIsNull(Long productId, Long optionId, Long tenantId);
    
    /**
     * Find product IDs by option ID and tenant ID
     */
    @Query(value = "SELECT DISTINCT product_id FROM product_options WHERE option_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    List<Long> findProductIdsByOptionIdAndTenantId(Long optionId, Long tenantId);
    
    /**
     * Count active product options by product ID and tenant ID
     */
    long countByProductIdAndTenantIdAndDeletedAtIsNull(Long productId, Long tenantId);
    
    /**
     * Count required product options by product ID
     */
    long countByProductIdAndIsRequiredTrueAndTenantIdAndDeletedAtIsNull(Long productId, Long tenantId);
    
    /**
     * Delete all product options by product ID (soft delete)
     */
    @Query(value = "UPDATE product_options SET deleted_at = CURRENT_TIMESTAMP WHERE product_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void deleteByProductId(Long productId, Long tenantId);
    
    /**
     * Delete all product options by option ID (soft delete)
     */
    @Query(value = "UPDATE product_options SET deleted_at = CURRENT_TIMESTAMP WHERE option_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void deleteByOptionId(Long optionId, Long tenantId);
    
    /**
     * Find products that use a specific option
     */
    @Query(value = "SELECT DISTINCT product_id FROM product_options WHERE option_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    List<Long> findProductIdsByOptionId(Long optionId, Long tenantId);
    
    /**
     * Soft delete by option ID and tenant ID
     */
    @Modifying
    @Query(value = "UPDATE product_options SET deleted_at = CURRENT_TIMESTAMP WHERE option_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void softDeleteByOptionIdAndTenantId(Long optionId, Long tenantId);
    
    /**
     * Count active options by option ID and tenant ID
     */
    long countByOptionIdAndTenantIdAndDeletedAtIsNull(Long optionId, Long tenantId);
    
    /**
     * Find active options by multiple product IDs and tenant ID
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id IN ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findActiveByProductIdsAndTenantId(List<Long> productIds, Long tenantId);
    
    /**
     * Soft delete by product ID and tenant ID
     */
    @Modifying
    @Query(value = "UPDATE product_options SET deleted_at = CURRENT_TIMESTAMP WHERE product_id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void softDeleteByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find active required options by product ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id = ?1 AND is_required = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findActiveRequiredByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find active options by option ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_options WHERE option_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findActiveByOptionIdAndTenantId(Long optionId, Long tenantId);
    
    /**
     * Find active options by product ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductOption> findActiveByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Find product options by product ID with pagination for current tenant
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    Page<ProductOption> findActiveByProductIdAndTenantId(Long productId, Long tenantId, Pageable pageable);
    
    /**
     * Count active required product options by product ID for current tenant
     */
    @Query(value = "SELECT COUNT(*) FROM product_options WHERE product_id = ?1 AND is_required = true AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    long countActiveRequiredByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Check if product option exists by product ID, option ID and tenant ID
     */
    boolean existsByProductIdAndOptionIdAndTenantId(Long productId, Long optionId, Long tenantId);
    
    /**
     * Count active product options by product ID and tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM product_options WHERE product_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByProductIdAndTenantId(Long productId, Long tenantId);
    
    /**
     * Count active product options by option ID and tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM product_options WHERE option_id = ?1 AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByOptionIdAndTenantId(Long optionId, Long tenantId);
    
    /**
     * Find active product option by product ID, option ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_options WHERE product_id = ?1 AND option_id = ?2 AND tenant_id = ?3 AND deleted_at IS NULL", nativeQuery = true)
    Optional<ProductOption> findActiveByProductIdAndOptionIdAndTenantId(Long productId, Long optionId, Long tenantId);
}
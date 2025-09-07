package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductVariantOptionValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantOptionValueRepository extends TenantAwareRepository<ProductVariantOptionValue> {
    
    /**
     * Find all option values by variant ID for current tenant
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.option_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByVariantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by multiple variant IDs for current tenant
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id IN :variantIds AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.variant_id ASC, pvov.option_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByVariantIds(@Param("variantIds") List<Long> variantIds, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by option ID for current tenant
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.variant_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByOptionId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by option value ID for current tenant
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.option_value_id = :optionValueId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.variant_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByOptionValueId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Find option value by variant ID and option ID for current tenant
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<ProductVariantOptionValue> findByVariantIdAndOptionId(@Param("variantId") Long variantId, @Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Find variants by option value combinations for current tenant
     * This is used to find variants that match specific option value combinations
     */
    @Query(value = "SELECT DISTINCT pvov.variant_id FROM product_variant_option_values pvov WHERE pvov.option_value_id IN :optionValueIds AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL GROUP BY pvov.variant_id HAVING COUNT(DISTINCT pvov.option_value_id) = :optionValueCount", nativeQuery = true)
    List<Long> findVariantIdsByOptionValueCombination(@Param("optionValueIds") List<Long> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("tenantId") Long tenantId);
    
    /**
     * Check if variant has specific option value combination for current tenant
     */
    @Query(value = "SELECT COUNT(*) FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.option_value_id IN :optionValueIds AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countByVariantIdAndOptionValueIds(@Param("variantId") Long variantId, @Param("optionValueIds") List<Long> optionValueIds, @Param("tenantId") Long tenantId);
    
    /**
     * Check if option value combination exists for any variant (excluding current variant) for current tenant
     * This is used to enforce unique variant combinations
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM product_variant_option_values pvov WHERE pvov.option_value_id IN :optionValueIds AND pvov.variant_id != :excludeVariantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL GROUP BY pvov.variant_id HAVING COUNT(DISTINCT pvov.option_value_id) = :optionValueCount", nativeQuery = true)
    boolean existsByOptionValueCombinationAndVariantIdNot(@Param("optionValueIds") List<Long> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("excludeVariantId") Long excludeVariantId, @Param("tenantId") Long tenantId);
    
    /**
     * Check if option value combination exists for any variant for current tenant
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM product_variant_option_values pvov WHERE pvov.option_value_id IN :optionValueIds AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL GROUP BY pvov.variant_id HAVING COUNT(DISTINCT pvov.option_value_id) = :optionValueCount", nativeQuery = true)
    boolean existsByOptionValueCombination(@Param("optionValueIds") List<Long> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("tenantId") Long tenantId);
    
    /**
     * Count option values by variant ID for current tenant
     */
    @Query(value = "SELECT COUNT(*) FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countByVariantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Count variants by option ID for current tenant
     */
    @Query(value = "SELECT COUNT(DISTINCT pvov.variant_id) FROM product_variant_option_values pvov WHERE pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countVariantsByOptionId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Count variants by option value ID for current tenant
     */
    @Query(value = "SELECT COUNT(DISTINCT pvov.variant_id) FROM product_variant_option_values pvov WHERE pvov.option_value_id = :optionValueId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countVariantsByOptionValueId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Get distinct option IDs by variant ID for current tenant
     */
    @Query(value = "SELECT DISTINCT pvov.option_id FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<Long> getOptionIdsByVariantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Get distinct option value IDs by variant ID for current tenant
     */
    @Query(value = "SELECT DISTINCT pvov.option_value_id FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<Long> getOptionValueIdsByVariantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Get distinct variant IDs by option ID for current tenant
     */
    @Query(value = "SELECT DISTINCT pvov.variant_id FROM product_variant_option_values pvov WHERE pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<Long> getVariantIdsByOptionId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Get distinct variant IDs by option value ID for current tenant
     */
    @Query(value = "SELECT DISTINCT pvov.variant_id FROM product_variant_option_values pvov WHERE pvov.option_value_id = :optionValueId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<Long> getVariantIdsByOptionValueId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Delete all option values by variant ID (soft delete)
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE variant_id = :variantId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByVariantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Delete all option values by option ID (soft delete)
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE option_id = :optionId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByOptionId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Delete all option values by option value ID (soft delete)
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE option_value_id = :optionValueId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByOptionValueId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Delete specific option value by variant ID and option ID (soft delete)
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE variant_id = :variantId AND option_id = :optionId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByVariantIdAndOptionId(@Param("variantId") Long variantId, @Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    // Methods with explicit tenant ID parameters
    
    /**
     * Find all option values by variant ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.option_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByVariantIdAndTenantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by variant ID and tenant ID with pagination
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.option_id ASC", nativeQuery = true)
    Page<ProductVariantOptionValue> findByVariantIdAndTenantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId, Pageable pageable);
    
    /**
     * Find option value by variant ID and option ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    Optional<ProductVariantOptionValue> findByVariantIdAndOptionIdAndTenantId(@Param("variantId") Long variantId, @Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by option ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.variant_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by option value ID and tenant ID
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.option_value_id = :optionValueId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.variant_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByOptionValueIdAndTenantId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Find all option values by multiple variant IDs and tenant ID
     */
    @Query(value = "SELECT * FROM product_variant_option_values pvov WHERE pvov.variant_id IN :variantIds AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL ORDER BY pvov.variant_id ASC, pvov.option_id ASC", nativeQuery = true)
    List<ProductVariantOptionValue> findByVariantIdsAndTenantId(@Param("variantIds") List<Long> variantIds, @Param("tenantId") Long tenantId);
    
    /**
     * Count option values by variant ID and tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countByVariantIdAndTenantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
    
    /**
     * Count variants by option ID and tenant ID
     */
    @Query(value = "SELECT COUNT(DISTINCT pvov.variant_id) FROM product_variant_option_values pvov WHERE pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Count variants by option value ID and tenant ID
     */
    @Query(value = "SELECT COUNT(DISTINCT pvov.variant_id) FROM product_variant_option_values pvov WHERE pvov.option_value_id = :optionValueId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    long countByOptionValueIdAndTenantId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Get distinct variant IDs by option ID and tenant ID
     */
    @Query(value = "SELECT DISTINCT pvov.variant_id FROM product_variant_option_values pvov WHERE pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<Long> findVariantIdsByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Get distinct variant IDs by option value ID and tenant ID
     */
    @Query(value = "SELECT DISTINCT pvov.variant_id FROM product_variant_option_values pvov WHERE pvov.option_value_id = :optionValueId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    List<Long> findVariantIdsByOptionValueIdAndTenantId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Check if variant has option and tenant ID
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM product_variant_option_values pvov WHERE pvov.variant_id = :variantId AND pvov.option_id = :optionId AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL", nativeQuery = true)
    boolean existsByVariantIdAndOptionIdAndTenantId(@Param("variantId") Long variantId, @Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Find variant by option values and tenant ID
     */
    @Query(value = "SELECT pvov.variant_id FROM product_variant_option_values pvov " +
           "JOIN product_variants pv ON pvov.variant_id = pv.id " +
           "WHERE pv.product_id = :productId AND pvov.option_value_id IN :optionValueIds " +
           "AND pvov.tenant_id = :tenantId AND pvov.deleted_at IS NULL " +
           "GROUP BY pvov.variant_id " +
           "HAVING COUNT(DISTINCT pvov.option_value_id) = :optionValueCount", nativeQuery = true)
    Optional<Long> findVariantByOptionValues(@Param("productId") Long productId, @Param("optionValueIds") java.util.Collection<Long> optionValueIds, @Param("optionValueCount") int optionValueCount, @Param("tenantId") Long tenantId);
    
    /**
     * Delete by option value ID and tenant ID
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE option_value_id = :optionValueId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByOptionValueIdAndTenantId(@Param("optionValueId") Long optionValueId, @Param("tenantId") Long tenantId);
    
    /**
     * Delete by option ID and tenant ID
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE option_id = :optionId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Delete by variant ID and tenant ID
     */
    @Modifying
    @Query(value = "UPDATE product_variant_option_values SET deleted_at = CURRENT_TIMESTAMP WHERE variant_id = :variantId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByVariantIdAndTenantId(@Param("variantId") Long variantId, @Param("tenantId") Long tenantId);
}
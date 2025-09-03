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
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.optionId ASC")
    List<ProductVariantOptionValue> findByVariantId(@Param("variantId") UUID variantId, @Param("tenantId") String tenantId);
    
    /**
     * Find all option values by multiple variant IDs for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId IN :variantIds AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC, pvov.optionId ASC")
    List<ProductVariantOptionValue> findByVariantIds(@Param("variantIds") List<UUID> variantIds, @Param("tenantId") String tenantId);
    
    /**
     * Find all option values by option ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC")
    List<ProductVariantOptionValue> findByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") String tenantId);
    
    /**
     * Find all option values by option value ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC")
    List<ProductVariantOptionValue> findByOptionValueId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") String tenantId);
    
    /**
     * Find option value by variant ID and option ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<ProductVariantOptionValue> findByVariantIdAndOptionId(@Param("variantId") UUID variantId, @Param("optionId") UUID optionId, @Param("tenantId") String tenantId);
    
    /**
     * Find variants by option value combinations for current tenant
     * This is used to find variants that match specific option value combinations
     */
    @Query("SELECT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId IN :optionValueIds AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL GROUP BY pvov.variantId HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    List<UUID> findVariantIdsByOptionValueCombination(@Param("optionValueIds") List<UUID> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("tenantId") String tenantId);
    
    /**
     * Check if variant has specific option value combination for current tenant
     */
    @Query("SELECT COUNT(pvov) FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.optionValueId IN :optionValueIds AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countByVariantIdAndOptionValueIds(@Param("variantId") UUID variantId, @Param("optionValueIds") List<UUID> optionValueIds, @Param("tenantId") String tenantId);
    
    /**
     * Check if option value combination exists for any variant (excluding current variant) for current tenant
     * This is used to enforce unique variant combinations
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) > 0 FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId IN :optionValueIds AND pvov.variantId != :excludeVariantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL GROUP BY pvov.variantId HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    boolean existsByOptionValueCombinationAndVariantIdNot(@Param("optionValueIds") List<UUID> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("excludeVariantId") UUID excludeVariantId, @Param("tenantId") String tenantId);
    
    /**
     * Check if option value combination exists for any variant for current tenant
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) > 0 FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId IN :optionValueIds AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL GROUP BY pvov.variantId HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    boolean existsByOptionValueCombination(@Param("optionValueIds") List<UUID> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("tenantId") String tenantId);
    
    /**
     * Count option values by variant ID for current tenant
     */
    @Query("SELECT COUNT(pvov) FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countByVariantId(@Param("variantId") UUID variantId, @Param("tenantId") String tenantId);
    
    /**
     * Count variants by option ID for current tenant
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countVariantsByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") String tenantId);
    
    /**
     * Count variants by option value ID for current tenant
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countVariantsByOptionValueId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") String tenantId);
    
    /**
     * Get distinct option IDs by variant ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.optionId FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<UUID> getOptionIdsByVariantId(@Param("variantId") UUID variantId, @Param("tenantId") String tenantId);
    
    /**
     * Get distinct option value IDs by variant ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.optionValueId FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<UUID> getOptionValueIdsByVariantId(@Param("variantId") UUID variantId, @Param("tenantId") String tenantId);
    
    /**
     * Get distinct variant IDs by option ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<UUID> getVariantIdsByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") String tenantId);
    
    /**
     * Get distinct variant IDs by option value ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<UUID> getVariantIdsByOptionValueId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") String tenantId);
    
    /**
     * Delete all option values by variant ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId")
    void deleteByVariantId(@Param("variantId") UUID variantId, @Param("tenantId") String tenantId);
    
    /**
     * Delete all option values by option ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId")
    void deleteByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") String tenantId);
    
    /**
     * Delete all option values by option value ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId")
    void deleteByOptionValueId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") String tenantId);
    
    /**
     * Delete specific option value by variant ID and option ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.variantId = :variantId AND pvov.optionId = :optionId AND pvov.tenantId = :tenantId")
    void deleteByVariantIdAndOptionId(@Param("variantId") UUID variantId, @Param("optionId") UUID optionId, @Param("tenantId") String tenantId);
    
    // Methods with explicit tenant ID parameters
    
    /**
     * Find all option values by variant ID and tenant ID
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.optionId ASC")
    List<ProductVariantOptionValue> findByVariantIdAndTenantId(@Param("variantId") UUID variantId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all option values by variant ID and tenant ID with pagination
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.optionId ASC")
    Page<ProductVariantOptionValue> findByVariantIdAndTenantId(@Param("variantId") UUID variantId, @Param("tenantId") UUID tenantId, Pageable pageable);
    
    /**
     * Find option value by variant ID and option ID and tenant ID
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    Optional<ProductVariantOptionValue> findByVariantIdAndOptionIdAndTenantId(@Param("variantId") UUID variantId, @Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all option values by option ID and tenant ID
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC")
    List<ProductVariantOptionValue> findByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all option values by option value ID and tenant ID
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC")
    List<ProductVariantOptionValue> findByOptionValueIdAndTenantId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all option values by multiple variant IDs and tenant ID
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId IN :variantIds AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC, pvov.optionId ASC")
    List<ProductVariantOptionValue> findByVariantIdsAndTenantId(@Param("variantIds") List<UUID> variantIds, @Param("tenantId") UUID tenantId);
    
    /**
     * Count option values by variant ID and tenant ID
     */
    @Query("SELECT COUNT(pvov) FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countByVariantIdAndTenantId(@Param("variantId") UUID variantId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count variants by option ID and tenant ID
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count variants by option value ID and tenant ID
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    long countByOptionValueIdAndTenantId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get distinct variant IDs by option ID and tenant ID
     */
    @Query("SELECT DISTINCT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<UUID> findVariantIdsByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Get distinct variant IDs by option value ID and tenant ID
     */
    @Query("SELECT DISTINCT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    List<UUID> findVariantIdsByOptionValueIdAndTenantId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if variant has option and tenant ID
     */
    @Query("SELECT COUNT(pvov) > 0 FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.optionId = :optionId AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL")
    boolean existsByVariantIdAndOptionIdAndTenantId(@Param("variantId") UUID variantId, @Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find variant by option values and tenant ID
     */
    @Query("SELECT pvov.variantId FROM ProductVariantOptionValue pvov " +
           "JOIN ProductVariant pv ON pvov.variantId = pv.id " +
           "WHERE pv.productId = :productId AND pvov.optionValueId IN :optionValueIds " +
           "AND pvov.tenantId = :tenantId AND pvov.deletedAt IS NULL " +
           "GROUP BY pvov.variantId " +
           "HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    Optional<UUID> findVariantByOptionValues(@Param("productId") UUID productId, @Param("optionValueIds") java.util.Collection<UUID> optionValueIds, @Param("optionValueCount") int optionValueCount, @Param("tenantId") UUID tenantId);
    
    /**
     * Delete by option value ID and tenant ID
     */
    @Modifying
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :tenantId")
    void deleteByOptionValueIdAndTenantId(@Param("optionValueId") UUID optionValueId, @Param("tenantId") UUID tenantId);
    
    /**
     * Delete by option ID and tenant ID
     */
    @Modifying
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.optionId = :optionId AND pvov.tenantId = :tenantId")
    void deleteByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Delete by variant ID and tenant ID
     */
    @Modifying
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.variantId = :variantId AND pvov.tenantId = :tenantId")
    void deleteByVariantIdAndTenantId(@Param("variantId") UUID variantId, @Param("tenantId") UUID tenantId);
}
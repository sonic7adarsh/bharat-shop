package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductVariantOptionValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantOptionValueRepository extends TenantAwareRepository<ProductVariantOptionValue> {
    
    /**
     * Find all option values by variant ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL ORDER BY pvov.optionId ASC")
    List<ProductVariantOptionValue> findByVariantId(@Param("variantId") UUID variantId);
    
    /**
     * Find all option values by multiple variant IDs for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId IN :variantIds AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC, pvov.optionId ASC")
    List<ProductVariantOptionValue> findByVariantIds(@Param("variantIds") List<UUID> variantIds);
    
    /**
     * Find all option values by option ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC")
    List<ProductVariantOptionValue> findByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Find all option values by option value ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL ORDER BY pvov.variantId ASC")
    List<ProductVariantOptionValue> findByOptionValueId(@Param("optionValueId") UUID optionValueId);
    
    /**
     * Find option value by variant ID and option ID for current tenant
     */
    @Query("SELECT pvov FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.optionId = :optionId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    List<ProductVariantOptionValue> findByVariantIdAndOptionId(@Param("variantId") UUID variantId, @Param("optionId") UUID optionId);
    
    /**
     * Find variants by option value combinations for current tenant
     * This is used to find variants that match specific option value combinations
     */
    @Query("SELECT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId IN :optionValueIds AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL GROUP BY pvov.variantId HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    List<UUID> findVariantIdsByOptionValueCombination(@Param("optionValueIds") List<UUID> optionValueIds, @Param("optionValueCount") long optionValueCount);
    
    /**
     * Check if variant has specific option value combination for current tenant
     */
    @Query("SELECT COUNT(pvov) FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.optionValueId IN :optionValueIds AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    long countByVariantIdAndOptionValueIds(@Param("variantId") UUID variantId, @Param("optionValueIds") List<UUID> optionValueIds);
    
    /**
     * Check if option value combination exists for any variant (excluding current variant) for current tenant
     * This is used to enforce unique variant combinations
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) > 0 FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId IN :optionValueIds AND pvov.variantId != :excludeVariantId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL GROUP BY pvov.variantId HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    boolean existsByOptionValueCombinationAndVariantIdNot(@Param("optionValueIds") List<UUID> optionValueIds, @Param("optionValueCount") long optionValueCount, @Param("excludeVariantId") UUID excludeVariantId);
    
    /**
     * Check if option value combination exists for any variant for current tenant
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) > 0 FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId IN :optionValueIds AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL GROUP BY pvov.variantId HAVING COUNT(DISTINCT pvov.optionValueId) = :optionValueCount")
    boolean existsByOptionValueCombination(@Param("optionValueIds") List<UUID> optionValueIds, @Param("optionValueCount") long optionValueCount);
    
    /**
     * Count option values by variant ID for current tenant
     */
    @Query("SELECT COUNT(pvov) FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    long countByVariantId(@Param("variantId") UUID variantId);
    
    /**
     * Count variants by option ID for current tenant
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    long countVariantsByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Count variants by option value ID for current tenant
     */
    @Query("SELECT COUNT(DISTINCT pvov.variantId) FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    long countVariantsByOptionValueId(@Param("optionValueId") UUID optionValueId);
    
    /**
     * Get distinct option IDs by variant ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.optionId FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    List<UUID> getOptionIdsByVariantId(@Param("variantId") UUID variantId);
    
    /**
     * Get distinct option value IDs by variant ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.optionValueId FROM ProductVariantOptionValue pvov WHERE pvov.variantId = :variantId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    List<UUID> getOptionValueIdsByVariantId(@Param("variantId") UUID variantId);
    
    /**
     * Get distinct variant IDs by option ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionId = :optionId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    List<UUID> getVariantIdsByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Get distinct variant IDs by option value ID for current tenant
     */
    @Query("SELECT DISTINCT pvov.variantId FROM ProductVariantOptionValue pvov WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND pvov.deletedAt IS NULL")
    List<UUID> getVariantIdsByOptionValueId(@Param("optionValueId") UUID optionValueId);
    
    /**
     * Delete all option values by variant ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.variantId = :variantId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByVariantId(@Param("variantId") UUID variantId);
    
    /**
     * Delete all option values by option ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.optionId = :optionId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Delete all option values by option value ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.optionValueId = :optionValueId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByOptionValueId(@Param("optionValueId") UUID optionValueId);
    
    /**
     * Delete specific option value by variant ID and option ID (soft delete)
     */
    @Query("UPDATE ProductVariantOptionValue pvov SET pvov.deletedAt = CURRENT_TIMESTAMP WHERE pvov.variantId = :variantId AND pvov.optionId = :optionId AND pvov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByVariantIdAndOptionId(@Param("variantId") UUID variantId, @Param("optionId") UUID optionId);
}
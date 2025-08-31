package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductOption;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductOptionRepository extends TenantAwareRepository<ProductOption> {
    
    /**
     * Find all product options by product ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findByProductId(@Param("productId") UUID productId);
    
    /**
     * Find product option by product ID and option ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.optionId = :optionId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL")
    Optional<ProductOption> findByProductIdAndOptionId(@Param("productId") UUID productId, @Param("optionId") UUID optionId);
    
    /**
     * Find all product options by option ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.optionId = :optionId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Find required product options by product ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isRequired = true AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findRequiredByProductId(@Param("productId") UUID productId);
    
    /**
     * Find product options by product IDs for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId IN :productIds AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL ORDER BY po.productId ASC, po.sortOrder ASC")
    List<ProductOption> findByProductIds(@Param("productIds") List<UUID> productIds);
    
    /**
     * Check if product option exists
     */
    @Query("SELECT COUNT(po) > 0 FROM ProductOption po WHERE po.productId = :productId AND po.optionId = :optionId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL")
    boolean existsByProductIdAndOptionId(@Param("productId") UUID productId, @Param("optionId") UUID optionId);
    
    /**
     * Count product options by product ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.productId = :productId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL")
    long countByProductId(@Param("productId") UUID productId);
    
    /**
     * Count required product options by product ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.productId = :productId AND po.isRequired = true AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL")
    long countRequiredByProductId(@Param("productId") UUID productId);
    
    /**
     * Delete all product options by product ID (soft delete)
     */
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP WHERE po.productId = :productId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByProductId(@Param("productId") UUID productId);
    
    /**
     * Delete all product options by option ID (soft delete)
     */
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP WHERE po.optionId = :optionId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Find products that use a specific option
     */
    @Query("SELECT DISTINCT po.productId FROM ProductOption po WHERE po.optionId = :optionId AND po.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND po.deletedAt IS NULL")
    List<UUID> findProductIdsByOptionId(@Param("optionId") UUID optionId);
}
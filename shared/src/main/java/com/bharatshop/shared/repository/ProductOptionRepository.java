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
import java.util.UUID;

@Repository
public interface ProductOptionRepository extends TenantAwareRepository<ProductOption> {
    
    /**
     * Find all product options by product ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find product option by product ID and option ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    Optional<ProductOption> findByProductIdAndOptionId(@Param("productId") UUID productId, @Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all product options by option ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find required product options by product ID for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isRequired = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findRequiredByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find product options by product IDs for current tenant
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId IN :productIds AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.productId ASC, po.sortOrder ASC")
    List<ProductOption> findByProductIds(@Param("productIds") List<UUID> productIds, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if product option exists
     */
    @Query("SELECT COUNT(po) > 0 FROM ProductOption po WHERE po.productId = :productId AND po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    boolean existsByProductIdAndOptionId(@Param("productId") UUID productId, @Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if product option exists by tenant ID
     */
    @Query("SELECT COUNT(po) > 0 FROM ProductOption po WHERE po.productId = :productId AND po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    boolean existsByProductIdAndOptionIdAndTenantId(@Param("productId") UUID productId, @Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active product option by product ID, option ID and tenant ID
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    Optional<ProductOption> findActiveByProductIdAndOptionIdAndTenantId(@Param("productId") UUID productId, @Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find product IDs by option ID and tenant ID
     */
    @Query("SELECT DISTINCT po.productId FROM ProductOption po WHERE po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    List<UUID> findProductIdsByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active required product options by product ID and tenant ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.productId = :productId AND po.isRequired = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    long countActiveRequiredByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count product options by product ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.productId = :productId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    long countByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count required product options by product ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.productId = :productId AND po.isRequired = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    long countRequiredByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Delete all product options by product ID (soft delete)
     */
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP WHERE po.productId = :productId AND po.tenantId = :tenantId")
    void deleteByProductId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Delete all product options by option ID (soft delete)
     */
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP WHERE po.optionId = :optionId AND po.tenantId = :tenantId")
    void deleteByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find products that use a specific option
     */
    @Query("SELECT DISTINCT po.productId FROM ProductOption po WHERE po.optionId = :optionId AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    List<UUID> findProductIdsByOptionId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Soft delete by option ID and tenant ID
     */
    @Modifying
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP WHERE po.optionId = :optionId AND po.tenantId = :tenantId")
    void softDeleteByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active options by product ID and tenant ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.productId = :productId AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    long countActiveByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active options by option ID and tenant ID
     */
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.optionId = :optionId AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL")
    long countActiveByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active options by multiple product IDs and tenant ID
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId IN :productIds AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findActiveByProductIdsAndTenantId(@Param("productIds") List<UUID> productIds, @Param("tenantId") UUID tenantId);
    
    /**
     * Soft delete by product ID and tenant ID
     */
    @Modifying
    @Query("UPDATE ProductOption po SET po.deletedAt = CURRENT_TIMESTAMP WHERE po.productId = :productId AND po.tenantId = :tenantId")
    void softDeleteByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active required options by product ID and tenant ID
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isRequired = true AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findActiveRequiredByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active options by option ID and tenant ID
     */
    @Query("SELECT po FROM ProductOption po WHERE po.optionId = :optionId AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findActiveByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active options by product ID and tenant ID
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    List<ProductOption> findActiveByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active options by product ID and tenant ID with pagination
     */
    @Query("SELECT po FROM ProductOption po WHERE po.productId = :productId AND po.isActive = true AND po.tenantId = :tenantId AND po.deletedAt IS NULL ORDER BY po.sortOrder ASC")
    Page<ProductOption> findActiveByProductIdAndTenantId(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId, Pageable pageable);
}
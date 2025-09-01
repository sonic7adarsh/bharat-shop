package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.OptionValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OptionValueRepository extends TenantAwareRepository<OptionValue> {
    
    /**
     * Find all active option values by option ID for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL ORDER BY ov.sortOrder ASC")
    List<OptionValue> findActiveByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Find active option value by option ID and value for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.value = :value AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL")
    Optional<OptionValue> findActiveByOptionIdAndValue(@Param("optionId") UUID optionId, @Param("value") String value);
    
    /**
     * Find option values by option IDs for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId IN :optionIds AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL ORDER BY ov.optionId ASC, ov.sortOrder ASC")
    List<OptionValue> findActiveByOptionIds(@Param("optionIds") List<UUID> optionIds);
    
    /**
     * Find option values by IDs for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.id IN :ids AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL ORDER BY ov.sortOrder ASC")
    List<OptionValue> findActiveByIds(@Param("ids") List<UUID> ids);
    
    /**
     * Check if option value exists for option (excluding current value)
     */
    @Query("SELECT COUNT(ov) > 0 FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.value = :value AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.id != :excludeId AND ov.deletedAt IS NULL")
    boolean existsByOptionIdAndValueAndIdNot(@Param("optionId") UUID optionId, @Param("value") String value, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if option value exists for option
     */
    @Query("SELECT COUNT(ov) > 0 FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.value = :value AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL")
    boolean existsByOptionIdAndValue(@Param("optionId") UUID optionId, @Param("value") String value);
    
    /**
     * Search option values by value or display value for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL AND (LOWER(ov.value) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(ov.displayValue) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY ov.sortOrder ASC")
    List<OptionValue> searchActiveByOptionId(@Param("optionId") UUID optionId, @Param("search") String search);
    
    /**
     * Find active option value by ID for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.id = :id AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL")
    Optional<OptionValue> findActiveById(@Param("id") UUID id);
    
    /**
     * Count active option values by option ID
     */
    @Query("SELECT COUNT(ov) FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND ov.deletedAt IS NULL")
    long countActiveByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Delete all option values by option ID (soft delete)
     */
    @Query("UPDATE OptionValue ov SET ov.deletedAt = CURRENT_TIMESTAMP WHERE ov.optionId = :optionId AND ov.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()}")
    void deleteByOptionId(@Param("optionId") UUID optionId);
    
    /**
     * Search option values by value or display value with pagination for current tenant
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL AND (LOWER(ov.value) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(ov.displayValue) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY ov.sortOrder ASC")
    Page<OptionValue> searchActiveByOptionIdAndKeyword(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Check if option value exists for option and tenant
     */
    @Query("SELECT COUNT(ov) > 0 FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.value = :value AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL")
    boolean existsByOptionIdAndValueAndTenantId(@Param("optionId") UUID optionId, @Param("value") String value, @Param("tenantId") UUID tenantId);
    
    /**
     * Find option values by IDs and tenant ID
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.id IN :ids AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL ORDER BY ov.sortOrder ASC")
    List<OptionValue> findActiveByIdsAndTenantId(@Param("ids") List<UUID> ids, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active option value by ID and tenant ID
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.id = :id AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL")
    Optional<OptionValue> findActiveByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    
    /**
     * Soft delete all option values by option ID and tenant ID
     */
    @Query("UPDATE OptionValue ov SET ov.deletedAt = CURRENT_TIMESTAMP WHERE ov.optionId = :optionId AND ov.tenantId = :tenantId")
    void softDeleteByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active option values by option ID and tenant ID
     */
    @Query("SELECT COUNT(ov) FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL")
    long countActiveByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active option values by tenant ID
     */
    @Query("SELECT COUNT(ov) FROM OptionValue ov WHERE ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Find all active option values by option ID and tenant ID with pagination
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL ORDER BY ov.sortOrder ASC")
    Page<OptionValue> findActiveByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId, Pageable pageable);
    
    /**
     * Find active option value by option ID, value and tenant ID
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.value = :value AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL")
    Optional<OptionValue> findActiveByOptionIdAndValueAndTenantId(@Param("optionId") UUID optionId, @Param("value") String value, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all active option values by option ID and tenant ID (non-pageable)
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionId = :optionId AND ov.isActive = true AND ov.tenantId = :tenantId AND ov.deletedAt IS NULL ORDER BY ov.sortOrder ASC")
    List<OptionValue> findActiveByOptionIdAndTenantId(@Param("optionId") UUID optionId, @Param("tenantId") UUID tenantId);
}
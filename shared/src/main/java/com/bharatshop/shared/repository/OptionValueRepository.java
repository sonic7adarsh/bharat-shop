package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.OptionValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptionValueRepository extends TenantAwareRepository<OptionValue> {
    
    /**
     * Find all active option values by option ID for current tenant
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<OptionValue> findActiveByOptionId(@Param("optionId") Long optionId, @Param("tenantId") String tenantId);
    
    /**
     * Find active option value by option ID and value for current tenant
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND value = :value AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<OptionValue> findActiveByOptionIdAndValue(@Param("optionId") Long optionId, @Param("value") String value, @Param("tenantId") String tenantId);
    
    /**
     * Find option values by option IDs for current tenant
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id IN :optionIds AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY option_id ASC, sort_order ASC", nativeQuery = true)
    List<OptionValue> findActiveByOptionIds(@Param("optionIds") List<Long> optionIds, @Param("tenantId") String tenantId);
    
    /**
     * Find option values by IDs for current tenant
     */
    @Query(value = "SELECT * FROM option_values WHERE id IN :ids AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<OptionValue> findActiveByIds(@Param("ids") List<Long> ids, @Param("tenantId") String tenantId);
    
    /**
     * Check if option value exists for option (excluding current value)
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM option_values WHERE option_id = :optionId AND value = :value AND tenant_id = :tenantId AND id != :excludeId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByOptionIdAndValueAndIdNot(@Param("optionId") Long optionId, @Param("value") String value, @Param("excludeId") Long excludeId, @Param("tenantId") String tenantId);
    
    /**
     * Check if option value exists for option
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM option_values WHERE option_id = :optionId AND value = :value AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByOptionIdAndValue(@Param("optionId") Long optionId, @Param("value") String value, @Param("tenantId") String tenantId);
    
    /**
     * Search option values by value or display value for current tenant
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND (LOWER(value) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(display_value) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY sort_order ASC", nativeQuery = true)
    List<OptionValue> searchActiveByOptionId(@Param("optionId") Long optionId, @Param("search") String search, @Param("tenantId") String tenantId);
    
    /**
     * Find active option value by ID for current tenant
     */
    @Query(value = "SELECT * FROM option_values WHERE id = :id AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<OptionValue> findActiveById(@Param("id") Long id, @Param("tenantId") String tenantId);
    
    /**
     * Count active option values by option ID
     */
    @Query(value = "SELECT COUNT(*) FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByOptionId(@Param("optionId") Long optionId, @Param("tenantId") String tenantId);
    
    /**
     * Delete all option values by option ID (soft delete)
     */
    @Query(value = "UPDATE option_values SET deleted_at = CURRENT_TIMESTAMP WHERE option_id = :optionId AND tenant_id = :tenantId", nativeQuery = true)
    void deleteByOptionId(@Param("optionId") Long optionId, @Param("tenantId") String tenantId);
    
    /**
     * Search option values by option ID and keyword with pagination
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND (LOWER(value) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(display_value) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY sort_order ASC", nativeQuery = true)
    Page<OptionValue> searchActiveByOptionIdAndKeyword(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Check if option value exists for option and tenant
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM option_values WHERE option_id = :optionId AND value = :value AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByOptionIdAndValueAndTenantId(@Param("optionId") Long optionId, @Param("value") String value, @Param("tenantId") Long tenantId);
    
    /**
     * Find option values by IDs and tenant ID
     */
    @Query(value = "SELECT * FROM option_values WHERE id IN :ids AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<OptionValue> findActiveByIdsAndTenantId(@Param("ids") List<Long> ids, @Param("tenantId") Long tenantId);
    
    /**
     * Find active option value by ID and tenant ID
     */
    @Query(value = "SELECT * FROM option_values WHERE id = :id AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<OptionValue> findActiveByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
    
    /**
     * Soft delete all option values by option ID and tenant ID
     */
    @Query(value = "UPDATE option_values SET deleted_at = CURRENT_TIMESTAMP WHERE option_id = :optionId AND tenant_id = :tenantId", nativeQuery = true)
    void softDeleteByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Count active option values by option ID and tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
    
    /**
     * Count active option values by tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM option_values WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Find all active option values by option ID and tenant ID with pagination
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    Page<OptionValue> findActiveByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId, Pageable pageable);
    
    /**
     * Find active option value by option ID, value and tenant ID
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND value = :value AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<OptionValue> findActiveByOptionIdAndValueAndTenantId(@Param("optionId") Long optionId, @Param("value") String value, @Param("tenantId") Long tenantId);
    
    /**
     * Find all active option values by option ID and tenant ID (non-pageable)
     */
    @Query(value = "SELECT * FROM option_values WHERE option_id = :optionId AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<OptionValue> findActiveByOptionIdAndTenantId(@Param("optionId") Long optionId, @Param("tenantId") Long tenantId);
}
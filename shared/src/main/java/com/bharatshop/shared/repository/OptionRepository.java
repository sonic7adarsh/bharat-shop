package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptionRepository extends TenantAwareRepository<Option> {
    
    /**
     * Find all active options for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> findAllActive(@Param("tenantId") String tenantId);
    
    /**
     * Find active option by name for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE name = :name AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<Option> findActiveByName(@Param("name") String name, @Param("tenantId") String tenantId);
    
    /**
     * Find options by type for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE type = :type AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> findActiveByType(@Param("type") String type, @Param("tenantId") String tenantId);
    
    /**
     * Find required options for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE is_required = true AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> findRequiredOptions(@Param("tenantId") String tenantId);
    
    /**
     * Check if option name exists for current tenant (excluding current option)
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM options WHERE name = :name AND tenant_id = :tenantId AND id != :excludeId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByNameAndTenantIdAndIdNot(@Param("name") String name, @Param("excludeId") Long excludeId, @Param("tenantId") String tenantId);
    
    /**
     * Check if option name exists for current tenant
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM options WHERE name = :name AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") String tenantId);
    
    /**
     * Search options by name or display name for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND (LOWER(name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(display_name) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> searchActiveOptions(@Param("search") String search, @Param("tenantId") String tenantId);
    
    /**
     * Find active option by ID for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE id = :id AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<Option> findActiveById(@Param("id") Long id, @Param("tenantId") String tenantId);
    
    /**
     * Find options by IDs for current tenant
     */
    @Query(value = "SELECT * FROM options WHERE id IN :ids AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> findActiveByIds(@Param("ids") List<Long> ids, @Param("tenantId") String tenantId);
    
    /**
     * Find all active options by tenant ID
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> findAllActiveByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Find all active options by tenant ID with pagination
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    Page<Option> findAllActiveByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);
    
    /**
     * Find active option by ID and tenant ID
     */
    @Query(value = "SELECT * FROM options WHERE id = :id AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<Option> findActiveByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
    
    /**
     * Find active option by name and tenant ID
     */
    @Query(value = "SELECT * FROM options WHERE name = :name AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<Option> findActiveByNameAndTenantId(@Param("name") String name, @Param("tenantId") Long tenantId);
    
    /**
     * Find options by type and tenant ID
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND type = :type AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> findActiveByTenantIdAndType(@Param("tenantId") Long tenantId, @Param("type") String type);
    
    /**
     * Search active options by tenant ID and keyword
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND (LOWER(name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(display_name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY sort_order ASC", nativeQuery = true)
    List<Option> searchActiveByTenantIdAndKeyword(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);

    /**
     * Search active options by tenant ID and keyword with pagination
     */
    @Query(value = "SELECT * FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND (LOWER(name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(display_name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY sort_order ASC", nativeQuery = true)
    Page<Option> searchActiveByTenantIdAndKeywordWithPagination(@Param("tenantId") Long tenantId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Check if option name exists for tenant
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM options WHERE name = :name AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") Long tenantId);
    
    /**
     * Count active options by tenant ID
     */
    @Query(value = "SELECT COUNT(*) FROM options WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Count active options by tenant ID and type
     */
    @Query(value = "SELECT COUNT(*) FROM options WHERE is_active = true AND tenant_id = :tenantId AND type = :type AND deleted_at IS NULL", nativeQuery = true)
    long countActiveByTenantIdAndType(@Param("tenantId") Long tenantId, @Param("type") String type);
}
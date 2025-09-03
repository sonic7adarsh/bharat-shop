package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OptionRepository extends TenantAwareRepository<Option> {
    
    /**
     * Find all active options for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findAllActive(@Param("tenantId") String tenantId);
    
    /**
     * Find active option by name for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.name = :name AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    Optional<Option> findActiveByName(@Param("name") String name, @Param("tenantId") String tenantId);
    
    /**
     * Find options by type for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.type = :type AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findActiveByType(@Param("type") Option.OptionType type, @Param("tenantId") String tenantId);
    
    /**
     * Find required options for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.isRequired = true AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findRequiredOptions(@Param("tenantId") String tenantId);
    
    /**
     * Check if option name exists for current tenant (excluding current option)
     */
    @Query("SELECT COUNT(o) > 0 FROM Option o WHERE o.name = :name AND o.tenantId = :tenantId AND o.id != :excludeId AND o.deletedAt IS NULL")
    boolean existsByNameAndTenantIdAndIdNot(@Param("name") String name, @Param("excludeId") UUID excludeId, @Param("tenantId") String tenantId);
    
    /**
     * Check if option name exists for current tenant
     */
    @Query("SELECT COUNT(o) > 0 FROM Option o WHERE o.name = :name AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") String tenantId);
    
    /**
     * Search options by name or display name for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL AND (LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.displayName) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY o.sortOrder ASC")
    List<Option> searchActiveOptions(@Param("search") String search, @Param("tenantId") String tenantId);
    
    /**
     * Find active option by ID for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.id = :id AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    Optional<Option> findActiveById(@Param("id") UUID id, @Param("tenantId") String tenantId);
    
    /**
     * Find options by IDs for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.id IN :ids AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findActiveByIds(@Param("ids") List<UUID> ids, @Param("tenantId") String tenantId);
    
    /**
     * Find all active options by tenant ID
     */
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findAllActiveByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Find all active options by tenant ID with pagination
     */
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    Page<Option> findAllActiveByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
    
    /**
     * Find active option by ID and tenant ID
     */
    @Query("SELECT o FROM Option o WHERE o.id = :id AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    Optional<Option> findActiveByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active option by name and tenant ID
     */
    @Query("SELECT o FROM Option o WHERE o.name = :name AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    Optional<Option> findActiveByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);
    
    /**
     * Find options by type and tenant ID
     */
    @Query("SELECT o FROM Option o WHERE o.type = :type AND o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findActiveByTenantIdAndType(@Param("tenantId") UUID tenantId, @Param("type") Option.OptionType type);
    
    /**
     * Search options by keyword and tenant ID
     */
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL AND (LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY o.sortOrder ASC")
    Page<Option> searchActiveByTenantIdAndKeyword(@Param("tenantId") UUID tenantId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Check if option name exists for tenant ID
     */
    @Query("SELECT COUNT(o) > 0 FROM Option o WHERE o.name = :name AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);
    
    /**
     * Count active options by tenant ID
     */
    @Query("SELECT COUNT(o) FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.deletedAt IS NULL")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Count active options by tenant ID and type
     */
    @Query("SELECT COUNT(o) FROM Option o WHERE o.isActive = true AND o.tenantId = :tenantId AND o.type = :type AND o.deletedAt IS NULL")
    long countActiveByTenantIdAndType(@Param("tenantId") UUID tenantId, @Param("type") Option.OptionType type);
}
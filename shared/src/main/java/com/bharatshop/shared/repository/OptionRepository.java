package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Option;
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
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findAllActive();
    
    /**
     * Find active option by name for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.name = :name AND o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL")
    Optional<Option> findActiveByName(@Param("name") String name);
    
    /**
     * Find options by type for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.type = :type AND o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findActiveByType(@Param("type") Option.OptionType type);
    
    /**
     * Find required options for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.isRequired = true AND o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findRequiredOptions();
    
    /**
     * Check if option name exists for current tenant (excluding current option)
     */
    @Query("SELECT COUNT(o) > 0 FROM Option o WHERE o.name = :name AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.id != :excludeId AND o.deletedAt IS NULL")
    boolean existsByNameAndTenantIdAndIdNot(@Param("name") String name, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if option name exists for current tenant
     */
    @Query("SELECT COUNT(o) > 0 FROM Option o WHERE o.name = :name AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL")
    boolean existsByNameAndTenantId(@Param("name") String name);
    
    /**
     * Search options by name or display name for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL AND (LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.displayName) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY o.sortOrder ASC")
    List<Option> searchActiveOptions(@Param("search") String search);
    
    /**
     * Find active option by ID for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.id = :id AND o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL")
    Optional<Option> findActiveById(@Param("id") UUID id);
    
    /**
     * Find options by IDs for current tenant
     */
    @Query("SELECT o FROM Option o WHERE o.id IN :ids AND o.isActive = true AND o.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND o.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<Option> findActiveByIds(@Param("ids") List<UUID> ids);
}
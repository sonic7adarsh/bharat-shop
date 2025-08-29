package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Template;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends TenantAwareRepository<Template> {
    
    /**
     * Find all active templates
     */
    @Query("SELECT t FROM Template t WHERE t.isActive = true AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.deletedAt IS NULL ORDER BY t.sortOrder ASC")
    List<Template> findAllActiveTemplates();
    
    /**
     * Find active template by name
     */
    @Query("SELECT t FROM Template t WHERE t.name = :name AND t.isActive = true AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.deletedAt IS NULL")
    Optional<Template> findActiveByName(@Param("name") String name);
    
    /**
     * Find templates by category
     */
    @Query("SELECT t FROM Template t WHERE t.category = :category AND t.isActive = true AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.deletedAt IS NULL ORDER BY t.sortOrder ASC")
    List<Template> findActiveByCategory(@Param("category") String category);
    
    /**
     * Find all template categories
     */
    @Query("SELECT DISTINCT t.category FROM Template t WHERE t.isActive = true AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.deletedAt IS NULL AND t.category IS NOT NULL ORDER BY t.category")
    List<String> findAllCategories();
    
    /**
     * Check if template name exists for tenant (excluding current template)
     */
    @Query("SELECT COUNT(t) > 0 FROM Template t WHERE t.name = :name AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.id != :excludeId AND t.deletedAt IS NULL")
    boolean existsByNameAndTenantIdAndIdNot(@Param("name") String name, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if template name exists for tenant
     */
    @Query("SELECT COUNT(t) > 0 FROM Template t WHERE t.name = :name AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.deletedAt IS NULL")
    boolean existsByNameAndTenantId(@Param("name") String name);
    
    /**
     * Search templates by name or description
     */
    @Query("SELECT t FROM Template t WHERE t.isActive = true AND t.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND t.deletedAt IS NULL AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY t.sortOrder ASC")
    List<Template> searchActiveTemplates(@Param("search") String search);
    
    /**
     * Find active template by ID
     */
    @Query("SELECT t FROM Template t WHERE t.id = :id AND t.isActive = true AND t.deletedAt IS NULL")
    Optional<Template> findActiveById(@Param("id") UUID id);
}
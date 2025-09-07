package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Template;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
// import java.util.UUID;

@Repository
public interface TemplateRepository extends TenantAwareRepository<Template> {
    
    /**
     * Find all active templates
     */
    @Query(value = "SELECT * FROM templates WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Template> findAllActiveTemplates(@Param("tenantId") Long tenantId);
    
    /**
     * Find active template by name
     */
    @Query(value = "SELECT * FROM templates WHERE name = :name AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<Template> findActiveByName(@Param("name") String name, @Param("tenantId") Long tenantId);
    
    /**
     * Find templates by category
     */
    @Query(value = "SELECT * FROM templates WHERE category = :category AND is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Template> findActiveByCategory(@Param("category") String category, @Param("tenantId") Long tenantId);
    
    /**
     * Find all template categories
     */
    @Query(value = "SELECT DISTINCT category FROM templates WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND category IS NOT NULL ORDER BY category", nativeQuery = true)
    List<String> findAllCategories(@Param("tenantId") Long tenantId);
    
    /**
     * Check if template name exists for tenant (excluding current template)
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM templates WHERE name = :name AND tenant_id = :tenantId AND id != :excludeId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByNameAndTenantIdAndIdNot(@Param("name") String name, @Param("excludeId") Long excludeId, @Param("tenantId") Long tenantId);
    
    /**
     * Check if template name exists for tenant
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM templates WHERE name = :name AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") Long tenantId);
    
    /**
     * Search templates by name or description
     */
    @Query(value = "SELECT * FROM templates WHERE is_active = true AND tenant_id = :tenantId AND deleted_at IS NULL AND (LOWER(name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY sort_order ASC", nativeQuery = true)
    List<Template> searchActiveTemplates(@Param("search") String search, @Param("tenantId") Long tenantId);
    
    /**
     * Find active template by ID
     */
    @Query(value = "SELECT * FROM templates WHERE id = :id AND is_active = true AND deleted_at IS NULL", nativeQuery = true)
    Optional<Template> findActiveById(@Param("id") Long id);
}
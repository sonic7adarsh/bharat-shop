package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.enums.PageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Page entity operations.
 * Provides CRUD operations and tenant-specific queries for page management.
 */
@Repository
public interface PageRepository extends TenantAwareRepository<Page> {
    
    /**
     * Find active page by slug for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.slug = :slug AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false")
    Optional<Page> findActiveBySlug(@Param("slug") String slug);
    
    /**
     * Find active and published page by slug for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.slug = :slug AND p.active = true AND p.published = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false")
    Optional<Page> findActivePublishedBySlug(@Param("slug") String slug);
    
    /**
     * Find active page by slug for specific tenant
     */
    @Query("SELECT p FROM Page p WHERE p.slug = :slug AND p.active = true AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<Page> findActiveBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find active and published page by slug for specific tenant
     */
    @Query("SELECT p FROM Page p WHERE p.slug = :slug AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<Page> findActivePublishedBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find all active pages by page type for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.pageType = :pageType AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.sortOrder ASC")
    List<Page> findActiveByPageType(@Param("pageType") PageType pageType);
    
    /**
     * Find all active and published pages by page type for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.pageType = :pageType AND p.active = true AND p.published = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.sortOrder ASC")
    List<Page> findActivePublishedByPageType(@Param("pageType") PageType pageType);
    
    /**
     * Find all active and published pages for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.active = true AND p.published = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.sortOrder ASC")
    List<Page> findAllActivePublished();
    
    /**
     * Find all active and published pages for current tenant with pagination
     */
    @Query("SELECT p FROM Page p WHERE p.active = true AND p.published = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.sortOrder ASC")
    org.springframework.data.domain.Page<Page> findAllActivePublished(Pageable pageable);
    
    /**
     * Find pages by template for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.template = :template AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.sortOrder ASC")
    List<Page> findActiveByTemplate(@Param("template") String template);
    
    /**
     * Find pages by template ID for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.templateId = :templateId AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.sortOrder ASC")
    List<Page> findActiveByTemplateId(@Param("templateId") String templateId);
    
    /**
     * Check if slug exists for current tenant (excluding specific page)
     */
    @Query("SELECT COUNT(p) > 0 FROM Page p WHERE p.slug = :slug AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.id != :excludeId AND p.deleted = false")
    boolean existsBySlugExcludingId(@Param("slug") String slug, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if slug exists for current tenant
     */
    @Query("SELECT COUNT(p) > 0 FROM Page p WHERE p.slug = :slug AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false")
    boolean existsBySlug(@Param("slug") String slug);
    
    /**
     * Find pages by status for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.status = :status AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.updatedAt DESC")
    List<Page> findActiveByStatus(@Param("status") String status);
    
    /**
     * Find pages by author for current tenant
     */
    @Query("SELECT p FROM Page p WHERE p.author = :author AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.updatedAt DESC")
    List<Page> findActiveByAuthor(@Param("author") String author);
    
    /**
     * Search pages by title or content for current tenant
     */
    @Query("SELECT p FROM Page p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND p.active = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.updatedAt DESC")
    List<Page> searchActiveByKeyword(@Param("keyword") String keyword);
    
    /**
     * Search published pages by title or content for current tenant
     */
    @Query("SELECT p FROM Page p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND p.active = true AND p.published = true AND p.tenantId = :#{T(com.bharatshop.shared.tenant.TenantContext).getCurrentTenant()} AND p.deleted = false ORDER BY p.updatedAt DESC")
    List<Page> searchActivePublishedByKeyword(@Param("keyword") String keyword);
}
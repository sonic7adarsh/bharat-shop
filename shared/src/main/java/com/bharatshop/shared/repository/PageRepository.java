package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.enums.PageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/**
 * Repository interface for Page entity operations.
 * Provides CRUD operations and tenant-specific queries for page management.
 */
@Repository
public interface PageRepository extends TenantAwareRepository<Page> {
    
    /**
     * Find active page by slug for current tenant
     */
    Optional<Page> findBySlugAndActiveTrueAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);
    
    /**
     * Find active and published page by slug for current tenant
     */
    Optional<Page> findBySlugAndActiveTrueAndPublishedTrueAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);
    
    /**
     * Find all active pages by page type for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE page_type = ?1 AND active = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActiveByPageType(PageType pageType, Long tenantId);
    
    /**
     * Find all active and published pages by page type for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE page_type = ?1 AND active = true AND published = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActivePublishedByPageType(PageType pageType, Long tenantId);
    
    /**
     * Find all active and published pages for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE active = true AND published = true AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findAllActivePublished(Long tenantId);
    
    /**
     * Find all active and published pages for current tenant with pagination
     */
    @Query(value = "SELECT * FROM pages WHERE active = true AND published = true AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    org.springframework.data.domain.Page<Page> findAllActivePublished(Long tenantId, Pageable pageable);
    
    /**
     * Find pages by template for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE template = ?1 AND active = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActiveByTemplate(String template, Long tenantId);
    
    /**
     * Find pages by template ID for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE template_id = ?1 AND active = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActiveByTemplateId(String templateId, Long tenantId);
    
    /**
     * Check if slug exists for current tenant (excluding specific page)
     */
    boolean existsBySlugAndTenantIdAndIdNotAndDeletedAtIsNull(String slug, Long tenantId, Long excludeId);
    
    /**
     * Check if slug exists for current tenant
     */
    boolean existsBySlugAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);
    
    /**
     * Find pages by status for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE status = ?1 AND active = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActiveByStatus(String status, Long tenantId);
    
    /**
     * Find pages by author for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE author = ?1 AND active = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActiveByAuthor(String author, Long tenantId);
    
    /**
     * Search pages by title or content for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE (LOWER(title) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(content) LIKE LOWER(CONCAT('%', ?1, '%'))) AND active = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> searchActiveByKeyword(String keyword, Long tenantId);
    
    /**
     * Search published pages by title or content for current tenant
     */
    @Query(value = "SELECT * FROM pages WHERE (LOWER(title) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(content) LIKE LOWER(CONCAT('%', ?1, '%'))) AND active = true AND published = true AND tenant_id = ?2 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> searchActivePublishedByKeyword(String keyword, Long tenantId);
    
    /**
     * SEO-related methods for sitemap generation
     */
    @Query(value = "SELECT * FROM pages WHERE featured_in_sitemap = true AND active = true AND published = true AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findActivePublishedFeaturedInSitemap(Long tenantId);
    
    @Query(value = "SELECT * FROM pages WHERE featured_in_sitemap = true AND tenant_id = ?1 AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Page> findByTenantIdAndFeaturedInSitemapTrueAndDeletedAtIsNull(Long tenantId);
    
    @Query(value = "SELECT COUNT(*) FROM pages WHERE featured_in_sitemap = true AND active = true AND published = true AND tenant_id = ?1 AND deleted_at IS NULL", nativeQuery = true)
    long countActivePublishedFeaturedInSitemap(Long tenantId);
}
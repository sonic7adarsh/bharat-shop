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
    @Query("SELECT p FROM SharedPage p WHERE p.slug = :slug AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    Optional<Page> findActiveBySlug(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find active and published page by slug for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.slug = :slug AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    Optional<Page> findActivePublishedBySlug(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find active page by slug for specific tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.slug = :slug AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    Optional<Page> findActiveBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find active and published page by slug for specific tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.slug = :slug AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    Optional<Page> findActivePublishedBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find all active pages by page type for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.pageType = :pageType AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findActiveByPageType(@Param("pageType") PageType pageType, @Param("tenantId") String tenantId);
    
    /**
     * Find all active and published pages by page type for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.pageType = :pageType AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findActivePublishedByPageType(@Param("pageType") PageType pageType, @Param("tenantId") String tenantId);
    
    /**
     * Find all active and published pages for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findAllActivePublished(@Param("tenantId") String tenantId);
    
    /**
     * Find all active and published pages for current tenant with pagination
     */
    @Query("SELECT p FROM SharedPage p WHERE p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    org.springframework.data.domain.Page<Page> findAllActivePublished(@Param("tenantId") String tenantId, Pageable pageable);
    
    /**
     * Find pages by template for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.template = :template AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findActiveByTemplate(@Param("template") String template, @Param("tenantId") String tenantId);
    
    /**
     * Find pages by template ID for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.templateId = :templateId AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findActiveByTemplateId(@Param("templateId") String templateId, @Param("tenantId") String tenantId);
    
    /**
     * Check if slug exists for current tenant (excluding specific page)
     */
    @Query("SELECT COUNT(p) > 0 FROM SharedPage p WHERE p.slug = :slug AND p.tenantId = :tenantId AND p.id != :excludeId AND p.deletedAt IS NULL")
    boolean existsBySlugExcludingId(@Param("slug") String slug, @Param("excludeId") UUID excludeId, @Param("tenantId") String tenantId);
    
    /**
     * Check if slug exists for current tenant
     */
    @Query("SELECT COUNT(p) > 0 FROM SharedPage p WHERE p.slug = :slug AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    boolean existsBySlug(@Param("slug") String slug, @Param("tenantId") String tenantId);
    
    /**
     * Find pages by status for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.status = :status AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
    List<Page> findActiveByStatus(@Param("status") String status, @Param("tenantId") String tenantId);
    
    /**
     * Find pages by author for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE p.author = :author AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
    List<Page> findActiveByAuthor(@Param("author") String author, @Param("tenantId") String tenantId);
    
    /**
     * Search pages by title or content for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND p.active = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
    List<Page> searchActiveByKeyword(@Param("keyword") String keyword, @Param("tenantId") String tenantId);
    
    /**
     * Search published pages by title or content for current tenant
     */
    @Query("SELECT p FROM SharedPage p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.updatedAt DESC")
    List<Page> searchActivePublishedByKeyword(@Param("keyword") String keyword, @Param("tenantId") String tenantId);
}
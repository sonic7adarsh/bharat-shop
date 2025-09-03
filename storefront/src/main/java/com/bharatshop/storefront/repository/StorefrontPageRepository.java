package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.model.Page;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontPageRepository extends TenantAwareRepository<Page> {
    
    /**
     * Find active and published page by slug
     */
    Optional<Page> findBySlugAndActiveAndPublished(String slug, Boolean active, Boolean published);
    
    /**
     * Find active and published page by slug and tenant
     */
    @Query("SELECT p FROM StorefrontPage p WHERE p.slug = :slug AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    Optional<Page> findBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") UUID tenantId);
    
    /**
     * Find all active and published pages
     */
    List<Page> findByActiveAndPublishedOrderBySortOrderAsc(Boolean active, Boolean published);
    
    /**
     * Find all active and published pages by tenant
     */
    @Query("SELECT p FROM StorefrontPage p WHERE p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findActivePublishedByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Find pages by type
     */
    @Query("SELECT p FROM StorefrontPage p WHERE p.pageType = :pageType AND p.active = true AND p.published = true AND p.tenantId = :tenantId AND p.deletedAt IS NULL ORDER BY p.sortOrder ASC")
    List<Page> findByPageTypeAndTenantId(@Param("pageType") Page.PageType pageType, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if slug exists for tenant (excluding current page)
     */
    @Query("SELECT COUNT(p) > 0 FROM StorefrontPage p WHERE p.slug = :slug AND p.tenantId = :tenantId AND p.id != :excludeId AND p.deletedAt IS NULL")
    boolean existsBySlugAndTenantIdAndIdNot(@Param("slug") String slug, @Param("tenantId") UUID tenantId, @Param("excludeId") UUID excludeId);
    
    /**
     * Check if slug exists for tenant
     */
    @Query("SELECT COUNT(p) > 0 FROM StorefrontPage p WHERE p.slug = :slug AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    boolean existsBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active page by ID
     */
    @Query("SELECT p FROM StorefrontPage p WHERE p.id = :id AND p.active = true AND p.deletedAt IS NULL")
    Optional<Page> findActiveById(@Param("id") UUID id);
}
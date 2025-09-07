package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.enums.PageType;
import com.bharatshop.shared.repository.TenantAwareRepository;

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
    Optional<Page> findBySlugAndActiveAndPublishedAndTenantIdAndDeletedAtIsNull(String slug, Boolean active, Boolean published, Long tenantId);
    
    /**
     * Find all active and published pages
     */
    List<Page> findByActiveAndPublishedOrderBySortOrderAsc(Boolean active, Boolean published);
    
    /**
     * Find all active and published pages by tenant
     */
    List<Page> findByActiveAndPublishedAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(Boolean active, Boolean published, Long tenantId);
    
    /**
     * Find pages by type
     */
    List<Page> findByPageTypeAndActiveAndPublishedAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(PageType pageType, Boolean active, Boolean published, Long tenantId);
    
    /**
     * Check if slug exists for tenant (excluding current page)
     */
    boolean existsBySlugAndTenantIdAndIdNotAndDeletedAtIsNull(String slug, Long tenantId, Long excludeId);
    
    /**
     * Check if slug exists for tenant
     */
    boolean existsBySlugAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);
    
    /**
     * Find active page by ID
     */
    Optional<Page> findByIdAndActiveAndDeletedAtIsNull(Long id, Boolean active);
}
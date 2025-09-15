package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.SeoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SeoMetadata entity
 */
@Repository
public interface SeoMetadataRepository extends JpaRepository<SeoMetadata, Long> {
    
    /**
     * Find SEO metadata by entity type, entity ID, and tenant ID
     */
    Optional<SeoMetadata> findByEntityTypeAndEntityIdAndTenantId(
            String entityType, Long entityId, Long tenantId);
    
    /**
     * Find all SEO metadata for a tenant
     */
    List<SeoMetadata> findByTenantId(Long tenantId);
    
    /**
     * Find SEO metadata by entity type and tenant ID
     */
    List<SeoMetadata> findByEntityTypeAndTenantId(String entityType, Long tenantId);
    
    /**
     * Check if SEO metadata exists for entity
     */
    boolean existsByEntityTypeAndEntityIdAndTenantId(
            String entityType, Long entityId, Long tenantId);
    
    /**
     * Delete SEO metadata by entity type, entity ID, and tenant ID
     */
    void deleteByEntityTypeAndEntityIdAndTenantId(
            String entityType, Long entityId, Long tenantId);
    
    /**
     * Find SEO metadata with specific canonical URL pattern
     */
    @Query("SELECT s FROM SeoMetadata s WHERE s.tenantId = :tenantId AND s.canonicalUrl LIKE :urlPattern")
    List<SeoMetadata> findByTenantIdAndCanonicalUrlLike(
            @Param("tenantId") Long tenantId, 
            @Param("urlPattern") String urlPattern);
    
    /**
     * Count SEO metadata entries by tenant
     */
    long countByTenantId(Long tenantId);
    
    /**
     * Find SEO metadata with non-null structured data
     */
    @Query("SELECT s FROM SeoMetadata s WHERE s.tenantId = :tenantId AND s.structuredData IS NOT NULL")
    List<SeoMetadata> findByTenantIdWithStructuredData(@Param("tenantId") Long tenantId);
    
    /**
     * Find SEO metadata with OpenGraph data
     */
    @Query("SELECT s FROM SeoMetadata s WHERE s.tenantId = :tenantId AND s.openGraphData IS NOT NULL")
    List<SeoMetadata> findByTenantIdWithOpenGraphData(@Param("tenantId") Long tenantId);
    
    /**
     * Find SEO metadata with Twitter Card data
     */
    @Query("SELECT s FROM SeoMetadata s WHERE s.tenantId = :tenantId AND s.twitterCardData IS NOT NULL")
    List<SeoMetadata> findByTenantIdWithTwitterCardData(@Param("tenantId") Long tenantId);
}
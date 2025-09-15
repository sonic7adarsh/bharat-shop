package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.SlugRedirect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SlugRedirect entity
 */
@Repository
public interface SlugRedirectRepository extends JpaRepository<SlugRedirect, Long> {
    
    /**
     * Find redirect by old slug, entity type, and tenant ID
     */
    Optional<SlugRedirect> findByOldSlugAndEntityTypeAndTenantId(
            String oldSlug, String entityType, Long tenantId);
    
    /**
     * Find active redirect by old slug, entity type, and tenant ID
     */
    Optional<SlugRedirect> findByOldSlugAndEntityTypeAndTenantIdAndIsActiveTrue(
            String oldSlug, String entityType, Long tenantId);
    
    /**
     * Find all redirects for a tenant
     */
    List<SlugRedirect> findByTenantId(Long tenantId);
    
    /**
     * Find all redirects for a tenant with pagination
     */
    Page<SlugRedirect> findByTenantId(Long tenantId, Pageable pageable);
    
    /**
     * Find active redirects for a tenant
     */
    List<SlugRedirect> findByTenantIdAndIsActiveTrue(Long tenantId);
    
    /**
     * Find active redirects for a tenant with pagination
     */
    Page<SlugRedirect> findByTenantIdAndIsActiveTrue(Long tenantId, Pageable pageable);
    
    /**
     * Find redirects by tenant and entity type with pagination
     */
    Page<SlugRedirect> findByTenantIdAndEntityType(Long tenantId, String entityType, Pageable pageable);
    
    /**
     * Find redirects by entity type and tenant ID
     */
    List<SlugRedirect> findByEntityTypeAndTenantId(String entityType, Long tenantId);
    
    /**
     * Find redirects by entity ID and tenant ID
     */
    List<SlugRedirect> findByEntityIdAndTenantId(Long entityId, Long tenantId);
    
    /**
     * Find redirects by entity type, entity ID and tenant ID
     */
    List<SlugRedirect> findByEntityTypeAndEntityIdAndTenantId(String entityType, Long entityId, Long tenantId);
    
    /**
     * Find redirects by new slug (to detect redirect chains)
     */
    List<SlugRedirect> findByNewSlugAndEntityTypeAndTenantId(
            String newSlug, String entityType, Long tenantId);
    
    /**
     * Count total redirects by tenant
     */
    long countByTenantId(Long tenantId);
    
    /**
     * Count active redirects by tenant
     */
    long countByTenantIdAndIsActiveTrue(Long tenantId);
    
    /**
     * Sum redirect counts by tenant
     */
    @Query("SELECT COALESCE(SUM(sr.redirectCount), 0) FROM SlugRedirect sr WHERE sr.tenantId = :tenantId")
    long sumRedirectCountByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Find redirects created before a certain date
     */
    List<SlugRedirect> findByTenantIdAndCreatedAtBefore(Long tenantId, LocalDateTime cutoffDate);
    
    /**
     * Delete old redirects
     */
    @Modifying
    @Query("DELETE FROM SlugRedirect sr WHERE sr.tenantId = :tenantId AND sr.createdAt < :cutoffDate")
    int deleteByTenantIdAndCreatedAtBefore(
            @Param("tenantId") Long tenantId, 
            @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find expired redirects
     */
    @Query("SELECT sr FROM SlugRedirect sr WHERE sr.tenantId = :tenantId AND sr.expiresAt IS NOT NULL AND sr.expiresAt < :now")
    List<SlugRedirect> findExpiredRedirects(
            @Param("tenantId") Long tenantId, 
            @Param("now") LocalDateTime now);
    
    /**
     * Find most used redirects
     */
    @Query("SELECT sr FROM SlugRedirect sr WHERE sr.tenantId = :tenantId ORDER BY sr.redirectCount DESC")
    List<SlugRedirect> findMostUsedRedirects(@Param("tenantId") Long tenantId);
    
    /**
     * Find redirects by redirect type
     */
    List<SlugRedirect> findByTenantIdAndRedirectType(Long tenantId, Integer redirectType);
    
    /**
     * Check if old slug exists as redirect
     */
    boolean existsByOldSlugAndEntityTypeAndTenantId(
            String oldSlug, String entityType, Long tenantId);
    
    /**
     * Find redirects that need cleanup (inactive and old)
     */
    @Query("SELECT sr FROM SlugRedirect sr WHERE sr.tenantId = :tenantId AND sr.isActive = false AND sr.createdAt < :cutoffDate")
    List<SlugRedirect> findInactiveOldRedirects(
            @Param("tenantId") Long tenantId, 
            @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Update redirect count
     */
    @Modifying
    @Query("UPDATE SlugRedirect sr SET sr.redirectCount = sr.redirectCount + 1, sr.lastAccessedAt = :now WHERE sr.id = :id")
    void incrementRedirectCount(@Param("id") Long id, @Param("now") LocalDateTime now);
}
package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Vendor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Vendor entity operations.
 * Extends TenantAwareRepository for multi-tenant support.
 */
@Repository
public interface VendorRepository extends TenantAwareRepository<Vendor> {
    
    /**
     * Find vendor by domain name (active vendors only)
     */
    @Query("SELECT v FROM Vendor v WHERE v.domain = :domain AND v.deletedAt IS NULL AND v.isActive = true")
    Optional<Vendor> findByDomain(@Param("domain") String domain);
    
    /**
     * Find vendor by domain name for specific tenant (active vendors only)
     */
    @Query("SELECT v FROM Vendor v WHERE v.domain = :domain AND v.tenantId = :tenantId AND v.deletedAt IS NULL AND v.isActive = true")
    Optional<Vendor> findByDomainAndTenantId(@Param("domain") String domain, @Param("tenantId") UUID tenantId);
    
    /**
     * Check if domain is available (not used by any active vendor)
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN false ELSE true END FROM Vendor v WHERE v.domain = :domain AND v.deletedAt IS NULL")
    boolean isDomainAvailable(@Param("domain") String domain);
    
    /**
     * Find vendor by store name for specific tenant
     */
    @Query("SELECT v FROM Vendor v WHERE v.storeName = :storeName AND v.tenantId = :tenantId AND v.deletedAt IS NULL")
    Optional<Vendor> findByStoreNameAndTenantId(@Param("storeName") String storeName, @Param("tenantId") UUID tenantId);
    
    /**
     * Find active vendors by status
     */
    @Query("SELECT v FROM Vendor v WHERE v.status = :status AND v.deletedAt IS NULL AND v.isActive = true")
    java.util.List<Vendor> findByStatus(@Param("status") Vendor.VendorStatus status);
}
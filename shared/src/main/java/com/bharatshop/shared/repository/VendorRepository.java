package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Vendor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Vendor entity operations.
 * Extends TenantAwareRepository for multi-tenant support.
 */
@Repository
public interface VendorRepository extends TenantAwareRepository<Vendor> {
    
    /**
     * Find vendor by domain name (active vendors only)
     */
    @Query(value = "SELECT * FROM vendors WHERE domain = :domain AND deleted_at IS NULL AND is_active = true", nativeQuery = true)
    Optional<Vendor> findByDomain(@Param("domain") String domain);
    
    /**
     * Find vendor by domain name for specific tenant (active vendors only)
     */
    @Query(value = "SELECT * FROM vendors WHERE domain = ?1 AND tenant_id = ?2 AND deleted_at IS NULL AND is_active = true", nativeQuery = true)
    Optional<Vendor> findByDomainAndTenantId(String domain, String tenantId);
    
    /**
     * Check if domain is available (not used by any active vendor)
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN false ELSE true END FROM vendors WHERE domain = :domain AND deleted_at IS NULL", nativeQuery = true)
    boolean isDomainAvailable(@Param("domain") String domain);
    
    /**
     * Find vendor by store name for specific tenant
     */
    @Query(value = "SELECT * FROM vendors WHERE store_name = ?1 AND tenant_id = ?2 AND deleted_at IS NULL", nativeQuery = true)
    Optional<Vendor> findByStoreNameAndTenantId(String storeName, String tenantId);
    
    /**
     * Find active vendors by status
     */
    @Query(value = "SELECT * FROM vendors WHERE status = :status AND deleted_at IS NULL AND is_active = true", nativeQuery = true)
    java.util.List<Vendor> findByStatus(@Param("status") String status);
}
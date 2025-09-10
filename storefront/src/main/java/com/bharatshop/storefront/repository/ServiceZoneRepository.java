package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.ServiceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ServiceZone entity operations.
 * Provides methods for querying service zones by pincode ranges and tenant.
 */
@Repository
public interface ServiceZoneRepository extends JpaRepository<ServiceZone, Long> {

    /**
     * Find all service zones for a tenant (active only)
     */
    List<ServiceZone> findByTenantIdAndDeletedAtIsNullOrderByName(Long tenantId);

    /**
     * Find service zone by ID and tenant (active only)
     */
    Optional<ServiceZone> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    /**
     * Find service zones that contain the given pincode in their explicit pincode list
     */
    @Query("SELECT sz FROM ServiceZone sz WHERE sz.tenantId = :tenantId " +
           "AND sz.deletedAt IS NULL " +
           "AND sz.explicitPincodes IS NOT NULL " +
           "AND sz.explicitPincodes LIKE CONCAT('%', :pincode, '%') " +
           "ORDER BY sz.priority ASC, sz.name ASC")
    List<ServiceZone> findByTenantIdAndExplicitPincodesContaining(@Param("tenantId") Long tenantId, 
                                                                  @Param("pincode") String pincode);

    /**
     * Find service zones where the pincode falls within the range (pinFrom to pinTo)
     */
    @Query("SELECT sz FROM ServiceZone sz WHERE sz.tenantId = :tenantId " +
           "AND sz.deletedAt IS NULL " +
           "AND sz.pinFrom IS NOT NULL AND sz.pinTo IS NOT NULL " +
           "AND :pincode >= sz.pinFrom AND :pincode <= sz.pinTo " +
           "ORDER BY sz.priority ASC, sz.name ASC")
    List<ServiceZone> findByTenantIdAndPincodeRange(@Param("tenantId") Long tenantId, 
                                                    @Param("pincode") String pincode);

    /**
     * Find service zones by name for a tenant
     */
    List<ServiceZone> findByTenantIdAndNameContainingIgnoreCaseAndDeletedAtIsNull(Long tenantId, String name);

    /**
     * Find service zones that support COD for a tenant
     */
    List<ServiceZone> findByTenantIdAndCodAllowedTrueAndDeletedAtIsNullOrderByName(Long tenantId);

    /**
     * Find service zones that support express delivery for a tenant
     */
    List<ServiceZone> findByTenantIdAndExpressDeliveryAvailableTrueAndDeletedAtIsNullOrderByName(Long tenantId);

    /**
     * Check if any service zone covers the given pincode for a tenant
     */
    @Query("SELECT COUNT(sz) > 0 FROM ServiceZone sz WHERE sz.tenantId = :tenantId " +
           "AND sz.deletedAt IS NULL " +
           "AND (" +
           "  (sz.explicitPincodes IS NOT NULL AND sz.explicitPincodes LIKE CONCAT('%', :pincode, '%')) " +
           "  OR (sz.pinFrom IS NOT NULL AND sz.pinTo IS NOT NULL AND :pincode >= sz.pinFrom AND :pincode <= sz.pinTo)" +
           ")")
    boolean existsByTenantIdAndPincodeInRange(@Param("tenantId") Long tenantId, 
                                              @Param("pincode") String pincode);

    /**
     * Find service zones with the lowest shipping cost for a tenant
     */
    @Query("SELECT sz FROM ServiceZone sz WHERE sz.tenantId = :tenantId " +
           "AND sz.deletedAt IS NULL " +
           "ORDER BY sz.baseRate ASC, sz.perKgRate ASC")
    List<ServiceZone> findByTenantIdOrderByLowestCost(Long tenantId);

    /**
     * Find service zones with the fastest delivery for a tenant
     */
    List<ServiceZone> findByTenantIdAndDeletedAtIsNullOrderBySlaDaysAsc(Long tenantId);

    /**
     * Find service zones by priority for a tenant
     */
    List<ServiceZone> findByTenantIdAndDeletedAtIsNullOrderByPriorityAscNameAsc(Long tenantId);

    /**
     * Count active service zones for a tenant
     */
    Long countByTenantIdAndDeletedAtIsNull(Long tenantId);

    /**
     * Find service zones that need rate updates (no rates defined)
     */
    @Query("SELECT sz FROM ServiceZone sz WHERE sz.tenantId = :tenantId " +
           "AND sz.deletedAt IS NULL " +
           "AND (sz.baseRate IS NULL OR sz.perKgRate IS NULL OR sz.minCharge IS NULL)")
    List<ServiceZone> findByTenantIdAndIncompleteRates(Long tenantId);

    /**
     * Find overlapping service zones (same pincode ranges)
     */
    @Query("SELECT sz1 FROM ServiceZone sz1, ServiceZone sz2 WHERE sz1.tenantId = :tenantId " +
           "AND sz2.tenantId = :tenantId " +
           "AND sz1.id != sz2.id " +
           "AND sz1.deletedAt IS NULL AND sz2.deletedAt IS NULL " +
           "AND sz1.pinFrom IS NOT NULL AND sz1.pinTo IS NOT NULL " +
           "AND sz2.pinFrom IS NOT NULL AND sz2.pinTo IS NOT NULL " +
           "AND NOT (sz1.pinTo < sz2.pinFrom OR sz1.pinFrom > sz2.pinTo)")
    List<ServiceZone> findOverlappingZones(@Param("tenantId") Long tenantId);

    /**
     * Find service zones by SLA days range
     */
    List<ServiceZone> findByTenantIdAndSlaDaysBetweenAndDeletedAtIsNull(Long tenantId, Integer minDays, Integer maxDays);

    /**
     * Find service zones with specific carrier support
     */
    @Query("SELECT sz FROM ServiceZone sz WHERE sz.tenantId = :tenantId " +
           "AND sz.deletedAt IS NULL " +
           "AND sz.preferredCarriers IS NOT NULL " +
           "AND sz.preferredCarriers LIKE CONCAT('%', :carrierCode, '%')")
    List<ServiceZone> findByTenantIdAndCarrierSupport(@Param("tenantId") Long tenantId, 
                                                      @Param("carrierCode") String carrierCode);
}
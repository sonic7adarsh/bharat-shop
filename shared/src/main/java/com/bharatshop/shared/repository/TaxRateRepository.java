package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.TaxRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {

    /**
     * Find tax rate by tenant, HSN code, state, tax type and status
     */
    Optional<TaxRate> findByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndStatus(
            Long tenantId, String hsnCode, String stateCode, 
            TaxRate.TaxType taxType, TaxRate.TaxRateStatus status);

    /**
     * Find tax rate by ID and tenant ID
     */
    Optional<TaxRate> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Find all tax rates for a tenant
     */
    Page<TaxRate> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

    /**
     * Find tax rates by HSN code for a tenant
     */
    List<TaxRate> findByTenantIdAndHsnCodeAndDeletedAtIsNull(Long tenantId, String hsnCode);

    /**
     * Find tax rates by state for a tenant
     */
    List<TaxRate> findByTenantIdAndStateCodeAndDeletedAtIsNull(Long tenantId, String stateCode);

    /**
     * Find active tax rates for a tenant
     */
    List<TaxRate> findByTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, TaxRate.TaxRateStatus status);

    /**
     * Check if tax rate exists for tenant, HSN, state and tax type
     */
    boolean existsByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndDeletedAtIsNull(
            Long tenantId, String hsnCode, String stateCode, TaxRate.TaxType taxType);

    /**
     * Search tax rates by HSN code pattern
     */
    @Query("SELECT tr FROM TaxRate tr WHERE tr.tenantId = :tenantId " +
           "AND tr.hsnCode LIKE %:hsnCode% AND tr.deletedAt IS NULL")
    List<TaxRate> searchByHsnCode(@Param("tenantId") Long tenantId, @Param("hsnCode") String hsnCode);

    /**
     * Find tax rates by multiple HSN codes
     */
    @Query("SELECT tr FROM TaxRate tr WHERE tr.tenantId = :tenantId " +
           "AND tr.hsnCode IN :hsnCodes AND tr.status = :status AND tr.deletedAt IS NULL")
    List<TaxRate> findByTenantIdAndHsnCodesAndStatus(
            @Param("tenantId") Long tenantId, 
            @Param("hsnCodes") List<String> hsnCodes, 
            @Param("status") TaxRate.TaxRateStatus status);

    /**
     * Find distinct HSN codes for active tax rates by tenant
     */
    @Query("SELECT DISTINCT tr.hsnCode FROM TaxRate tr WHERE tr.tenantId = :tenantId " +
           "AND tr.status = 'ACTIVE' AND tr.deletedAt IS NULL ORDER BY tr.hsnCode")
    List<String> findDistinctHsnCodesByTenantIdAndStatusActive(@Param("tenantId") Long tenantId);

    /**
     * Find distinct state codes for active tax rates by tenant
     */
    @Query("SELECT DISTINCT tr.stateCode FROM TaxRate tr WHERE tr.tenantId = :tenantId " +
           "AND tr.status = 'ACTIVE' AND tr.deletedAt IS NULL ORDER BY tr.stateCode")
    List<String> findDistinctStateCodesByTenantIdAndStatusActive(@Param("tenantId") Long tenantId);

    /**
     * Find tax rates by tenant with filters
     */
    @Query("SELECT tr FROM TaxRate tr WHERE tr.tenantId = :tenantId " +
           "AND (:hsnCode IS NULL OR tr.hsnCode = :hsnCode) " +
           "AND (:stateCode IS NULL OR tr.stateCode = :stateCode) " +
           "AND (:taxType IS NULL OR tr.taxType = :taxType) " +
           "AND (:isActive IS NULL OR (CASE WHEN :isActive = true THEN tr.status = 'ACTIVE' ELSE tr.status = 'INACTIVE' END)) " +
           "AND tr.deletedAt IS NULL")
    Page<TaxRate> findByTenantIdAndFilters(
            @Param("tenantId") Long tenantId,
            @Param("hsnCode") String hsnCode,
            @Param("stateCode") String stateCode,
            @Param("taxType") String taxType,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
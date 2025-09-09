package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.InvoiceNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for InvoiceNumberSequence entity
 */
@Repository
public interface InvoiceNumberSequenceRepository extends JpaRepository<InvoiceNumberSequence, Long> {

    /**
     * Find sequence by ID and tenant ID (not deleted)
     */
    Optional<InvoiceNumberSequence> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    /**
     * Find sequence by tenant ID and financial year (not deleted)
     */
    Optional<InvoiceNumberSequence> findByTenantIdAndFinancialYearAndDeletedAtIsNull(
            Long tenantId, String financialYear);

    /**
     * Find all sequences for a tenant (not deleted)
     */
    List<InvoiceNumberSequence> findByTenantIdAndDeletedAtIsNullOrderByFinancialYearDesc(Long tenantId);

    /**
     * Find sequences by prefix and tenant ID (not deleted)
     */
    List<InvoiceNumberSequence> findByPrefixAndTenantIdAndDeletedAtIsNullOrderByFinancialYearDesc(
            String prefix, Long tenantId);

    /**
     * Check if sequence exists for tenant and financial year
     */
    boolean existsByTenantIdAndFinancialYearAndDeletedAtIsNull(Long tenantId, String financialYear);

    /**
     * Get current number for a sequence
     */
    @Query("SELECT ins.currentNumber FROM InvoiceNumberSequence ins " +
           "WHERE ins.tenantId = :tenantId AND ins.financialYear = :financialYear " +
           "AND ins.deletedAt IS NULL")
    Optional<Long> getCurrentNumber(@Param("tenantId") Long tenantId, 
                                   @Param("financialYear") String financialYear);

    /**
     * Get next number for a sequence (increment and return)
     */
    @Query("UPDATE InvoiceNumberSequence ins " +
           "SET ins.currentNumber = ins.currentNumber + 1 " +
           "WHERE ins.tenantId = :tenantId AND ins.financialYear = :financialYear " +
           "AND ins.deletedAt IS NULL")
    void incrementCurrentNumber(@Param("tenantId") Long tenantId, 
                               @Param("financialYear") String financialYear);

    /**
     * Reset sequence to a specific number
     */
    @Query("UPDATE InvoiceNumberSequence ins " +
           "SET ins.currentNumber = :newNumber " +
           "WHERE ins.tenantId = :tenantId AND ins.financialYear = :financialYear " +
           "AND ins.deletedAt IS NULL")
    void resetSequence(@Param("tenantId") Long tenantId, 
                      @Param("financialYear") String financialYear,
                      @Param("newNumber") Long newNumber);

    /**
     * Find active sequences (current financial year)
     */
    @Query("SELECT ins FROM InvoiceNumberSequence ins " +
           "WHERE ins.tenantId = :tenantId " +
           "AND ins.financialYear = :currentFinancialYear " +
           "AND ins.deletedAt IS NULL")
    List<InvoiceNumberSequence> findActiveSequences(
            @Param("tenantId") Long tenantId,
            @Param("currentFinancialYear") String currentFinancialYear);
}
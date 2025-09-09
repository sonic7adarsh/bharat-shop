package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Invoice entity
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by ID and tenant ID (not deleted)
     */
    Optional<Invoice> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    /**
     * Find invoice by order ID and tenant ID (not deleted)
     */
    Optional<Invoice> findByOrderIdAndTenantIdAndDeletedAtIsNull(Long orderId, Long tenantId);

    /**
     * Find invoice by invoice number and tenant ID (not deleted)
     */
    Optional<Invoice> findByInvoiceNumberAndTenantIdAndDeletedAtIsNull(String invoiceNumber, Long tenantId);

    /**
     * Find all invoices for a tenant (not deleted)
     */
    List<Invoice> findByTenantIdAndDeletedAtIsNullOrderByInvoiceDateDesc(Long tenantId);

    /**
     * Find invoices by status and tenant ID (not deleted)
     */
    List<Invoice> findByStatusAndTenantIdAndDeletedAtIsNullOrderByInvoiceDateDesc(
            Invoice.InvoiceStatus status, Long tenantId);

    /**
     * Find invoices by date range and tenant ID (not deleted)
     */
    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND i.deletedAt IS NULL " +
           "ORDER BY i.invoiceDate DESC")
    List<Invoice> findByDateRangeAndTenantId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId);

    /**
     * Find overdue invoices (past due date and not paid)
     */
    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId " +
           "AND i.dueDate < :currentDate " +
           "AND i.status NOT IN ('PAID', 'CANCELLED') " +
           "AND i.deletedAt IS NULL " +
           "ORDER BY i.dueDate ASC")
    List<Invoice> findOverdueInvoices(
            @Param("currentDate") LocalDate currentDate,
            @Param("tenantId") Long tenantId);

    /**
     * Check if invoice number exists for tenant
     */
    boolean existsByInvoiceNumberAndTenantIdAndDeletedAtIsNull(String invoiceNumber, Long tenantId);

    /**
     * Count invoices by status and tenant ID
     */
    long countByStatusAndTenantIdAndDeletedAtIsNull(Invoice.InvoiceStatus status, Long tenantId);

    /**
     * Find invoices by buyer email and tenant ID (not deleted)
     */
    List<Invoice> findByBuyerEmailAndTenantIdAndDeletedAtIsNullOrderByInvoiceDateDesc(
            String buyerEmail, Long tenantId);

    /**
     * Find invoices by GSTIN and tenant ID (not deleted)
     */
    List<Invoice> findByBuyerGstinAndTenantIdAndDeletedAtIsNullOrderByInvoiceDateDesc(
            String buyerGstin, Long tenantId);
}
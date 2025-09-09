package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for InvoiceItem entity
 */
@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * Find invoice item by ID and tenant ID (not deleted)
     */
    Optional<InvoiceItem> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    /**
     * Find all invoice items for an invoice (not deleted)
     */
    List<InvoiceItem> findByInvoiceIdAndDeletedAtIsNullOrderByIdAsc(Long invoiceId);

    /**
     * Find all invoice items for an invoice and tenant (not deleted)
     */
    List<InvoiceItem> findByInvoiceIdAndTenantIdAndDeletedAtIsNullOrderByIdAsc(Long invoiceId, Long tenantId);

    /**
     * Find invoice items by product ID and tenant ID (not deleted)
     */
    List<InvoiceItem> findByProductIdAndTenantIdAndDeletedAtIsNullOrderByIdDesc(Long productId, Long tenantId);

    /**
     * Find invoice items by order item ID and tenant ID (not deleted)
     */
    List<InvoiceItem> findByOrderItemIdAndTenantIdAndDeletedAtIsNull(Long orderItemId, Long tenantId);

    /**
     * Find invoice items by HSN code and tenant ID (not deleted)
     */
    List<InvoiceItem> findByHsnCodeAndTenantIdAndDeletedAtIsNullOrderByIdDesc(String hsnCode, Long tenantId);

    /**
     * Count invoice items for an invoice
     */
    long countByInvoiceIdAndDeletedAtIsNull(Long invoiceId);

    /**
     * Get tax summary by HSN code for an invoice
     */
    @Query("SELECT ii.hsnCode, SUM(ii.netAmount), SUM(ii.cgstAmount), SUM(ii.sgstAmount), " +
           "SUM(ii.igstAmount), SUM(ii.cessAmount), SUM(ii.totalTaxAmount) " +
           "FROM InvoiceItem ii " +
           "WHERE ii.invoiceId = :invoiceId AND ii.deletedAt IS NULL " +
           "GROUP BY ii.hsnCode " +
           "ORDER BY ii.hsnCode")
    List<Object[]> getTaxSummaryByHsnCode(@Param("invoiceId") Long invoiceId);

    /**
     * Get tax summary for all items in an invoice
     */
    @Query("SELECT SUM(ii.netAmount), SUM(ii.cgstAmount), SUM(ii.sgstAmount), " +
           "SUM(ii.igstAmount), SUM(ii.cessAmount), SUM(ii.totalTaxAmount), SUM(ii.totalAmount) " +
           "FROM InvoiceItem ii " +
           "WHERE ii.invoiceId = :invoiceId AND ii.deletedAt IS NULL")
    Object[] getInvoiceTotals(@Param("invoiceId") Long invoiceId);

    /**
     * Delete all items for an invoice (soft delete)
     */
    @Query("UPDATE InvoiceItem ii SET ii.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE ii.invoiceId = :invoiceId AND ii.tenantId = :tenantId")
    void deleteByInvoiceIdAndTenantId(@Param("invoiceId") Long invoiceId, @Param("tenantId") Long tenantId);
}
package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReturnRequest entity operations.
 * Provides methods for querying return requests with various filters.
 */
@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * Find return request by ID and tenant ID
     */
    Optional<ReturnRequest> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Find return request by return number and tenant ID
     */
    Optional<ReturnRequest> findByReturnNumberAndTenantId(String returnNumber, Long tenantId);

    /**
     * Find all return requests for a tenant, ordered by creation date
     */
    Page<ReturnRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    /**
     * Find return requests by tenant and status
     */
    Page<ReturnRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(
            Long tenantId, ReturnRequest.ReturnStatus status, Pageable pageable);

    /**
     * Find return requests by order ID
     */
    List<ReturnRequest> findByOrderIdAndTenantId(Long orderId, Long tenantId);

    /**
     * Find return requests by customer ID
     */
    Page<ReturnRequest> findByCustomerIdAndTenantIdOrderByCreatedAtDesc(
            Long customerId, Long tenantId, Pageable pageable);

    /**
     * Find return requests by customer and status
     */
    Page<ReturnRequest> findByCustomerIdAndTenantIdAndStatusOrderByCreatedAtDesc(
            Long customerId, Long tenantId, ReturnRequest.ReturnStatus status, Pageable pageable);

    /**
     * Find return requests created within date range
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.requestedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY rr.requestedAt DESC")
    Page<ReturnRequest> findByTenantIdAndRequestedAtBetween(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find return requests by return type
     */
    Page<ReturnRequest> findByTenantIdAndReturnTypeOrderByCreatedAtDesc(
            Long tenantId, ReturnRequest.ReturnType returnType, Pageable pageable);

    /**
     * Find pending return requests (for admin review)
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status = 'PENDING' ORDER BY rr.requestedAt ASC")
    Page<ReturnRequest> findPendingReturnRequests(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * Find return requests requiring pickup
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status IN ('APPROVED', 'PICKUP_SCHEDULED') " +
           "ORDER BY rr.approvedAt ASC")
    Page<ReturnRequest> findReturnRequestsRequiringPickup(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * Find return requests in quality check
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status IN ('RECEIVED', 'QUALITY_CHECK') " +
           "ORDER BY rr.pickupCompletedAt ASC")
    Page<ReturnRequest> findReturnRequestsInQualityCheck(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * Find return requests ready for refund
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status = 'QUALITY_APPROVED' " +
           "ORDER BY rr.qualityCheckCompletedAt ASC")
    Page<ReturnRequest> findReturnRequestsReadyForRefund(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * Count return requests by status
     */
    @Query("SELECT COUNT(rr) FROM ReturnRequest rr WHERE rr.tenantId = :tenantId AND rr.status = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") ReturnRequest.ReturnStatus status);

    /**
     * Count total return requests for tenant
     */
    Long countByTenantId(Long tenantId);

    /**
     * Find return requests with overdue pickup (approved but not picked up within timeframe)
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status IN ('APPROVED', 'PICKUP_SCHEDULED') " +
           "AND rr.approvedAt < :overdueDate " +
           "ORDER BY rr.approvedAt ASC")
    List<ReturnRequest> findOverduePickupRequests(
            @Param("tenantId") Long tenantId,
            @Param("overdueDate") LocalDateTime overdueDate);

    /**
     * Find return requests with overdue quality check
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status IN ('RECEIVED', 'QUALITY_CHECK') " +
           "AND rr.pickupCompletedAt < :overdueDate " +
           "ORDER BY rr.pickupCompletedAt ASC")
    List<ReturnRequest> findOverdueQualityCheckRequests(
            @Param("tenantId") Long tenantId,
            @Param("overdueDate") LocalDateTime overdueDate);

    /**
     * Get return request statistics for dashboard
     */
    @Query("SELECT new map(" +
           "COUNT(CASE WHEN rr.status = 'PENDING' THEN 1 END) as pending, " +
           "COUNT(CASE WHEN rr.status = 'APPROVED' THEN 1 END) as approved, " +
           "COUNT(CASE WHEN rr.status = 'PICKUP_SCHEDULED' THEN 1 END) as pickupScheduled, " +
           "COUNT(CASE WHEN rr.status = 'QUALITY_CHECK' THEN 1 END) as qualityCheck, " +
           "COUNT(CASE WHEN rr.status = 'QUALITY_APPROVED' THEN 1 END) as readyForRefund, " +
           "COUNT(CASE WHEN rr.status = 'COMPLETED' THEN 1 END) as completed, " +
           "COUNT(CASE WHEN rr.status = 'REJECTED' THEN 1 END) as rejected" +
           ") FROM ReturnRequest rr WHERE rr.tenantId = :tenantId")
    Object getReturnRequestStatistics(@Param("tenantId") Long tenantId);

    /**
     * Find return requests by multiple statuses
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND rr.status IN :statuses ORDER BY rr.requestedAt DESC")
    Page<ReturnRequest> findByTenantIdAndStatusIn(
            @Param("tenantId") Long tenantId,
            @Param("statuses") List<ReturnRequest.ReturnStatus> statuses,
            Pageable pageable);

    /**
     * Check if order has any active return requests
     */
    @Query("SELECT COUNT(rr) > 0 FROM ReturnRequest rr WHERE rr.orderId = :orderId " +
           "AND rr.tenantId = :tenantId AND rr.status NOT IN ('COMPLETED', 'REJECTED', 'CANCELLED')")
    boolean hasActiveReturnRequest(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId);

    /**
     * Find return requests that need automatic status updates (for scheduled jobs)
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.tenantId = :tenantId " +
           "AND ((rr.status = 'PICKUP_SCHEDULED' AND rr.pickupScheduledAt < :autoPickupDate) " +
           "OR (rr.status = 'IN_TRANSIT' AND rr.pickupCompletedAt < :autoReceiveDate))")
    List<ReturnRequest> findReturnRequestsForAutoStatusUpdate(
            @Param("tenantId") Long tenantId,
            @Param("autoPickupDate") LocalDateTime autoPickupDate,
            @Param("autoReceiveDate") LocalDateTime autoReceiveDate);

    /**
     * Find return requests by search criteria (return number, customer name, order ID)
     */
    @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
           "LEFT JOIN rr.customer c " +
           "LEFT JOIN rr.order o " +
           "WHERE rr.tenantId = :tenantId " +
           "AND (LOWER(rr.returnNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR CAST(o.id AS string) LIKE CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY rr.requestedAt DESC")
    Page<ReturnRequest> searchReturnRequests(
            @Param("tenantId") Long tenantId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
}
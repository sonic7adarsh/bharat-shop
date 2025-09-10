package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Shipment entity operations
 * Provides data access methods for shipment management and tracking
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /**
     * Find shipment by tracking number
     */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * Find shipments by order ID
     */
    List<Shipment> findByOrderId(Long orderId);

    /**
     * Find shipments by tenant ID
     */
    List<Shipment> findByTenantId(Long tenantId);

    /**
     * Find shipments by carrier name
     */
    List<Shipment> findByCarrierName(String carrierName);

    /**
     * Find shipments by status
     */
    List<Shipment> findByStatus(Shipment.ShipmentStatus status);

    /**
     * Find shipments by status and tenant
     */
    List<Shipment> findByStatusAndTenantId(Shipment.ShipmentStatus status, Long tenantId);

    /**
     * Find active shipments for polling (used by tracking scheduler)
     */
    @Query("SELECT s FROM Shipment s WHERE s.status IN :statuses " +
           "AND (s.lastTrackedAt IS NULL OR s.lastTrackedAt < :lastTrackedBefore) " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.lastTrackedAt ASC NULLS FIRST")
    List<Shipment> findActiveShipmentsForPolling(
            @Param("statuses") List<Shipment.ShipmentStatus> statuses,
            @Param("lastTrackedBefore") LocalDateTime lastTrackedBefore,
            @Param("limit") int limit);

    /**
     * Find shipments that need status sync with orders
     */
    @Query("SELECT s FROM Shipment s " +
           "JOIN Orders o ON s.orderId = o.id " +
           "WHERE s.status = 'DELIVERED' AND o.status != 'DELIVERED' " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.deliveredAt DESC")
    List<Shipment> findShipmentsNeedingStatusSync(@Param("limit") int limit);

    /**
     * Find shipments with exceptions that need handling
     */
    @Query("SELECT s FROM Shipment s WHERE s.status = 'EXCEPTION' " +
           "AND s.exceptionAt > :since " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.exceptionAt DESC")
    List<Shipment> findShipmentsWithExceptions(@Param("limit") int limit);

    /**
     * Find shipments created between dates
     */
    @Query("SELECT s FROM Shipment s WHERE s.createdAt BETWEEN :startDate AND :endDate " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.createdAt DESC")
    List<Shipment> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find shipments delivered between dates
     */
    @Query("SELECT s FROM Shipment s WHERE s.deliveredAt BETWEEN :startDate AND :endDate " +
           "AND s.status = 'DELIVERED' " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.deliveredAt DESC")
    List<Shipment> findDeliveredBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find overdue shipments (past expected delivery date)
     */
    @Query("SELECT s FROM Shipment s WHERE s.expectedDeliveryDate < :currentDate " +
           "AND s.status NOT IN ('DELIVERED', 'RETURNED', 'CANCELLED') " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.expectedDeliveryDate ASC")
    List<Shipment> findOverdueShipments(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Count active shipments
     */
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status IN ('SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY') " +
           "AND s.deletedAt IS NULL")
    long countActiveShipments();

    /**
     * Count shipments delivered today
     */
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = 'DELIVERED' " +
           "AND DATE(s.deliveredAt) = CURRENT_DATE " +
           "AND s.deletedAt IS NULL")
    long countDeliveredToday();

    /**
     * Count shipments pending tracking updates
     */
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status IN ('SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY') " +
           "AND (s.lastTrackedAt IS NULL OR s.lastTrackedAt < :cutoffTime) " +
           "AND s.deletedAt IS NULL")
    long countPendingUpdates(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count shipments with exceptions
     */
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = 'EXCEPTION' " +
           "AND s.deletedAt IS NULL")
    long countExceptions();

    /**
     * Get average delivery days for completed shipments
     */
    @Query("SELECT AVG(DATEDIFF(s.deliveredAt, s.shippedAt)) FROM Shipment s " +
           "WHERE s.status = 'DELIVERED' " +
           "AND s.shippedAt IS NOT NULL " +
           "AND s.deliveredAt IS NOT NULL " +
           "AND s.deliveredAt > :since " +
           "AND s.deletedAt IS NULL")
    Double getAverageDeliveryDays(@Param("since") LocalDateTime since);

    /**
     * Get average delivery days (overloaded for current period)
     */
    default double getAverageDeliveryDays() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Double avg = getAverageDeliveryDays(thirtyDaysAgo);
        return avg != null ? avg : 0.0;
    }

    /**
     * Get count of pending updates (overloaded for current time)
     */
    default long countPendingUpdates() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return countPendingUpdates(oneHourAgo);
    }

    /**
     * Find shipments by multiple tracking numbers
     */
    @Query("SELECT s FROM Shipment s WHERE s.trackingNumber IN :trackingNumbers " +
           "AND s.deletedAt IS NULL")
    List<Shipment> findByTrackingNumberIn(@Param("trackingNumbers") List<String> trackingNumbers);

    /**
     * Find shipments by carrier and status
     */
    @Query("SELECT s FROM Shipment s WHERE s.carrierName = :carrierName " +
           "AND s.status = :status " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.createdAt DESC")
    List<Shipment> findByCarrierNameAndStatus(
            @Param("carrierName") String carrierName,
            @Param("status") Shipment.ShipmentStatus status);

    /**
     * Find shipments requiring SLA monitoring
     */
    @Query("SELECT s FROM Shipment s " +
           "JOIN Orders o ON s.orderId = o.id " +
           "WHERE s.status IN ('SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY') " +
           "AND o.promisedDeliveryDate < :currentDate " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY o.promisedDeliveryDate ASC")
    List<Shipment> findShipmentsBreachingSLA(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Update last tracked timestamp for multiple shipments
     */
    @Query("UPDATE Shipment s SET s.lastTrackedAt = :timestamp " +
           "WHERE s.id IN :shipmentIds")
    void updateLastTrackedAt(
            @Param("shipmentIds") List<Long> shipmentIds,
            @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find shipments by service zone
     */
    @Query("SELECT s FROM Shipment s " +
           "JOIN Orders o ON s.orderId = o.id " +
           "WHERE o.serviceZoneId = :serviceZoneId " +
           "AND s.deletedAt IS NULL " +
           "ORDER BY s.createdAt DESC")
    List<Shipment> findByServiceZone(@Param("serviceZoneId") Long serviceZoneId);

    /**
     * Get shipment performance metrics by carrier
     */
    @Query("SELECT s.carrierName, " +
           "COUNT(s) as totalShipments, " +
           "SUM(CASE WHEN s.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredCount, " +
           "AVG(CASE WHEN s.status = 'DELIVERED' THEN DATEDIFF(s.deliveredAt, s.shippedAt) ELSE NULL END) as avgDeliveryDays " +
           "FROM Shipment s " +
           "WHERE s.createdAt >= :since " +
           "AND s.deletedAt IS NULL " +
           "GROUP BY s.carrierName")
    List<Object[]> getCarrierPerformanceMetrics(@Param("since") LocalDateTime since);

    /**
     * Check if tracking number exists
     */
    boolean existsByTrackingNumber(String trackingNumber);

    /**
     * Check if tracking number exists for different shipment
     */
    @Query("SELECT COUNT(s) > 0 FROM Shipment s WHERE s.trackingNumber = :trackingNumber " +
           "AND s.id != :shipmentId " +
           "AND s.deletedAt IS NULL")
    boolean existsByTrackingNumberAndIdNot(
            @Param("trackingNumber") String trackingNumber,
            @Param("shipmentId") Long shipmentId);
}
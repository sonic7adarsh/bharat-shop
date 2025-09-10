package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ShipmentTracking entity operations
 * Provides data access methods for shipment tracking events and history
 */
@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    /**
     * Find all tracking events for a shipment, ordered by event date
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByShipmentIdOrderByEventDateDesc(@Param("shipmentId") Long shipmentId);

    /**
     * Find tracking events by shipment tracking number
     */
    @Query("SELECT st FROM ShipmentTracking st " +
           "JOIN st.shipment s " +
           "WHERE s.trackingNumber = :trackingNumber " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByTrackingNumber(@Param("trackingNumber") String trackingNumber);

    /**
     * Find latest tracking event for a shipment
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC, st.createdAt DESC " +
           "LIMIT 1")
    Optional<ShipmentTracking> findLatestByShipmentId(@Param("shipmentId") Long shipmentId);

    /**
     * Find tracking event by shipment, event date and status (for duplicate detection)
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.eventDate = :eventDate " +
           "AND st.status = :status " +
           "AND st.deletedAt IS NULL")
    Optional<ShipmentTracking> findByShipmentIdAndEventDateAndStatus(
            @Param("shipmentId") Long shipmentId,
            @Param("eventDate") LocalDateTime eventDate,
            @Param("status") String status);

    /**
     * Find milestone events for a shipment
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.isMilestone = true " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findMilestonesByShipmentId(@Param("shipmentId") Long shipmentId);

    /**
     * Find exception events for a shipment
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.isException = true " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findExceptionsByShipmentId(@Param("shipmentId") Long shipmentId);

    /**
     * Find tracking events by status
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.status = :status " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByStatus(@Param("status") String status);

    /**
     * Find tracking events by event type
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.eventType = :eventType " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByEventType(@Param("eventType") String eventType);

    /**
     * Find tracking events by location
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE LOWER(st.location) LIKE LOWER(CONCAT('%', :location, '%')) " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByLocationContaining(@Param("location") String location);

    /**
     * Find tracking events created between dates
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.eventDate BETWEEN :startDate AND :endDate " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByEventDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find tracking events by source (API_POLL, WEBHOOK, MANUAL)
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.source = :source " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findBySource(@Param("source") String source);

    /**
     * Find tracking events by carrier event code
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.carrierEventCode = :carrierEventCode " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByCarrierEventCode(@Param("carrierEventCode") String carrierEventCode);

    /**
     * Find recent tracking events (last 24 hours)
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.eventDate >= :since " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findRecentEvents(@Param("since") LocalDateTime since);

    /**
     * Find tracking events for multiple shipments
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id IN :shipmentIds " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.shipment.id, st.eventDate DESC")
    List<ShipmentTracking> findByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    /**
     * Count tracking events for a shipment
     */
    @Query("SELECT COUNT(st) FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.deletedAt IS NULL")
    long countByShipmentId(@Param("shipmentId") Long shipmentId);

    /**
     * Count milestone events for a shipment
     */
    @Query("SELECT COUNT(st) FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.isMilestone = true " +
           "AND st.deletedAt IS NULL")
    long countMilestonesByShipmentId(@Param("shipmentId") Long shipmentId);

    /**
     * Count exception events for a shipment
     */
    @Query("SELECT COUNT(st) FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.isException = true " +
           "AND st.deletedAt IS NULL")
    long countExceptionsByShipmentId(@Param("shipmentId") Long shipmentId);

    /**
     * Delete old tracking events (cleanup)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ShipmentTracking st SET st.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE st.eventDate < :cutoffDate " +
           "AND st.deletedAt IS NULL")
    int deleteOldTrackingEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find tracking events that need notification processing
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.isMilestone = true " +
           "AND st.eventDate >= :since " +
           "AND st.status IN ('DELIVERED', 'OUT_FOR_DELIVERY', 'EXCEPTION') " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findEventsNeedingNotification(@Param("since") LocalDateTime since);

    /**
     * Find duplicate tracking events (same shipment, status, and event date)
     */
    @Query("SELECT st FROM ShipmentTracking st WHERE st.shipment.id = :shipmentId " +
           "AND st.status = :status " +
           "AND st.eventDate = :eventDate " +
           "AND st.id != :excludeId " +
           "AND st.deletedAt IS NULL")
    List<ShipmentTracking> findDuplicateEvents(
            @Param("shipmentId") Long shipmentId,
            @Param("status") String status,
            @Param("eventDate") LocalDateTime eventDate,
            @Param("excludeId") Long excludeId);

    /**
     * Get tracking statistics by status
     */
    @Query("SELECT st.status, COUNT(st) FROM ShipmentTracking st " +
           "WHERE st.eventDate >= :since " +
           "AND st.deletedAt IS NULL " +
           "GROUP BY st.status " +
           "ORDER BY COUNT(st) DESC")
    List<Object[]> getTrackingStatsByStatus(@Param("since") LocalDateTime since);

    /**
     * Get tracking statistics by event type
     */
    @Query("SELECT st.eventType, COUNT(st) FROM ShipmentTracking st " +
           "WHERE st.eventDate >= :since " +
           "AND st.deletedAt IS NULL " +
           "GROUP BY st.eventType " +
           "ORDER BY COUNT(st) DESC")
    List<Object[]> getTrackingStatsByEventType(@Param("since") LocalDateTime since);

    /**
     * Get tracking statistics by source
     */
    @Query("SELECT st.source, COUNT(st) FROM ShipmentTracking st " +
           "WHERE st.eventDate >= :since " +
           "AND st.deletedAt IS NULL " +
           "GROUP BY st.source " +
           "ORDER BY COUNT(st) DESC")
    List<Object[]> getTrackingStatsBySource(@Param("since") LocalDateTime since);

    /**
     * Find tracking events by carrier name (through shipment)
     */
    @Query("SELECT st FROM ShipmentTracking st " +
           "JOIN st.shipment s " +
           "WHERE s.carrierName = :carrierName " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByCarrierName(@Param("carrierName") String carrierName);

    /**
     * Find tracking events by tenant (through shipment)
     */
    @Query("SELECT st FROM ShipmentTracking st " +
           "JOIN st.shipment s " +
           "WHERE s.tenantId = :tenantId " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Find latest milestone event for each shipment
     */
    @Query("SELECT st FROM ShipmentTracking st " +
           "WHERE st.isMilestone = true " +
           "AND st.deletedAt IS NULL " +
           "AND st.eventDate = (" +
           "    SELECT MAX(st2.eventDate) FROM ShipmentTracking st2 " +
           "    WHERE st2.shipment.id = st.shipment.id " +
           "    AND st2.isMilestone = true " +
           "    AND st2.deletedAt IS NULL" +
           ") " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findLatestMilestoneForEachShipment();

    /**
     * Check if a specific tracking event exists
     */
    @Query("SELECT COUNT(st) > 0 FROM ShipmentTracking st " +
           "WHERE st.shipment.id = :shipmentId " +
           "AND st.status = :status " +
           "AND st.eventDate = :eventDate " +
           "AND st.carrierEventCode = :carrierEventCode " +
           "AND st.deletedAt IS NULL")
    boolean existsByShipmentAndStatusAndEventDateAndCarrierEventCode(
            @Param("shipmentId") Long shipmentId,
            @Param("status") String status,
            @Param("eventDate") LocalDateTime eventDate,
            @Param("carrierEventCode") String carrierEventCode);

    /**
     * Find tracking events with raw data containing specific text
     */
    @Query("SELECT st FROM ShipmentTracking st " +
           "WHERE st.rawData IS NOT NULL " +
           "AND LOWER(st.rawData) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
           "AND st.deletedAt IS NULL " +
           "ORDER BY st.eventDate DESC")
    List<ShipmentTracking> findByRawDataContaining(@Param("searchText") String searchText);

    /**
     * Get average time between milestone events
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, st1.eventDate, st2.eventDate)) " +
           "FROM ShipmentTracking st1, ShipmentTracking st2 " +
           "WHERE st1.shipment.id = st2.shipment.id " +
           "AND st1.isMilestone = true " +
           "AND st2.isMilestone = true " +
           "AND st1.eventDate < st2.eventDate " +
           "AND st1.deletedAt IS NULL " +
           "AND st2.deletedAt IS NULL " +
           "AND st1.eventDate >= :since")
    Double getAverageTimeBetweenMilestones(@Param("since") LocalDateTime since);

    /**
     * Find shipments with no tracking events in specified time
     */
    @Query("SELECT DISTINCT s.id FROM Shipment s " +
           "WHERE s.id NOT IN (" +
           "    SELECT st.shipment.id FROM ShipmentTracking st " +
           "    WHERE st.eventDate >= :since " +
           "    AND st.deletedAt IS NULL" +
           ") " +
           "AND s.status IN ('SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY') " +
           "AND s.deletedAt IS NULL")
    List<Long> findShipmentsWithoutRecentTracking(@Param("since") LocalDateTime since);
}
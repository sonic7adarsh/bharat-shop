package com.bharatshop.notifications.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OutboxEvent entities.
 * Provides methods for outbox pattern implementation and event processing.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    
    /**
     * Find events ready for processing (pending or failed with retry time passed)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "(e.status = 'PENDING' OR " +
           "(e.status = 'FAILED' AND e.retryCount < e.maxRetries AND " +
           "(e.nextRetryAt IS NULL OR e.nextRetryAt <= :now))) " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findEventsReadyForProcessing(@Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Find events by status
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxEventStatus status);
    
    /**
     * Find events by tenant and status
     */
    List<OutboxEvent> findByTenantIdAndStatusOrderByCreatedAtAsc(String tenantId, OutboxEvent.OutboxEventStatus status);
    
    /**
     * Find events by event type and status
     */
    List<OutboxEvent> findByEventTypeAndStatusOrderByCreatedAtAsc(String eventType, OutboxEvent.OutboxEventStatus status);
    
    /**
     * Find events that are stuck in processing state (potential orphans)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "e.status = 'PROCESSING' AND " +
           "e.lastAttemptAt < :cutoffTime")
    List<OutboxEvent> findStuckProcessingEvents(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find events by processing instance ID
     */
    List<OutboxEvent> findByProcessingInstanceId(String instanceId);
    
    /**
     * Count events by status
     */
    long countByStatus(OutboxEvent.OutboxEventStatus status);
    
    /**
     * Count events by tenant and status
     */
    long countByTenantIdAndStatus(String tenantId, OutboxEvent.OutboxEventStatus status);
    
    /**
     * Find events created within date range
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "e.createdAt >= :startDate AND e.createdAt <= :endDate " +
           "ORDER BY e.createdAt DESC")
    Page<OutboxEvent> findEventsByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);
    
    /**
     * Find failed events that can be retried
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "e.status = 'FAILED' AND " +
           "e.retryCount < e.maxRetries AND " +
           "(e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findRetryableEvents(@Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Find dead letter events
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "e.status = 'DEAD_LETTER' OR " +
           "(e.status = 'FAILED' AND e.retryCount >= e.maxRetries) " +
           "ORDER BY e.createdAt DESC")
    Page<OutboxEvent> findDeadLetterEvents(Pageable pageable);
    
    /**
     * Find events by aggregate
     */
    List<OutboxEvent> findByAggregateIdAndAggregateTypeOrderByCreatedAtAsc(String aggregateId, String aggregateType);
    
    /**
     * Delete processed events older than specified date
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE " +
           "e.status = 'PROCESSED' AND " +
           "e.processedAt < :cutoffDate")
    int deleteProcessedEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Reset stuck processing events to pending
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET " +
           "e.status = 'PENDING', " +
           "e.processingInstanceId = null, " +
           "e.updatedAt = :now " +
           "WHERE e.status = 'PROCESSING' AND " +
           "e.lastAttemptAt < :cutoffTime")
    int resetStuckProcessingEvents(@Param("cutoffTime") LocalDateTime cutoffTime, 
                                  @Param("now") LocalDateTime now);
    
    /**
     * Update event status with optimistic locking
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET " +
           "e.status = :status, " +
           "e.updatedAt = :now " +
           "WHERE e.id = :id AND e.version = :version")
    int updateEventStatus(@Param("id") String id, 
                         @Param("status") OutboxEvent.OutboxEventStatus status,
                         @Param("version") Long version,
                         @Param("now") LocalDateTime now);
    
    /**
     * Find events with metadata key-value pair
     */
    @Query("SELECT e FROM OutboxEvent e JOIN e.metadata m WHERE " +
           "KEY(m) = :key AND VALUE(m) = :value")
    List<OutboxEvent> findByMetadata(@Param("key") String key, @Param("value") String value);
    
    /**
     * Get processing statistics
     */
    @Query("SELECT e.status, COUNT(e) FROM OutboxEvent e " +
           "WHERE e.tenantId = :tenantId " +
           "GROUP BY e.status")
    List<Object[]> getProcessingStatsByTenant(@Param("tenantId") String tenantId);
    
    /**
     * Find events that need cleanup (old processed events)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "e.status = 'PROCESSED' AND " +
           "e.processedAt < :cutoffDate " +
           "ORDER BY e.processedAt ASC")
    List<OutboxEvent> findEventsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    
    /**
     * Check if event exists for aggregate and event type
     */
    boolean existsByAggregateIdAndAggregateTypeAndEventType(String aggregateId, String aggregateType, String eventType);
    
    /**
     * Find latest event for aggregate
     */
    Optional<OutboxEvent> findFirstByAggregateIdAndAggregateTypeOrderByCreatedAtDesc(String aggregateId, String aggregateType);
}
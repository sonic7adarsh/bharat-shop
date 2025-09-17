package com.bharatshop.notifications.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing outbox events.
 * Handles event creation, processing, retry logic, and cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_CLEANUP_BATCH_SIZE = 1000;
    
    /**
     * Create and save an outbox event
     */
    @Transactional
    public OutboxEvent createEvent(String tenantId, String eventType, String aggregateId, 
                                  String aggregateType, Object eventData, Map<String, String> metadata) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            
            OutboxEvent event = OutboxEvent.builder()
                .tenantId(tenantId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventData(eventDataJson)
                .metadata(metadata)
                .status(OutboxEvent.OutboxEventStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .build();
            
            OutboxEvent savedEvent = outboxEventRepository.save(event);
            log.debug("Created outbox event: {} for tenant: {}, eventType: {}", 
                     savedEvent.getId(), tenantId, eventType);
            
            return savedEvent;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data for eventType: {}, aggregateId: {}", 
                     eventType, aggregateId, e);
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }
    
    /**
     * Get events ready for processing
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsReadyForProcessing(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return outboxEventRepository.findEventsReadyForProcessing(LocalDateTime.now(), pageable);
    }
    
    /**
     * Get events ready for processing with default batch size
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsReadyForProcessing() {
        return getEventsReadyForProcessing(DEFAULT_BATCH_SIZE);
    }
    
    /**
     * Mark event as processing
     */
    @Transactional
    public boolean markAsProcessing(String eventId, String instanceId) {
        Optional<OutboxEvent> eventOpt = outboxEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            log.warn("Event not found for processing: {}", eventId);
            return false;
        }
        
        OutboxEvent event = eventOpt.get();
        if (event.getStatus() != OutboxEvent.OutboxEventStatus.PENDING && 
            event.getStatus() != OutboxEvent.OutboxEventStatus.FAILED) {
            log.warn("Event {} is not in processable state: {}", eventId, event.getStatus());
            return false;
        }
        
        event.markAsProcessing(instanceId);
        outboxEventRepository.save(event);
        
        log.debug("Marked event {} as processing by instance: {}", eventId, instanceId);
        return true;
    }
    
    /**
     * Mark event as processed successfully
     */
    @Transactional
    public void markAsProcessed(String eventId) {
        Optional<OutboxEvent> eventOpt = outboxEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            log.warn("Event not found for completion: {}", eventId);
            return;
        }
        
        OutboxEvent event = eventOpt.get();
        event.markAsProcessed();
        outboxEventRepository.save(event);
        
        log.debug("Marked event {} as processed", eventId);
    }
    
    /**
     * Mark event as failed
     */
    @Transactional
    public void markAsFailed(String eventId, String errorMessage, String stackTrace) {
        Optional<OutboxEvent> eventOpt = outboxEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            log.warn("Event not found for failure marking: {}", eventId);
            return;
        }
        
        OutboxEvent event = eventOpt.get();
        event.markAsFailed(errorMessage, stackTrace);
        
        if (event.hasExceededMaxRetries()) {
            event.markAsDeadLetter();
            log.warn("Event {} moved to dead letter after {} retries", eventId, event.getRetryCount());
        } else {
            log.debug("Event {} marked as failed, retry {} scheduled for {}", 
                     eventId, event.getRetryCount(), event.getNextRetryAt());
        }
        
        outboxEventRepository.save(event);
    }
    
    /**
     * Get retryable events
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getRetryableEvents(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return outboxEventRepository.findRetryableEvents(LocalDateTime.now(), pageable);
    }
    
    /**
     * Reset stuck processing events
     */
    @Transactional
    public int resetStuckProcessingEvents(int timeoutMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int resetCount = outboxEventRepository.resetStuckProcessingEvents(cutoffTime, LocalDateTime.now());
        
        if (resetCount > 0) {
            log.warn("Reset {} stuck processing events older than {} minutes", resetCount, timeoutMinutes);
        }
        
        return resetCount;
    }
    
    /**
     * Get dead letter events
     */
    @Transactional(readOnly = true)
    public Page<OutboxEvent> getDeadLetterEvents(Pageable pageable) {
        return outboxEventRepository.findDeadLetterEvents(pageable);
    }
    
    /**
     * Retry dead letter event
     */
    @Transactional
    public boolean retryDeadLetterEvent(String eventId) {
        Optional<OutboxEvent> eventOpt = outboxEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            log.warn("Event not found for retry: {}", eventId);
            return false;
        }
        
        OutboxEvent event = eventOpt.get();
        if (event.getStatus() != OutboxEvent.OutboxEventStatus.DEAD_LETTER) {
            log.warn("Event {} is not in dead letter state: {}", eventId, event.getStatus());
            return false;
        }
        
        event.resetForRetry();
        outboxEventRepository.save(event);
        
        log.info("Reset dead letter event {} for retry", eventId);
        return true;
    }
    
    /**
     * Get events by aggregate
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsByAggregate(String aggregateId, String aggregateType) {
        return outboxEventRepository.findByAggregateIdAndAggregateTypeOrderByCreatedAtAsc(aggregateId, aggregateType);
    }
    
    /**
     * Get processing statistics by tenant
     */
    @Transactional(readOnly = true)
    public Map<OutboxEvent.OutboxEventStatus, Long> getProcessingStatsByTenant(String tenantId) {
        List<Object[]> results = outboxEventRepository.getProcessingStatsByTenant(tenantId);
        return results.stream()
            .collect(java.util.stream.Collectors.toMap(
                result -> (OutboxEvent.OutboxEventStatus) result[0],
                result -> (Long) result[1]
            ));
    }
    
    /**
     * Clean up old processed events
     */
    @Transactional
    public int cleanupProcessedEvents(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = outboxEventRepository.deleteProcessedEventsOlderThan(cutoffDate);
        
        if (deletedCount > 0) {
            log.info("Cleaned up {} processed events older than {} days", deletedCount, daysOld);
        }
        
        return deletedCount;
    }
    
    /**
     * Get events for cleanup in batches
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getEventsForCleanup(int daysOld, int batchSize) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        Pageable pageable = PageRequest.of(0, batchSize);
        return outboxEventRepository.findEventsForCleanup(cutoffDate, pageable);
    }
    
    /**
     * Delete events by IDs
     */
    @Transactional
    public void deleteEvents(List<String> eventIds) {
        outboxEventRepository.deleteAllById(eventIds);
        log.debug("Deleted {} events", eventIds.size());
    }
    
    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> getEventById(String eventId) {
        return outboxEventRepository.findById(eventId);
    }
    
    /**
     * Check if event exists for aggregate and event type
     */
    @Transactional(readOnly = true)
    public boolean eventExists(String aggregateId, String aggregateType, String eventType) {
        return outboxEventRepository.existsByAggregateIdAndAggregateTypeAndEventType(
            aggregateId, aggregateType, eventType);
    }
    
    /**
     * Get latest event for aggregate
     */
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> getLatestEventForAggregate(String aggregateId, String aggregateType) {
        return outboxEventRepository.findFirstByAggregateIdAndAggregateTypeOrderByCreatedAtDesc(
            aggregateId, aggregateType);
    }
    
    /**
     * Deserialize event data
     */
    public <T> T deserializeEventData(OutboxEvent event, Class<T> targetClass) {
        try {
            return objectMapper.readValue(event.getEventData(), targetClass);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event data for event: {}", event.getId(), e);
            throw new RuntimeException("Failed to deserialize event data", e);
        }
    }
    
    /**
     * Generate unique processing instance ID
     */
    public String generateInstanceId() {
        return UUID.randomUUID().toString();
    }
}
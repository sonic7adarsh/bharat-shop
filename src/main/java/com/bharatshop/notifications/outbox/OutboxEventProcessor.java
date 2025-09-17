package com.bharatshop.notifications.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes outbox events asynchronously with retry mechanism.
 * Handles event publishing, error recovery, and cleanup operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {
    
    private final OutboxEventService outboxEventService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${bharatshop.outbox.processor.batch-size:50}")
    private int batchSize;
    
    @Value("${bharatshop.outbox.processor.processing-timeout-minutes:30}")
    private int processingTimeoutMinutes;
    
    @Value("${bharatshop.outbox.processor.cleanup-days:7}")
    private int cleanupDays;
    
    @Value("${bharatshop.outbox.processor.enabled:true}")
    private boolean processorEnabled;
    
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private String instanceId;
    
    @PostConstruct
    public void init() {
        this.instanceId = outboxEventService.generateInstanceId();
        log.info("Outbox event processor initialized with instance ID: {}", instanceId);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down outbox event processor: {}", instanceId);
    }
    
    /**
     * Scheduled method to process pending outbox events
     */
    @Scheduled(fixedDelayString = "${bharatshop.outbox.processor.poll-interval:5000}")
    public void processPendingEvents() {
        if (!processorEnabled) {
            return;
        }
        
        if (!isProcessing.compareAndSet(false, true)) {
            log.debug("Outbox processor already running, skipping this cycle");
            return;
        }
        
        try {
            log.debug("Starting outbox event processing cycle");
            
            // Reset stuck processing events first
            resetStuckEvents();
            
            // Process pending events
            List<OutboxEvent> events = outboxEventService.getEventsReadyForProcessing(batchSize);
            
            if (!events.isEmpty()) {
                log.info("Processing {} outbox events", events.size());
                processEventsBatch(events);
            } else {
                log.debug("No events ready for processing");
            }
            
        } catch (Exception e) {
            log.error("Error during outbox event processing cycle", e);
        } finally {
            isProcessing.set(false);
        }
    }
    
    /**
     * Process a batch of events
     */
    private void processEventsBatch(List<OutboxEvent> events) {
        for (OutboxEvent event : events) {
            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process event: {}", event.getId(), e);
                handleEventProcessingError(event, e);
            }
        }
    }
    
    /**
     * Process a single event
     */
    @Transactional
    public void processEvent(OutboxEvent event) {
        log.debug("Processing outbox event: {} of type: {}", event.getId(), event.getEventType());
        
        // Mark as processing
        if (!outboxEventService.markAsProcessing(event.getId(), instanceId)) {
            log.warn("Failed to mark event {} as processing, skipping", event.getId());
            return;
        }
        
        try {
            // Create domain event based on event type
            Object domainEvent = createDomainEvent(event);
            
            // Publish the domain event
            eventPublisher.publishEvent(domainEvent);
            
            // Mark as processed
            outboxEventService.markAsProcessed(event.getId());
            
            log.debug("Successfully processed outbox event: {}", event.getId());
            
        } catch (Exception e) {
            log.error("Error processing outbox event: {}", event.getId(), e);
            handleEventProcessingError(event, e);
            throw e;
        }
    }
    
    /**
     * Create domain event from outbox event
     */
    private Object createDomainEvent(OutboxEvent event) {
        try {
            // This would typically use a factory or registry to create the appropriate domain event
            // For now, we'll create a generic wrapper
            return new ProcessedOutboxEvent(
                event.getId(),
                event.getTenantId(),
                event.getEventType(),
                event.getAggregateId(),
                event.getAggregateType(),
                event.getEventData(),
                event.getMetadata(),
                event.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to create domain event from outbox event: {}", event.getId(), e);
            throw new RuntimeException("Failed to create domain event", e);
        }
    }
    
    /**
     * Handle event processing error
     */
    private void handleEventProcessingError(OutboxEvent event, Exception e) {
        String errorMessage = e.getMessage();
        String stackTrace = getStackTrace(e);
        
        outboxEventService.markAsFailed(event.getId(), errorMessage, stackTrace);
    }
    
    /**
     * Reset stuck processing events
     */
    private void resetStuckEvents() {
        try {
            int resetCount = outboxEventService.resetStuckProcessingEvents(processingTimeoutMinutes);
            if (resetCount > 0) {
                log.warn("Reset {} stuck processing events", resetCount);
            }
        } catch (Exception e) {
            log.error("Error resetting stuck events", e);
        }
    }
    
    /**
     * Scheduled cleanup of old processed events
     */
    @Scheduled(cron = "${bharatshop.outbox.processor.cleanup-cron:0 0 2 * * ?}") // Daily at 2 AM
    public void cleanupProcessedEvents() {
        if (!processorEnabled) {
            return;
        }
        
        try {
            log.info("Starting cleanup of processed events older than {} days", cleanupDays);
            int deletedCount = outboxEventService.cleanupProcessedEvents(cleanupDays);
            log.info("Cleanup completed, deleted {} processed events", deletedCount);
        } catch (Exception e) {
            log.error("Error during cleanup of processed events", e);
        }
    }
    
    /**
     * Process events asynchronously
     */
    @Async("outboxTaskExecutor")
    public CompletableFuture<Void> processEventsAsync(List<OutboxEvent> events) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Processing {} events asynchronously", events.size());
            processEventsBatch(events);
        });
    }
    
    /**
     * Manually trigger event processing
     */
    public void triggerProcessing() {
        if (processorEnabled) {
            log.info("Manually triggering outbox event processing");
            processPendingEvents();
        } else {
            log.warn("Outbox processor is disabled, cannot trigger processing");
        }
    }
    
    /**
     * Get processor status
     */
    public ProcessorStatus getStatus() {
        return ProcessorStatus.builder()
            .instanceId(instanceId)
            .enabled(processorEnabled)
            .processing(isProcessing.get())
            .batchSize(batchSize)
            .processingTimeoutMinutes(processingTimeoutMinutes)
            .cleanupDays(cleanupDays)
            .build();
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Processor status information
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessorStatus {
        private String instanceId;
        private boolean enabled;
        private boolean processing;
        private int batchSize;
        private int processingTimeoutMinutes;
        private int cleanupDays;
    }
    
    /**
     * Wrapper for processed outbox events
     */
    @lombok.AllArgsConstructor
    @lombok.Data
    public static class ProcessedOutboxEvent {
        private String eventId;
        private String tenantId;
        private String eventType;
        private String aggregateId;
        private String aggregateType;
        private String eventData;
        private java.util.Map<String, String> metadata;
        private LocalDateTime createdAt;
    }
}
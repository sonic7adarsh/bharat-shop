package com.bharatshop.notifications.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Outbox event entity for implementing the outbox pattern.
 * Ensures reliable event processing and delivery with retry mechanism.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
    @Index(name = "idx_outbox_tenant_event", columnList = "tenantId, eventType"),
    @Index(name = "idx_outbox_next_retry", columnList = "nextRetryAt"),
    @Index(name = "idx_outbox_processed", columnList = "processedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String tenantId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String aggregateType;
    
    @Column(columnDefinition = "TEXT")
    private String eventData;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.PENDING;
    
    @Column
    private Integer retryCount = 0;
    
    @Column
    private Integer maxRetries = 3;
    
    @Column
    private LocalDateTime nextRetryAt;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(columnDefinition = "TEXT")
    private String errorStackTrace;
    
    @ElementCollection
    @CollectionTable(name = "outbox_event_metadata", 
                    joinColumns = @JoinColumn(name = "outbox_event_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column
    private LocalDateTime lastAttemptAt;
    
    @Column
    private String processingInstanceId;
    
    @Version
    private Long version;
    
    /**
     * Check if the event can be retried
     */
    public boolean canRetry() {
        return status == OutboxEventStatus.FAILED && 
               retryCount < maxRetries &&
               (nextRetryAt == null || nextRetryAt.isBefore(LocalDateTime.now()));
    }
    
    /**
     * Check if the event has exceeded max retries
     */
    public boolean hasExceededMaxRetries() {
        return retryCount >= maxRetries;
    }
    
    /**
     * Mark event as processing
     */
    public void markAsProcessing(String instanceId) {
        this.status = OutboxEventStatus.PROCESSING;
        this.processingInstanceId = instanceId;
        this.lastAttemptAt = LocalDateTime.now();
    }
    
    /**
     * Mark event as processed successfully
     */
    public void markAsProcessed() {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
        this.errorStackTrace = null;
    }
    
    /**
     * Mark event as failed and schedule retry
     */
    public void markAsFailed(String errorMessage, String stackTrace) {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount++;
        this.errorMessage = errorMessage;
        this.errorStackTrace = stackTrace;
        this.processingInstanceId = null;
        
        if (canRetry()) {
            // Exponential backoff: 1min, 5min, 15min
            long delayMinutes = (long) Math.pow(5, retryCount - 1);
            this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
        }
    }
    
    /**
     * Mark event as dead letter (permanently failed)
     */
    public void markAsDeadLetter() {
        this.status = OutboxEventStatus.DEAD_LETTER;
        this.processingInstanceId = null;
    }
    
    /**
     * Reset for manual retry
     */
    public void resetForRetry() {
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = null;
        this.errorMessage = null;
        this.errorStackTrace = null;
        this.processingInstanceId = null;
    }
    
    public enum OutboxEventStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED,
        DEAD_LETTER
    }
}
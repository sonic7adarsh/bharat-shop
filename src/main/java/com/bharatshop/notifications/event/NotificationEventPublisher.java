package com.bharatshop.notifications.event;

import com.bharatshop.notifications.outbox.OutboxEventService;
import com.bharatshop.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for publishing domain events to the outbox pattern.
 * Ensures reliable event processing and delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {
    
    private final OutboxEventService outboxEventService;
    
    /**
     * Publish order placed event
     */
    @Transactional
    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        log.debug("Publishing OrderPlacedEvent for order: {} in tenant: {}", 
                 event.getOrderId(), event.getTenantId());
        
        Map<String, String> metadata = createMetadata("ORDER_PLACED", event.getCustomerId());
        
        outboxEventService.createEvent(
            event.getTenantId(),
            "ORDER_PLACED",
            event.getOrderId(),
            "Order",
            event,
            metadata
        );
    }
    
    /**
     * Publish payment captured event
     */
    @Transactional
    public void publishPaymentCapturedEvent(PaymentCapturedEvent event) {
        log.debug("Publishing PaymentCapturedEvent for order: {} in tenant: {}", 
                 event.getOrderId(), event.getTenantId());
        
        Map<String, String> metadata = createMetadata("PAYMENT_CAPTURED", event.getCustomerId());
        
        outboxEventService.createEvent(
            event.getTenantId(),
            "PAYMENT_CAPTURED",
            event.getOrderId(),
            "Order",
            event,
            metadata
        );
    }
    
    /**
     * Publish order shipped event
     */
    @Transactional
    public void publishOrderShippedEvent(OrderShippedEvent event) {
        log.debug("Publishing OrderShippedEvent for order: {} in tenant: {}", 
                 event.getOrderId(), event.getTenantId());
        
        Map<String, String> metadata = createMetadata("ORDER_SHIPPED", event.getCustomerId());
        
        outboxEventService.createEvent(
            event.getTenantId(),
            "ORDER_SHIPPED",
            event.getOrderId(),
            "Order",
            event,
            metadata
        );
    }
    
    /**
     * Publish order delivered event
     */
    @Transactional
    public void publishOrderDeliveredEvent(OrderDeliveredEvent event) {
        log.debug("Publishing OrderDeliveredEvent for order: {} in tenant: {}", 
                 event.getOrderId(), event.getTenantId());
        
        Map<String, String> metadata = createMetadata("ORDER_DELIVERED", event.getCustomerId());
        
        outboxEventService.createEvent(
            event.getTenantId(),
            "ORDER_DELIVERED",
            event.getOrderId(),
            "Order",
            event,
            metadata
        );
    }
    
    /**
     * Publish refund processed event
     */
    @Transactional
    public void publishRefundProcessedEvent(RefundProcessedEvent event) {
        log.debug("Publishing RefundProcessedEvent for order: {} in tenant: {}", 
                 event.getOrderId(), event.getTenantId());
        
        Map<String, String> metadata = createMetadata("REFUND_PROCESSED", event.getCustomerId());
        
        outboxEventService.createEvent(
            event.getTenantId(),
            "REFUND_PROCESSED",
            event.getOrderId(),
            "Order",
            event,
            metadata
        );
    }
    
    /**
     * Publish abandoned cart event
     */
    @Transactional
    public void publishAbandonedCartEvent(AbandonedCartEvent event) {
        log.debug("Publishing AbandonedCartEvent for cart: {} in tenant: {}", 
                 event.getCartId(), event.getTenantId());
        
        Map<String, String> metadata = createMetadata("ABANDONED_CART", event.getCustomerId());
        
        outboxEventService.createEvent(
            event.getTenantId(),
            "ABANDONED_CART",
            event.getCartId(),
            "Cart",
            event,
            metadata
        );
    }
    
    /**
     * Publish generic notification event
     */
    @Transactional
    public void publishNotificationEvent(String tenantId, String eventType, String aggregateId, 
                                        String aggregateType, Object eventData, String customerId) {
        log.debug("Publishing generic notification event: {} for aggregate: {} in tenant: {}", 
                 eventType, aggregateId, tenantId);
        
        Map<String, String> metadata = createMetadata(eventType, customerId);
        
        outboxEventService.createEvent(
            tenantId,
            eventType,
            aggregateId,
            aggregateType,
            eventData,
            metadata
        );
    }
    
    /**
     * Publish custom event with metadata
     */
    @Transactional
    public void publishCustomEvent(String tenantId, String eventType, String aggregateId, 
                                  String aggregateType, Object eventData, Map<String, String> customMetadata) {
        log.debug("Publishing custom event: {} for aggregate: {} in tenant: {}", 
                 eventType, aggregateId, tenantId);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventId", UUID.randomUUID().toString());
        metadata.put("source", "notification-service");
        metadata.put("version", "1.0");
        
        if (customMetadata != null) {
            metadata.putAll(customMetadata);
        }
        
        outboxEventService.createEvent(
            tenantId,
            eventType,
            aggregateId,
            aggregateType,
            eventData,
            metadata
        );
    }
    
    /**
     * Check if event already exists to prevent duplicates
     */
    public boolean eventExists(String aggregateId, String aggregateType, String eventType) {
        return outboxEventService.eventExists(aggregateId, aggregateType, eventType);
    }
    
    /**
     * Publish event only if it doesn't already exist
     */
    @Transactional
    public boolean publishEventIfNotExists(String tenantId, String eventType, String aggregateId, 
                                          String aggregateType, Object eventData, String customerId) {
        if (eventExists(aggregateId, aggregateType, eventType)) {
            log.debug("Event already exists: {} for aggregate: {}, skipping", eventType, aggregateId);
            return false;
        }
        
        publishNotificationEvent(tenantId, eventType, aggregateId, aggregateType, eventData, customerId);
        return true;
    }
    
    /**
     * Create standard metadata for events
     */
    private Map<String, String> createMetadata(String eventType, String customerId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventId", UUID.randomUUID().toString());
        metadata.put("eventType", eventType);
        metadata.put("customerId", customerId);
        metadata.put("source", "notification-service");
        metadata.put("version", "1.0");
        metadata.put("publishedAt", java.time.LocalDateTime.now().toString());
        return metadata;
    }
    
    /**
     * Get event publishing statistics
     */
    public EventPublishingStatistics getStatistics(String tenantId) {
        Map<com.bharatshop.notifications.outbox.OutboxEvent.OutboxEventStatus, Long> stats = 
            outboxEventService.getProcessingStatsByTenant(tenantId);
        
        return EventPublishingStatistics.builder()
            .tenantId(tenantId)
            .pendingEvents(stats.getOrDefault(
                com.bharatshop.notifications.outbox.OutboxEvent.OutboxEventStatus.PENDING, 0L))
            .processingEvents(stats.getOrDefault(
                com.bharatshop.notifications.outbox.OutboxEvent.OutboxEventStatus.PROCESSING, 0L))
            .processedEvents(stats.getOrDefault(
                com.bharatshop.notifications.outbox.OutboxEvent.OutboxEventStatus.PROCESSED, 0L))
            .failedEvents(stats.getOrDefault(
                com.bharatshop.notifications.outbox.OutboxEvent.OutboxEventStatus.FAILED, 0L))
            .deadLetterEvents(stats.getOrDefault(
                com.bharatshop.notifications.outbox.OutboxEvent.OutboxEventStatus.DEAD_LETTER, 0L))
            .build();
    }
    
    /**
     * Event publishing statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class EventPublishingStatistics {
        private String tenantId;
        private Long pendingEvents;
        private Long processingEvents;
        private Long processedEvents;
        private Long failedEvents;
        private Long deadLetterEvents;
        
        public Long getTotalEvents() {
            return pendingEvents + processingEvents + processedEvents + failedEvents + deadLetterEvents;
        }
        
        public double getSuccessRate() {
            Long total = getTotalEvents();
            return total > 0 ? (processedEvents.doubleValue() / total.doubleValue()) * 100 : 0.0;
        }
        
        public double getFailureRate() {
            Long total = getTotalEvents();
            return total > 0 ? ((failedEvents + deadLetterEvents).doubleValue() / total.doubleValue()) * 100 : 0.0;
        }
    }
}
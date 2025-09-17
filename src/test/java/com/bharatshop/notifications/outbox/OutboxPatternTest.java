package com.bharatshop.notifications.outbox;

import com.bharatshop.notifications.enums.OutboxEventStatus;
import com.bharatshop.shared.events.OrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the outbox pattern implementation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxPatternTest {
    
    @Autowired
    private OutboxEventService outboxEventService;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private OutboxEventProcessor outboxEventProcessor;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String TEST_AGGREGATE_ID = "test-aggregate-123";
    
    @BeforeEach
    void setUp() {
        // Clean up any existing events
        outboxEventRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should create outbox event successfully")
    void testCreateOutboxEvent() throws Exception {
        // Given
        OrderPlacedEvent event = createTestOrderPlacedEvent();
        String eventData = objectMapper.writeValueAsString(event);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "test");
        metadata.put("version", "1.0");
        
        // When
        OutboxEvent outboxEvent = outboxEventService.createEvent(
            TEST_TENANT_ID,
            "ORDER_PLACED",
            TEST_AGGREGATE_ID,
            "Order",
            eventData,
            metadata
        );
        
        // Then
        assertThat(outboxEvent).isNotNull();
        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(outboxEvent.getEventType()).isEqualTo("ORDER_PLACED");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(TEST_AGGREGATE_ID);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("Order");
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(outboxEvent.getRetryCount()).isEqualTo(0);
        assertThat(outboxEvent.getCreatedAt()).isNotNull();
        
        // Verify it was saved to database
        OutboxEvent savedEvent = outboxEventRepository.findById(outboxEvent.getId()).orElse(null);
        assertThat(savedEvent).isNotNull();
        assertThat(savedEvent.getEventData()).isEqualTo(eventData);
    }
    
    @Test
    @DisplayName("Should find events ready for processing")
    void testFindReadyForProcessing() throws Exception {
        // Given: Create multiple events with different statuses
        createTestEvent("EVENT_1", OutboxEventStatus.PENDING);
        createTestEvent("EVENT_2", OutboxEventStatus.PENDING);
        createTestEvent("EVENT_3", OutboxEventStatus.PROCESSING);
        createTestEvent("EVENT_4", OutboxEventStatus.COMPLETED);
        
        // When
        List<OutboxEvent> readyEvents = outboxEventRepository.findReadyForProcessing(10);
        
        // Then
        assertThat(readyEvents).hasSize(2);
        assertThat(readyEvents).allMatch(event -> event.getStatus() == OutboxEventStatus.PENDING);
    }
    
    @Test
    @DisplayName("Should mark event as processing")
    void testMarkAsProcessing() throws Exception {
        // Given
        OutboxEvent event = createTestEvent("TEST_EVENT", OutboxEventStatus.PENDING);
        
        // When
        outboxEventService.markAsProcessing(event.getId());
        
        // Then
        OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElse(null);
        assertThat(updatedEvent).isNotNull();
        assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(updatedEvent.getProcessingStartedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should mark event as completed")
    void testMarkAsCompleted() throws Exception {
        // Given
        OutboxEvent event = createTestEvent("TEST_EVENT", OutboxEventStatus.PROCESSING);
        
        // When
        outboxEventService.markAsCompleted(event.getId());
        
        // Then
        OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElse(null);
        assertThat(updatedEvent).isNotNull();
        assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEventStatus.COMPLETED);
        assertThat(updatedEvent.getCompletedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle event failure with retry")
    void testEventFailureWithRetry() throws Exception {
        // Given
        OutboxEvent event = createTestEvent("TEST_EVENT", OutboxEventStatus.PROCESSING);
        String errorMessage = "Test error message";
        
        // When
        outboxEventService.markAsFailed(event.getId(), errorMessage);
        
        // Then
        OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElse(null);
        assertThat(updatedEvent).isNotNull();
        assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(updatedEvent.getRetryCount()).isEqualTo(1);
        assertThat(updatedEvent.getLastError()).isEqualTo(errorMessage);
        assertThat(updatedEvent.getNextRetryAt()).isNotNull();
        assertThat(updatedEvent.getNextRetryAt()).isAfter(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("Should move to dead letter after max retries")
    void testDeadLetterAfterMaxRetries() throws Exception {
        // Given
        OutboxEvent event = createTestEvent("TEST_EVENT", OutboxEventStatus.PROCESSING);
        
        // Simulate multiple failures
        for (int i = 0; i < 5; i++) {
            outboxEventService.markAsFailed(event.getId(), "Retry attempt " + (i + 1));
            event = outboxEventRepository.findById(event.getId()).orElse(null);
        }
        
        // When: One more failure should move to dead letter
        outboxEventService.markAsFailed(event.getId(), "Final failure");
        
        // Then
        OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElse(null);
        assertThat(updatedEvent).isNotNull();
        assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
        assertThat(updatedEvent.getRetryCount()).isEqualTo(6);
    }
    
    @Test
    @DisplayName("Should find retryable events")
    void testFindRetryableEvents() throws Exception {
        // Given: Create events with different retry times
        OutboxEvent event1 = createTestEvent("EVENT_1", OutboxEventStatus.FAILED);
        OutboxEvent event2 = createTestEvent("EVENT_2", OutboxEventStatus.FAILED);
        OutboxEvent event3 = createTestEvent("EVENT_3", OutboxEventStatus.FAILED);
        
        // Set retry times
        event1.setNextRetryAt(LocalDateTime.now().minusMinutes(5)); // Ready for retry
        event2.setNextRetryAt(LocalDateTime.now().plusMinutes(5));  // Not ready yet
        event3.setNextRetryAt(LocalDateTime.now().minusMinutes(1)); // Ready for retry
        
        outboxEventRepository.saveAll(List.of(event1, event2, event3));
        
        // When
        List<OutboxEvent> retryableEvents = outboxEventRepository.findRetryableEvents(10);
        
        // Then
        assertThat(retryableEvents).hasSize(2);
        assertThat(retryableEvents).extracting(OutboxEvent::getId)
            .containsExactlyInAnyOrder(event1.getId(), event3.getId());
    }
    
    @Test
    @DisplayName("Should find stuck processing events")
    void testFindStuckProcessingEvents() throws Exception {
        // Given: Create events with different processing start times
        OutboxEvent event1 = createTestEvent("EVENT_1", OutboxEventStatus.PROCESSING);
        OutboxEvent event2 = createTestEvent("EVENT_2", OutboxEventStatus.PROCESSING);
        
        // Set processing start times
        event1.setProcessingStartedAt(LocalDateTime.now().minusHours(2)); // Stuck
        event2.setProcessingStartedAt(LocalDateTime.now().minusMinutes(5)); // Not stuck
        
        outboxEventRepository.saveAll(List.of(event1, event2));
        
        // When
        List<OutboxEvent> stuckEvents = outboxEventRepository.findStuckProcessingEvents(
            LocalDateTime.now().minusHours(1), 10);
        
        // Then
        assertThat(stuckEvents).hasSize(1);
        assertThat(stuckEvents.get(0).getId()).isEqualTo(event1.getId());
    }
    
    @Test
    @DisplayName("Should calculate exponential backoff correctly")
    void testExponentialBackoff() {
        // Given
        OutboxEvent event = new OutboxEvent();
        
        // Test different retry counts
        LocalDateTime baseTime = LocalDateTime.now();
        
        // First retry: 1 minute
        event.setRetryCount(1);
        LocalDateTime retry1 = event.calculateNextRetryTime();
        assertThat(retry1).isAfter(baseTime.plusSeconds(50)); // Allow some variance
        assertThat(retry1).isBefore(baseTime.plusMinutes(2));
        
        // Second retry: 2 minutes
        event.setRetryCount(2);
        LocalDateTime retry2 = event.calculateNextRetryTime();
        assertThat(retry2).isAfter(baseTime.plusMinutes(1));
        assertThat(retry2).isBefore(baseTime.plusMinutes(3));
        
        // Third retry: 4 minutes
        event.setRetryCount(3);
        LocalDateTime retry3 = event.calculateNextRetryTime();
        assertThat(retry3).isAfter(baseTime.plusMinutes(3));
        assertThat(retry3).isBefore(baseTime.plusMinutes(5));
    }
    
    @Test
    @DisplayName("Should cleanup old completed events")
    void testCleanupOldEvents() throws Exception {
        // Given: Create events with different completion times
        OutboxEvent oldCompleted = createTestEvent("OLD_COMPLETED", OutboxEventStatus.COMPLETED);
        OutboxEvent recentCompleted = createTestEvent("RECENT_COMPLETED", OutboxEventStatus.COMPLETED);
        OutboxEvent oldDeadLetter = createTestEvent("OLD_DEAD_LETTER", OutboxEventStatus.DEAD_LETTER);
        OutboxEvent pendingEvent = createTestEvent("PENDING", OutboxEventStatus.PENDING);
        
        // Set completion times
        oldCompleted.setCompletedAt(LocalDateTime.now().minusDays(8));
        recentCompleted.setCompletedAt(LocalDateTime.now().minusDays(2));
        oldDeadLetter.setCompletedAt(LocalDateTime.now().minusDays(8));
        
        outboxEventRepository.saveAll(List.of(oldCompleted, recentCompleted, oldDeadLetter, pendingEvent));
        
        // When
        int deletedCount = outboxEventService.cleanupOldEvents(7); // 7 days retention
        
        // Then
        assertThat(deletedCount).isEqualTo(2); // oldCompleted and oldDeadLetter
        
        List<OutboxEvent> remainingEvents = outboxEventRepository.findAll();
        assertThat(remainingEvents).hasSize(2);
        assertThat(remainingEvents).extracting(OutboxEvent::getEventType)
            .containsExactlyInAnyOrder("RECENT_COMPLETED", "PENDING");
    }
    
    @Test
    @DisplayName("Should get statistics correctly")
    void testGetStatistics() throws Exception {
        // Given: Create events with different statuses
        createTestEvent("PENDING_1", OutboxEventStatus.PENDING);
        createTestEvent("PENDING_2", OutboxEventStatus.PENDING);
        createTestEvent("PROCESSING_1", OutboxEventStatus.PROCESSING);
        createTestEvent("COMPLETED_1", OutboxEventStatus.COMPLETED);
        createTestEvent("COMPLETED_2", OutboxEventStatus.COMPLETED);
        createTestEvent("COMPLETED_3", OutboxEventStatus.COMPLETED);
        createTestEvent("FAILED_1", OutboxEventStatus.FAILED);
        createTestEvent("DEAD_LETTER_1", OutboxEventStatus.DEAD_LETTER);
        
        // When
        Map<String, Long> stats = outboxEventService.getStatistics();
        
        // Then
        assertThat(stats.get("pending")).isEqualTo(2L);
        assertThat(stats.get("processing")).isEqualTo(1L);
        assertThat(stats.get("completed")).isEqualTo(3L);
        assertThat(stats.get("failed")).isEqualTo(1L);
        assertThat(stats.get("deadLetter")).isEqualTo(1L);
        assertThat(stats.get("total")).isEqualTo(8L);
    }
    
    @Test
    @DisplayName("Should process events asynchronously")
    void testAsyncEventProcessing() throws Exception {
        // Given
        OutboxEvent event = createTestEvent("ASYNC_TEST", OutboxEventStatus.PENDING);
        
        // When
        outboxEventProcessor.processReadyEvents();
        
        // Then: Event should be marked as processing or completed
        // (depending on the actual processing logic)
        OutboxEvent processedEvent = outboxEventRepository.findById(event.getId()).orElse(null);
        assertThat(processedEvent).isNotNull();
        assertThat(processedEvent.getStatus()).isIn(
            OutboxEventStatus.PROCESSING, 
            OutboxEventStatus.COMPLETED
        );
    }
    
    private OutboxEvent createTestEvent(String eventType, OutboxEventStatus status) throws Exception {
        OrderPlacedEvent domainEvent = createTestOrderPlacedEvent();
        String eventData = objectMapper.writeValueAsString(domainEvent);
        
        OutboxEvent event = OutboxEvent.builder()
            .tenantId(TEST_TENANT_ID)
            .eventType(eventType)
            .aggregateId(TEST_AGGREGATE_ID + "-" + eventType)
            .aggregateType("Order")
            .eventData(eventData)
            .status(status)
            .retryCount(0)
            .createdAt(LocalDateTime.now())
            .build();
        
        return outboxEventRepository.save(event);
    }
    
    private OrderPlacedEvent createTestOrderPlacedEvent() {
        return OrderPlacedEvent.builder()
            .tenantId(TEST_TENANT_ID)
            .customerId("customer-123")
            .orderId("order-" + UUID.randomUUID().toString().substring(0, 8))
            .customerName("Test Customer")
            .orderTotal(new BigDecimal("99.99"))
            .currency("USD")
            .orderDate(LocalDateTime.now())
            .estimatedDeliveryDate(LocalDateTime.now().plusDays(3))
            .items(List.of("Product A", "Product B"))
            .shippingAddress("123 Test St, Test City, TS 12345")
            .build();
    }
}
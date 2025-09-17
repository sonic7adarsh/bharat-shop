package com.bharatshop.notifications.event;

import com.bharatshop.notifications.dto.NotificationRequest;
import com.bharatshop.notifications.dto.NotificationResponse;
import com.bharatshop.notifications.enums.NotificationChannel;
import com.bharatshop.notifications.enums.NotificationPriority;
import com.bharatshop.notifications.outbox.OutboxEventProcessor;
import com.bharatshop.notifications.service.NotificationOrchestrator;
import com.bharatshop.shared.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for processing domain events and triggering notifications.
 * Handles conversion from domain events to notification requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final NotificationOrchestrator notificationOrchestrator;
    private final ObjectMapper objectMapper;
    
    /**
     * Handle processed outbox events
     */
    @EventListener
    @Async
    @Transactional
    public void handleProcessedOutboxEvent(OutboxEventProcessor.ProcessedOutboxEvent event) {
        log.debug("Processing outbox event: {} of type: {}", event.getEventId(), event.getEventType());
        
        try {
            switch (event.getEventType()) {
                case "ORDER_PLACED" -> handleOrderPlacedEvent(event);
                case "PAYMENT_CAPTURED" -> handlePaymentCapturedEvent(event);
                case "ORDER_SHIPPED" -> handleOrderShippedEvent(event);
                case "ORDER_DELIVERED" -> handleOrderDeliveredEvent(event);
                case "REFUND_PROCESSED" -> handleRefundProcessedEvent(event);
                case "ABANDONED_CART" -> handleAbandonedCartEvent(event);
                default -> {
                    log.warn("Unknown event type: {}, skipping notification processing", event.getEventType());
                }
            }
        } catch (Exception e) {
            log.error("Error processing outbox event: {} of type: {}", 
                     event.getEventId(), event.getEventType(), e);
            throw e;
        }
    }
    
    /**
     * Handle order placed event
     */
    private void handleOrderPlacedEvent(OutboxEventProcessor.ProcessedOutboxEvent outboxEvent) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(outboxEvent.getEventData(), OrderPlacedEvent.class);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", event.getCustomerName());
            variables.put("orderId", event.getOrderId());
            variables.put("orderTotal", event.getOrderTotal());
            variables.put("currency", event.getCurrency());
            variables.put("orderDate", event.getOrderDate());
            variables.put("estimatedDelivery", event.getEstimatedDeliveryDate());
            variables.put("items", event.getItems());
            variables.put("shippingAddress", event.getShippingAddress());
            
            NotificationRequest request = createNotificationRequest(
                event.getTenantId(),
                event.getCustomerId(),
                "ORDER_PLACED",
                variables,
                NotificationPriority.HIGH
            );
            
            List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
            logNotificationResults("ORDER_PLACED", event.getOrderId(), responses);
            
        } catch (Exception e) {
            log.error("Error handling OrderPlacedEvent from outbox: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Failed to process OrderPlacedEvent", e);
        }
    }
    
    /**
     * Handle payment captured event
     */
    private void handlePaymentCapturedEvent(OutboxEventProcessor.ProcessedOutboxEvent outboxEvent) {
        try {
            PaymentCapturedEvent event = objectMapper.readValue(outboxEvent.getEventData(), PaymentCapturedEvent.class);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", event.getCustomerName());
            variables.put("orderId", event.getOrderId());
            variables.put("paymentAmount", event.getPaymentAmount());
            variables.put("currency", event.getCurrency());
            variables.put("paymentMethod", event.getPaymentMethod());
            variables.put("transactionId", event.getTransactionId());
            variables.put("paymentDate", event.getPaymentDate());
            
            NotificationRequest request = createNotificationRequest(
                event.getTenantId(),
                event.getCustomerId(),
                "PAYMENT_CAPTURED",
                variables,
                NotificationPriority.HIGH
            );
            
            List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
            logNotificationResults("PAYMENT_CAPTURED", event.getOrderId(), responses);
            
        } catch (Exception e) {
            log.error("Error handling PaymentCapturedEvent from outbox: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Failed to process PaymentCapturedEvent", e);
        }
    }
    
    /**
     * Handle order shipped event
     */
    private void handleOrderShippedEvent(OutboxEventProcessor.ProcessedOutboxEvent outboxEvent) {
        try {
            OrderShippedEvent event = objectMapper.readValue(outboxEvent.getEventData(), OrderShippedEvent.class);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", event.getCustomerName());
            variables.put("orderId", event.getOrderId());
            variables.put("trackingNumber", event.getTrackingNumber());
            variables.put("carrier", event.getCarrier());
            variables.put("shippedDate", event.getShippedDate());
            variables.put("estimatedDelivery", event.getEstimatedDeliveryDate());
            variables.put("shippingAddress", event.getShippingAddress());
            variables.put("trackingUrl", generateTrackingUrl(event.getCarrier(), event.getTrackingNumber()));
            
            NotificationRequest request = createNotificationRequest(
                event.getTenantId(),
                event.getCustomerId(),
                "ORDER_SHIPPED",
                variables,
                NotificationPriority.MEDIUM
            );
            
            List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
            logNotificationResults("ORDER_SHIPPED", event.getOrderId(), responses);
            
        } catch (Exception e) {
            log.error("Error handling OrderShippedEvent from outbox: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Failed to process OrderShippedEvent", e);
        }
    }
    
    /**
     * Handle order delivered event
     */
    private void handleOrderDeliveredEvent(OutboxEventProcessor.ProcessedOutboxEvent outboxEvent) {
        try {
            OrderDeliveredEvent event = objectMapper.readValue(outboxEvent.getEventData(), OrderDeliveredEvent.class);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", event.getCustomerName());
            variables.put("orderId", event.getOrderId());
            variables.put("deliveredDate", event.getDeliveredDate());
            variables.put("deliveredTo", event.getDeliveredTo());
            variables.put("signature", event.getSignature());
            variables.put("feedbackUrl", generateFeedbackUrl(event.getTenantId(), event.getOrderId()));
            
            NotificationRequest request = createNotificationRequest(
                event.getTenantId(),
                event.getCustomerId(),
                "ORDER_DELIVERED",
                variables,
                NotificationPriority.MEDIUM
            );
            
            List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
            logNotificationResults("ORDER_DELIVERED", event.getOrderId(), responses);
            
        } catch (Exception e) {
            log.error("Error handling OrderDeliveredEvent from outbox: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Failed to process OrderDeliveredEvent", e);
        }
    }
    
    /**
     * Handle refund processed event
     */
    private void handleRefundProcessedEvent(OutboxEventProcessor.ProcessedOutboxEvent outboxEvent) {
        try {
            RefundProcessedEvent event = objectMapper.readValue(outboxEvent.getEventData(), RefundProcessedEvent.class);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", event.getCustomerName());
            variables.put("orderId", event.getOrderId());
            variables.put("refundAmount", event.getRefundAmount());
            variables.put("currency", event.getCurrency());
            variables.put("refundReason", event.getRefundReason());
            variables.put("refundDate", event.getRefundDate());
            variables.put("refundMethod", event.getRefundMethod());
            variables.put("processingTime", event.getProcessingTime());
            
            NotificationRequest request = createNotificationRequest(
                event.getTenantId(),
                event.getCustomerId(),
                "REFUND_PROCESSED",
                variables,
                NotificationPriority.HIGH
            );
            
            List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
            logNotificationResults("REFUND_PROCESSED", event.getOrderId(), responses);
            
        } catch (Exception e) {
            log.error("Error handling RefundProcessedEvent from outbox: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Failed to process RefundProcessedEvent", e);
        }
    }
    
    /**
     * Handle abandoned cart event
     */
    private void handleAbandonedCartEvent(OutboxEventProcessor.ProcessedOutboxEvent outboxEvent) {
        try {
            AbandonedCartEvent event = objectMapper.readValue(outboxEvent.getEventData(), AbandonedCartEvent.class);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", event.getCustomerName());
            variables.put("cartId", event.getCartId());
            variables.put("cartTotal", event.getCartTotal());
            variables.put("currency", event.getCurrency());
            variables.put("abandonedDate", event.getAbandonedDate());
            variables.put("items", event.getItems());
            variables.put("cartUrl", generateCartUrl(event.getTenantId(), event.getCartId()));
            variables.put("discountCode", generateDiscountCode(event.getCustomerId()));
            
            NotificationRequest request = createNotificationRequest(
                event.getTenantId(),
                event.getCustomerId(),
                "ABANDONED_CART",
                variables,
                NotificationPriority.LOW
            );
            
            // Schedule abandoned cart notification for later (e.g., 1 hour delay)
            request.setScheduledAt(LocalDateTime.now().plusHours(1));
            
            List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
            logNotificationResults("ABANDONED_CART", event.getCartId(), responses);
            
        } catch (Exception e) {
            log.error("Error handling AbandonedCartEvent from outbox: {}", outboxEvent.getEventId(), e);
            throw new RuntimeException("Failed to process AbandonedCartEvent", e);
        }
    }
    
    /**
     * Create notification request
     */
    private NotificationRequest createNotificationRequest(String tenantId, String customerId, 
                                                         String eventType, Map<String, Object> variables,
                                                         NotificationPriority priority) {
        return NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .customerId(customerId)
            .eventType(eventType)
            .variables(variables)
            .priority(priority)
            .metadata(createMetadata(eventType))
            .build();
    }
    
    /**
     * Create metadata for notification request
     */
    private Map<String, String> createMetadata(String eventType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "event-listener");
        metadata.put("eventType", eventType);
        metadata.put("processedAt", LocalDateTime.now().toString());
        return metadata;
    }
    
    /**
     * Generate tracking URL
     */
    private String generateTrackingUrl(String carrier, String trackingNumber) {
        return switch (carrier.toLowerCase()) {
            case "fedex" -> "https://www.fedex.com/fedextrack/?trknbr=" + trackingNumber;
            case "ups" -> "https://www.ups.com/track?tracknum=" + trackingNumber;
            case "dhl" -> "https://www.dhl.com/track?trackingNumber=" + trackingNumber;
            default -> "https://track.example.com/" + trackingNumber;
        };
    }
    
    /**
     * Generate feedback URL
     */
    private String generateFeedbackUrl(String tenantId, String orderId) {
        return String.format("https://%s.bharatshop.com/feedback/%s", tenantId, orderId);
    }
    
    /**
     * Generate cart URL
     */
    private String generateCartUrl(String tenantId, String cartId) {
        return String.format("https://%s.bharatshop.com/cart/%s", tenantId, cartId);
    }
    
    /**
     * Generate discount code for abandoned cart
     */
    private String generateDiscountCode(String customerId) {
        return "COMEBACK10-" + customerId.substring(0, Math.min(8, customerId.length())).toUpperCase();
    }
    
    /**
     * Log notification results
     */
    private void logNotificationResults(String eventType, String entityId, List<NotificationResponse> responses) {
        if (responses.isEmpty()) {
            log.info("No notifications sent for {} event: {}", eventType, entityId);
            return;
        }
        
        long successCount = responses.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = responses.size() - successCount;
        
        log.info("Processed {} notifications for {} event: {} - Success: {}, Failed: {}", 
                responses.size(), eventType, entityId, successCount, failureCount);
        
        // Log failures
        responses.stream()
            .filter(r -> !r.isSuccess())
            .forEach(r -> log.warn("Failed notification: {} - {}", r.getNotificationId(), r.getErrorMessage()));
    }
}
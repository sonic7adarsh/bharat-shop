package com.bharatshop.shared.service;

import com.bharatshop.shared.event.OrderStatusChangeEvent;
import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.entity.ReturnRequest;
import com.bharatshop.shared.exception.BusinessException;
import com.bharatshop.shared.service.ReturnRequestEvents.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling webhooks and external notifications for order events.
 * Supports multiple webhook endpoints with retry logic and signature verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebhookService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${bharatshop.webhooks.enabled:true}")
    private boolean webhooksEnabled;
    
    @Value("${bharatshop.webhooks.secret}")
    private String webhookSecret;
    
    @Value("${bharatshop.webhooks.timeout:5000}")
    private int webhookTimeout;
    
    @Value("${bharatshop.webhooks.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${bharatshop.webhooks.endpoints}")
    private List<String> webhookEndpoints;

    /**
     * Listen for order status change events and send webhooks
     */
    @EventListener
    @Async
    public void handleOrderStatusChangeEvent(OrderStatusChangeEvent event) {
        if (!webhooksEnabled || webhookEndpoints.isEmpty()) {
            log.debug("Webhooks disabled or no endpoints configured");
            return;
        }

        WebhookPayload payload = createOrderStatusWebhookPayload(event);
        sendWebhookToAllEndpoints(payload, "order.status.changed");
    }

    /**
     * Listen for return request events and send webhooks
     */
    @EventListener
    @Async
    public void handleReturnRequestCreatedEvent(ReturnRequestCreatedEvent event) {
        if (!webhooksEnabled) return;
        
        WebhookPayload payload = createReturnRequestWebhookPayload(event.returnRequest(), "return.created");
        sendWebhookToAllEndpoints(payload, "return.created");
    }

    @EventListener
    @Async
    public void handleReturnRequestApprovedEvent(ReturnRequestApprovedEvent event) {
        if (!webhooksEnabled) return;
        
        WebhookPayload payload = createReturnRequestWebhookPayload(event.returnRequest(), "return.approved");
        sendWebhookToAllEndpoints(payload, "return.approved");
    }

    @EventListener
    @Async
    public void handleReturnRequestRejectedEvent(ReturnRequestRejectedEvent event) {
        if (!webhooksEnabled) return;
        
        WebhookPayload payload = createReturnRequestWebhookPayload(event.returnRequest(), "return.rejected");
        sendWebhookToAllEndpoints(payload, "return.rejected");
    }

    @EventListener
    @Async
    public void handleReturnRequestCompletedEvent(ReturnRequestCompletedEvent event) {
        if (!webhooksEnabled) return;
        
        WebhookPayload payload = createReturnRequestWebhookPayload(event.returnRequest(), "return.completed");
        sendWebhookToAllEndpoints(payload, "return.completed");
    }

    /**
     * Send webhook to all configured endpoints
     */
    private void sendWebhookToAllEndpoints(WebhookPayload payload, String eventType) {
        for (String endpoint : webhookEndpoints) {
            try {
                sendWebhookWithRetry(endpoint, payload, eventType, 1);
            } catch (Exception e) {
                log.error("Failed to send webhook to endpoint {} after all retries: {}", 
                        endpoint, e.getMessage(), e);
            }
        }
    }

    /**
     * Send webhook with retry logic
     */
    private void sendWebhookWithRetry(String endpoint, WebhookPayload payload, String eventType, int attempt) {
        try {
            sendWebhook(endpoint, payload, eventType);
            log.info("Webhook sent successfully to {} for event {} (attempt {})", 
                    endpoint, eventType, attempt);
        } catch (Exception e) {
            log.warn("Webhook attempt {} failed for endpoint {}: {}", attempt, endpoint, e.getMessage());
            
            if (attempt < maxRetryAttempts) {
                // Exponential backoff: wait 2^attempt seconds
                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                    sendWebhookWithRetry(endpoint, payload, eventType, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Webhook retry interrupted", ie);
                }
            } else {
                throw new RuntimeException("Max retry attempts exceeded for webhook", e);
            }
        }
    }

    /**
     * Send individual webhook
     */
    private void sendWebhook(String endpoint, WebhookPayload payload, String eventType) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String signature = generateSignature(jsonPayload);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Bharatshop-Event", eventType);
            headers.set("X-Bharatshop-Signature", signature);
            headers.set("X-Bharatshop-Timestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("User-Agent", "BharatShop-Webhooks/1.0");
            
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Webhook endpoint returned non-2xx status: " + 
                        response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("Webhook sending failed", e);
        }
    }

    /**
     * Generate HMAC signature for webhook payload
     */
    private String generateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate webhook signature: {}", e.getMessage());
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    /**
     * Create webhook payload for order status changes
     */
    private WebhookPayload createOrderStatusWebhookPayload(OrderStatusChangeEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_id", event.getOrderId());
        data.put("tenant_id", event.getTenantId());
        data.put("customer_id", event.getCustomerId());
        data.put("previous_status", event.getFromStatus());
        data.put("new_status", event.getToStatus());
        data.put("reason", null); // No reason field in OrderStatusChangeEvent
        data.put("timestamp", event.getTimestamp());
        data.put("is_terminal", event.isTerminalTransition());
        data.put("is_cancellation", event.isCancellation());
        data.put("is_delivery", event.isDelivery());
        data.put("is_return_related", event.isReturnRelated());
        
        return WebhookPayload.builder()
                .eventType("order.status.changed")
                .eventId(generateEventId())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    /**
     * Create webhook payload for return request events
     */
    private WebhookPayload createReturnRequestWebhookPayload(ReturnRequest returnRequest, String eventType) {
        Map<String, Object> data = new HashMap<>();
        data.put("return_request_id", returnRequest.getId());
        data.put("return_number", returnRequest.getReturnNumber());
        data.put("tenant_id", returnRequest.getTenantId());
        data.put("order_id", returnRequest.getOrderId());
        data.put("customer_id", returnRequest.getCustomerId());
        data.put("status", returnRequest.getStatus());
        data.put("return_type", returnRequest.getReturnType());
        data.put("reason", returnRequest.getReason());
        data.put("total_return_amount", returnRequest.getTotalReturnAmount());
        data.put("refund_amount", returnRequest.getRefundAmount());
        data.put("requested_at", returnRequest.getRequestedAt());
        
        // Add status-specific fields
        switch (returnRequest.getStatus()) {
            case APPROVED -> {
                data.put("approved_at", returnRequest.getApprovedAt());
                data.put("approved_by", returnRequest.getApprovedBy());
            }
            case REJECTED -> {
                data.put("rejected_at", returnRequest.getRejectedAt());
                data.put("rejected_by", returnRequest.getRejectedBy());
                data.put("rejection_reason", returnRequest.getRejectionReason());
            }
            case COMPLETED -> {
                data.put("completed_at", returnRequest.getCompletedAt());
                data.put("refund_processed_at", returnRequest.getRefundProcessedAt());
            }
        }
        
        return WebhookPayload.builder()
                .eventType(eventType)
                .eventId(generateEventId())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    /**
     * Generate unique event ID
     */
    private String generateEventId() {
        return "evt_" + System.currentTimeMillis() + "_" + 
               (int) (Math.random() * 10000);
    }

    /**
     * Verify webhook signature (for incoming webhooks)
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String expectedSignature = generateSignature(payload);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            System.out.println("Failed to verify webhook signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Manual webhook trigger for testing
     */
    public void triggerTestWebhook(String eventType, Map<String, Object> testData) {
        if (!webhooksEnabled) {
            throw BusinessException.invalidState("Webhooks", "Webhooks are disabled");
        }
        
        WebhookPayload payload = WebhookPayload.builder()
                .eventType("test." + eventType)
                .eventId(generateEventId())
                .timestamp(LocalDateTime.now())
                .data(testData)
                .build();
        
        sendWebhookToAllEndpoints(payload, "test." + eventType);
        System.out.println("Test webhook triggered for event type: " + eventType);
    }

    // Webhook payload class
    @lombok.Data
    @lombok.Builder
    public static class WebhookPayload {
        private String eventType;
        private String eventId;
        private LocalDateTime timestamp;
        private Map<String, Object> data;
        
        // Additional metadata
        @lombok.Builder.Default
        private String version = "1.0";
        
        @lombok.Builder.Default
        private String source = "bharatshop";
        

    }

    // Webhook configuration
    @lombok.Data
    public static class WebhookConfig {
        private String endpoint;
        private String secret;
        private boolean enabled;
        private List<String> eventTypes;
        private int timeoutMs;
        private int maxRetries;
    }
}
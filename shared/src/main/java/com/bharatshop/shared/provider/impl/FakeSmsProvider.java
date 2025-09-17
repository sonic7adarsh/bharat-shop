package com.bharatshop.shared.provider.impl;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;
import com.bharatshop.shared.provider.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fake SMS provider for testing and development.
 * Simulates SMS sending without actually sending messages.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.fake-providers.enabled", havingValue = "true")
@Slf4j
public class FakeSmsProvider implements SmsProvider {
    
    private static final Logger log = LoggerFactory.getLogger(FakeSmsProvider.class);
    private final Map<String, NotificationResponse> sentSms = new ConcurrentHashMap<>();
    private final Map<String, NotificationResponse.NotificationStatus> deliveryStatuses = new ConcurrentHashMap<>();
    private boolean simulateFailures = false;
    private double failureRate = 0.05; // 5% failure rate by default
    
    @Override
    public CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String messageId = "sms_" + UUID.randomUUID().toString();
            
            // Simulate processing delay
            try {
                Thread.sleep(50 + (long)(Math.random() * 100)); // 50-150ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            NotificationResponse response;
            
            if (simulateFailures && Math.random() < failureRate) {
                // Simulate failure
                response = NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage("Simulated SMS delivery failure")
                        .errorCode("FAKE_SMS_ERROR")
                        .providerName(getProviderName())
                        .attemptNumber(1)
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                // Simulate success
                response = NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .providerMessageId(messageId)
                        .status(NotificationResponse.NotificationStatus.SENT)
                        .sentAt(Instant.now())
                        .providerName(getProviderName())
                        .attemptNumber(1)
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .providerResponse(Map.of(
                                "recipient", request.getRecipient(),
                                "messageLength", request.getBody() != null ? request.getBody().length() : 0,
                                "parts", splitLongMessage(request.getBody()).size()
                        ))
                        .build();
                
                // Simulate delivery status updates
                deliveryStatuses.put(messageId, NotificationResponse.NotificationStatus.SENT);
                
                // Simulate delivery confirmation after a delay
                CompletableFuture.delayedExecutor(2, java.util.concurrent.TimeUnit.SECONDS)
                        .execute(() -> {
                            deliveryStatuses.put(messageId, NotificationResponse.NotificationStatus.DELIVERED);
                            log.debug("Fake SMS delivered: {}", messageId);
                        });
            }
            
            // Store for testing/verification
            sentSms.put(messageId, response);
            
            log.info("Fake SMS {} - To: {}, Length: {}, Status: {}", 
                    response.isSuccess() ? "sent" : "failed",
                    request.getRecipient(), 
                    request.getBody() != null ? request.getBody().length() : 0,
                    response.getStatus());
            
            return response;
        });
    }
    
    @Override
    public CompletableFuture<List<NotificationResponse>> sendBulkSms(List<NotificationRequest> requests) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Fake bulk SMS sending {} messages", requests.size());
            return requests.stream()
                    .map(request -> sendNotification(request).join())
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<NotificationResponse> getDeliveryStatus(String providerMessageId) {
        return CompletableFuture.supplyAsync(() -> {
            NotificationResponse originalResponse = sentSms.get(providerMessageId);
            if (originalResponse == null) {
                return NotificationResponse.builder()
                        .providerMessageId(providerMessageId)
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage("Message not found")
                        .errorCode("MESSAGE_NOT_FOUND")
                        .providerName(getProviderName())
                        .build();
            }
            
            NotificationResponse.NotificationStatus currentStatus = 
                    deliveryStatuses.getOrDefault(providerMessageId, originalResponse.getStatus());
            
            return originalResponse.toBuilder()
                    .status(currentStatus)
                    .deliveredAt(currentStatus == NotificationResponse.NotificationStatus.DELIVERED ? 
                            Instant.now() : null)
                    .build();
        });
    }
    
    @Override
    public String getProviderName() {
        return "FAKE_SMS";
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available for testing
    }
    
    // Testing utilities
    
    /**
     * Get all sent SMS for testing verification.
     */
    public Map<String, NotificationResponse> getSentSms() {
        return new ConcurrentHashMap<>(sentSms);
    }
    
    /**
     * Clear sent SMS history.
     */
    public void clearSentSms() {
        sentSms.clear();
        deliveryStatuses.clear();
    }
    
    /**
     * Enable/disable failure simulation.
     */
    public void setSimulateFailures(boolean simulateFailures) {
        this.simulateFailures = simulateFailures;
    }
    
    /**
     * Set failure rate for simulation (0.0 to 1.0).
     */
    public void setFailureRate(double failureRate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
    }
    
    /**
     * Get count of sent SMS.
     */
    public long getSentSmsCount() {
        return sentSms.values().stream()
                .filter(NotificationResponse::isSuccess)
                .count();
    }
    
    /**
     * Get count of failed SMS.
     */
    public long getFailedSmsCount() {
        return sentSms.values().stream()
                .filter(response -> !response.isSuccess())
                .count();
    }
    
    /**
     * Check if SMS was sent to specific recipient.
     */
    public boolean wasSmsSentTo(String recipient) {
        return sentSms.values().stream()
                .anyMatch(response -> {
                    Object providerResponseObj = response.getProviderResponse();
                    if (providerResponseObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> providerResponse = (Map<String, Object>) providerResponseObj;
                        return recipient.equals(providerResponse.get("recipient"));
                    }
                    return false;
                });
    }
    
    /**
     * Simulate delivery status change for testing.
     */
    public void simulateDeliveryStatusChange(String messageId, 
                                           NotificationResponse.NotificationStatus status) {
        if (sentSms.containsKey(messageId)) {
            deliveryStatuses.put(messageId, status);
            log.info("Simulated status change for {}: {}", messageId, status);
        }
    }
}
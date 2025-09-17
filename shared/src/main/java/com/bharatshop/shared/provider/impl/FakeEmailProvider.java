package com.bharatshop.shared.provider.impl;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;
import com.bharatshop.shared.provider.EmailProvider;
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

/**
 * Fake email provider for testing and development.
 * Simulates email sending without actually sending emails.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.fake-providers.enabled", havingValue = "true")
@Slf4j
public class FakeEmailProvider implements EmailProvider {
    
    // Manual log field since @Slf4j isn't working
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FakeEmailProvider.class);
    
    private final Map<String, NotificationResponse> sentEmails = new ConcurrentHashMap<>();
    private boolean simulateFailures = false;
    private double failureRate = 0.1; // 10% failure rate by default
    
    @Override
    public CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String messageId = UUID.randomUUID().toString();
            
            // Simulate processing delay
            try {
                Thread.sleep(100 + (long)(Math.random() * 200)); // 100-300ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            NotificationResponse response;
            
            if (simulateFailures && Math.random() < failureRate) {
                // Simulate failure
                response = NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage("Simulated email delivery failure")
                        .errorCode("FAKE_ERROR")
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
                                "subject", request.getSubject(),
                                "bodyLength", request.getBody() != null ? request.getBody().length() : 0
                        ))
                        .build();
            }
            
            // Store for testing/verification
            sentEmails.put(messageId, response);
            
            log.info("Fake email {} - To: {}, Subject: {}, Status: {}", 
                    response.isSuccess() ? "sent" : "failed",
                    request.getRecipient(), 
                    request.getSubject(),
                    response.getStatus());
            
            return response;
        });
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendEmailWithAttachments(
            NotificationRequest request, List<String> attachments) {
        log.info("Fake email with {} attachments to: {}", 
                attachments != null ? attachments.size() : 0, request.getRecipient());
        return sendNotification(request);
    }
    
    @Override
    public CompletableFuture<List<NotificationResponse>> sendBulkEmails(List<NotificationRequest> requests) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Fake bulk email sending {} emails", requests.size());
            return requests.stream()
                    .map(request -> sendNotification(request).join())
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public String getProviderName() {
        return "FAKE_EMAIL";
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available for testing
    }
    
    // Testing utilities
    
    /**
     * Get all sent emails for testing verification.
     */
    public Map<String, NotificationResponse> getSentEmails() {
        return new ConcurrentHashMap<>(sentEmails);
    }
    
    /**
     * Clear sent emails history.
     */
    public void clearSentEmails() {
        sentEmails.clear();
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
     * Get count of sent emails.
     */
    public long getSentEmailCount() {
        return sentEmails.values().stream()
                .filter(NotificationResponse::isSuccess)
                .count();
    }
    
    /**
     * Get count of failed emails.
     */
    public long getFailedEmailCount() {
        return sentEmails.values().stream()
                .filter(response -> !response.isSuccess())
                .count();
    }
    
    /**
     * Check if email was sent to specific recipient.
     */
    public boolean wasEmailSentTo(String recipient) {
        return sentEmails.values().stream()
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
}
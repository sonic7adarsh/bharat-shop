package com.bharatshop.shared.provider.impl;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;
import com.bharatshop.shared.provider.WhatsAppProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake WhatsApp provider for testing and development.
 * Simulates WhatsApp message sending without actually sending messages.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.fake-providers.enabled", havingValue = "true")
@Slf4j
public class FakeWhatsAppProvider implements WhatsAppProvider {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FakeWhatsAppProvider.class);
    
    private final Map<String, NotificationResponse> sentMessages = new ConcurrentHashMap<>();
    private final Map<String, NotificationResponse.NotificationStatus> messageStatuses = new ConcurrentHashMap<>();
    private final List<String> availableTemplates = List.of(
            "order_confirmation",
            "payment_received",
            "order_shipped",
            "order_delivered",
            "refund_processed",
            "abandoned_cart_reminder"
    );
    
    private boolean simulateFailures = false;
    private double failureRate = 0.03; // 3% failure rate by default
    
    @Override
    public CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request) {
        return sendTemplateMessage(request, "default_template", Map.of());
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendTemplateMessage(
            NotificationRequest request, String templateName, Map<String, Object> templateParams) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String messageId = "wa_" + UUID.randomUUID().toString();
            
            // Simulate processing delay
            try {
                Thread.sleep(150 + (long)(Math.random() * 200)); // 150-350ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            NotificationResponse response;
            
            if (simulateFailures && Math.random() < failureRate) {
                // Simulate failure
                response = NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage("Simulated WhatsApp delivery failure")
                        .errorCode("FAKE_WHATSAPP_ERROR")
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
                                "recipient", formatWhatsAppNumber(request.getRecipient()),
                                "template", templateName,
                                "templateParams", templateParams,
                                "messageType", "template"
                        ))
                        .build();
                
                // Simulate message status updates
                messageStatuses.put(messageId, NotificationResponse.NotificationStatus.SENT);
                
                // Simulate delivery confirmation after a delay
                CompletableFuture.delayedExecutor(3, java.util.concurrent.TimeUnit.SECONDS)
                        .execute(() -> {
                            messageStatuses.put(messageId, NotificationResponse.NotificationStatus.DELIVERED);
                            log.debug("Fake WhatsApp message delivered: {}", messageId);
                        });
            }
            
            // Store for testing/verification
            sentMessages.put(messageId, response);
            
            log.info("Fake WhatsApp {} - To: {}, Template: {}, Status: {}", 
                    response.isSuccess() ? "sent" : "failed",
                    request.getRecipient(), 
                    templateName,
                    response.getStatus());
            
            return response;
        });
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendMediaMessage(
            NotificationRequest request, String mediaUrl, String mediaType) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String messageId = "wa_media_" + UUID.randomUUID().toString();
            
            // Simulate processing delay for media
            try {
                Thread.sleep(300 + (long)(Math.random() * 400)); // 300-700ms delay for media
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            NotificationResponse response = NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .providerMessageId(messageId)
                    .status(NotificationResponse.NotificationStatus.SENT)
                    .sentAt(Instant.now())
                    .providerName(getProviderName())
                    .attemptNumber(1)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .providerResponse(Map.of(
                            "recipient", formatWhatsAppNumber(request.getRecipient()),
                            "mediaUrl", mediaUrl,
                            "mediaType", mediaType,
                            "messageType", "media"
                    ))
                    .build();
            
            sentMessages.put(messageId, response);
            
            log.info("Fake WhatsApp media sent - To: {}, Type: {}, URL: {}", 
                    request.getRecipient(), mediaType, mediaUrl);
            
            return response;
        });
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendInteractiveMessage(
            NotificationRequest request, Map<String, Object> interactiveContent) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String messageId = "wa_interactive_" + UUID.randomUUID().toString();
            
            // Simulate processing delay for interactive content
            try {
                Thread.sleep(200 + (long)(Math.random() * 300)); // 200-500ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            NotificationResponse response = NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .providerMessageId(messageId)
                    .status(NotificationResponse.NotificationStatus.SENT)
                    .sentAt(Instant.now())
                    .providerName(getProviderName())
                    .attemptNumber(1)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .providerResponse(Map.of(
                            "recipient", formatWhatsAppNumber(request.getRecipient()),
                            "interactiveContent", interactiveContent,
                            "messageType", "interactive"
                    ))
                    .build();
            
            sentMessages.put(messageId, response);
            
            log.info("Fake WhatsApp interactive message sent - To: {}, Content: {}", 
                    request.getRecipient(), interactiveContent.keySet());
            
            return response;
        });
    }
    
    @Override
    public CompletableFuture<NotificationResponse> getMessageStatus(String providerMessageId) {
        return CompletableFuture.supplyAsync(() -> {
            NotificationResponse originalResponse = sentMessages.get(providerMessageId);
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
                    messageStatuses.getOrDefault(providerMessageId, originalResponse.getStatus());
            
            return originalResponse.toBuilder()
                    .status(currentStatus)
                    .deliveredAt(currentStatus == NotificationResponse.NotificationStatus.DELIVERED ? 
                            Instant.now() : null)
                    .build();
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getAvailableTemplates() {
        return CompletableFuture.completedFuture(availableTemplates);
    }
    
    @Override
    public String getProviderName() {
        return "FAKE_WHATSAPP";
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available for testing
    }
    
    // Testing utilities
    
    /**
     * Get all sent messages for testing verification.
     */
    public Map<String, NotificationResponse> getSentMessages() {
        return new ConcurrentHashMap<>(sentMessages);
    }
    
    /**
     * Clear sent messages history.
     */
    public void clearSentMessages() {
        sentMessages.clear();
        messageStatuses.clear();
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
     * Get count of sent messages.
     */
    public long getSentMessageCount() {
        return sentMessages.values().stream()
                .filter(NotificationResponse::isSuccess)
                .count();
    }
    
    /**
     * Get count of failed messages.
     */
    public long getFailedMessageCount() {
        return sentMessages.values().stream()
                .filter(response -> !response.isSuccess())
                .count();
    }
    
    /**
     * Check if message was sent to specific recipient.
     */
    public boolean wasMessageSentTo(String recipient) {
        String formattedRecipient = formatWhatsAppNumber(recipient);
        return sentMessages.values().stream()
                .anyMatch(response -> {
                    Object providerResponseObj = response.getProviderResponse();
                    if (providerResponseObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> providerResponse = (Map<String, Object>) providerResponseObj;
                        return formattedRecipient.equals(providerResponse.get("recipient"));
                    }
                    return false;
                });
    }
    
    /**
     * Simulate message status change for testing.
     */
    public void simulateMessageStatusChange(String messageId, 
                                          NotificationResponse.NotificationStatus status) {
        if (sentMessages.containsKey(messageId)) {
            messageStatuses.put(messageId, status);
            log.info("Simulated WhatsApp status change for {}: {}", messageId, status);
        }
    }
}
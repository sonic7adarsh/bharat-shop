package com.bharatshop.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Request DTO for sending notifications through various providers.
 */
@Data
@Builder
public class NotificationRequest {
    
    private String notificationId;
    private String tenantId;
    private String eventType;
    private String channel; // EMAIL, SMS, WHATSAPP
    private String recipient; // email address, phone number, etc.
    private String subject;
    private String body;
    private String htmlBody; // For email notifications
    private Map<String, Object> metadata;
    private Instant scheduledAt;
    private Integer priority; // 1 (highest) to 5 (lowest)
    private Integer maxRetries;
    
    // Recipient information
    private String recipientName;
    

    
    // Manual getter methods since @Data isn't working
    public String getBody() { return body; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getNotificationId() { return notificationId; }
    public String getHtmlBody() {
        return htmlBody;
    }
    
    public String getChannel() {
        return channel;
    }
    
    private String recipientLocale;
    
    // Template information
    private String templateId;
    private Map<String, Object> templateVariables;
    
    // Provider-specific configuration
    private Map<String, Object> providerConfig;
}
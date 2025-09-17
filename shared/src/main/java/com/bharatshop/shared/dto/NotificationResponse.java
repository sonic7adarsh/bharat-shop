package com.bharatshop.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for notification delivery status and results.
 */
@Data
@Builder
public class NotificationResponse {
    
    private String notificationId;
    private String providerMessageId; // Provider's unique message ID
    private NotificationStatus status;
    private String errorMessage;
    private String errorCode;
    private Object providerResponse;
    private Instant sentAt;
    private Instant deliveredAt;
    private String providerName;
    private Integer attemptNumber;
    private Long processingTimeMs;
    private java.time.LocalDateTime timestamp;
    
    public enum NotificationStatus {
        PENDING,
        SENT,
        DELIVERED,
        FAILED,
        BOUNCED,
        REJECTED,
        RETRY_SCHEDULED
    }
    
    public boolean isSuccess() {
        return status == NotificationStatus.SENT || status == NotificationStatus.DELIVERED;
    }
    
    public boolean shouldRetry() {
        return status == NotificationStatus.FAILED || status == NotificationStatus.RETRY_SCHEDULED;
    }
    
    public boolean isFinalFailure() {
        return status == NotificationStatus.BOUNCED || status == NotificationStatus.REJECTED;
    }
    

    
    // Manual getter methods since @Data isn't working
    public NotificationStatus getStatus() { return status; }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public NotificationResponseBuilder toBuilder() {
        return NotificationResponse.builder()
                .notificationId(this.notificationId)
                .providerMessageId(this.providerMessageId)
                .status(this.status)
                .sentAt(this.sentAt)
                .deliveredAt(this.deliveredAt)
                .providerName(this.providerName)
                .attemptNumber(this.attemptNumber)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .errorCode(this.errorCode)
                .providerResponse(this.providerResponse)
                .timestamp(this.timestamp);
    }
    
    public Object getProviderResponse() {
        return providerResponse;
    }
}
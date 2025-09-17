package com.bharatshop.shared.provider;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Email-specific notification provider interface.
 */
public interface EmailProvider extends NotificationProvider {
    
    @Override
    default String getSupportedChannel() {
        return "EMAIL";
    }
    
    /**
     * Send email with attachments support.
     * 
     * @param request The email notification request
     * @param attachments List of attachment file paths or URLs
     * @return CompletableFuture containing the notification response
     */
    CompletableFuture<NotificationResponse> sendEmailWithAttachments(
            NotificationRequest request, List<String> attachments);
    
    /**
     * Send bulk emails (for newsletters, promotions, etc.).
     * 
     * @param requests List of email requests
     * @return CompletableFuture containing list of responses
     */
    CompletableFuture<List<NotificationResponse>> sendBulkEmails(List<NotificationRequest> requests);
    
    /**
     * Validate email address format.
     * 
     * @param email The email address to validate
     * @return true if email format is valid
     */
    default boolean isValidEmailAddress(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    @Override
    default boolean canHandle(NotificationRequest request) {
        return "EMAIL".equalsIgnoreCase(request.getChannel()) 
                && isValidEmailAddress(request.getRecipient());
    }
}
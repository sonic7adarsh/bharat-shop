package com.bharatshop.shared.provider;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * WhatsApp-specific notification provider interface.
 */
public interface WhatsAppProvider extends NotificationProvider {
    
    @Override
    default String getSupportedChannel() {
        return "WHATSAPP";
    }
    
    /**
     * Send WhatsApp message with template support.
     * 
     * @param request The WhatsApp notification request
     * @param templateName WhatsApp Business API template name
     * @param templateParams Template parameters
     * @return CompletableFuture containing the notification response
     */
    CompletableFuture<NotificationResponse> sendTemplateMessage(
            NotificationRequest request, String templateName, Map<String, Object> templateParams);
    
    /**
     * Send WhatsApp message with media (image, document, etc.).
     * 
     * @param request The WhatsApp notification request
     * @param mediaUrl URL of the media to send
     * @param mediaType Type of media (image, document, video, audio)
     * @return CompletableFuture containing the notification response
     */
    CompletableFuture<NotificationResponse> sendMediaMessage(
            NotificationRequest request, String mediaUrl, String mediaType);
    
    /**
     * Send interactive WhatsApp message (buttons, lists, etc.).
     * 
     * @param request The WhatsApp notification request
     * @param interactiveContent Interactive content definition
     * @return CompletableFuture containing the notification response
     */
    CompletableFuture<NotificationResponse> sendInteractiveMessage(
            NotificationRequest request, Map<String, Object> interactiveContent);
    
    /**
     * Get message delivery status from WhatsApp.
     * 
     * @param providerMessageId The provider's message ID
     * @return CompletableFuture containing delivery status
     */
    CompletableFuture<NotificationResponse> getMessageStatus(String providerMessageId);
    
    /**
     * Validate WhatsApp phone number format.
     * WhatsApp requires phone numbers in international format without + sign.
     * 
     * @param phoneNumber The phone number to validate
     * @return true if phone number format is valid for WhatsApp
     */
    default boolean isValidWhatsAppNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^\\d]", "");
        
        // WhatsApp numbers should be 10-15 digits
        return cleaned.matches("^[1-9]\\d{9,14}$");
    }
    
    /**
     * Format phone number for WhatsApp (remove + and non-digits).
     * 
     * @param phoneNumber The phone number to format
     * @return Formatted phone number for WhatsApp
     */
    default String formatWhatsAppNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        
        // Remove all non-digit characters including +
        return phoneNumber.replaceAll("[^\\d]", "");
    }
    
    /**
     * Check if template message is required based on WhatsApp Business API rules.
     * 
     * @param request The notification request
     * @return true if template message is required
     */
    default boolean requiresTemplate(NotificationRequest request) {
        // WhatsApp Business API requires templates for notifications
        // outside of 24-hour customer service window
        return true; // Simplified - in real implementation, check conversation window
    }
    
    /**
     * Get available WhatsApp message templates.
     * 
     * @return List of available template names
     */
    CompletableFuture<List<String>> getAvailableTemplates();
    
    @Override
    default boolean canHandle(NotificationRequest request) {
        return "WHATSAPP".equalsIgnoreCase(request.getChannel()) 
                && isValidWhatsAppNumber(request.getRecipient());
    }
}
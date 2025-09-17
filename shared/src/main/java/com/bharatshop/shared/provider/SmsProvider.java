package com.bharatshop.shared.provider;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SMS-specific notification provider interface.
 */
public interface SmsProvider extends NotificationProvider {
    
    @Override
    default String getSupportedChannel() {
        return "SMS";
    }
    
    /**
     * Send bulk SMS messages.
     * 
     * @param requests List of SMS requests
     * @return CompletableFuture containing list of responses
     */
    CompletableFuture<List<NotificationResponse>> sendBulkSms(List<NotificationRequest> requests);
    
    /**
     * Get SMS delivery status by provider message ID.
     * 
     * @param providerMessageId The provider's message ID
     * @return CompletableFuture containing delivery status
     */
    CompletableFuture<NotificationResponse> getDeliveryStatus(String providerMessageId);
    
    /**
     * Validate phone number format.
     * 
     * @param phoneNumber The phone number to validate
     * @return true if phone number format is valid
     */
    default boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        
        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        
        // Check if it starts with + and has 10-15 digits
        return cleaned.matches("^\\+?[1-9]\\d{9,14}$");
    }
    
    /**
     * Format phone number to international format.
     * 
     * @param phoneNumber The phone number to format
     * @param defaultCountryCode Default country code if not provided
     * @return Formatted phone number
     */
    default String formatPhoneNumber(String phoneNumber, String defaultCountryCode) {
        if (phoneNumber == null) return null;
        
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        
        // Add default country code if not present
        if (defaultCountryCode != null && !defaultCountryCode.startsWith("+")) {
            defaultCountryCode = "+" + defaultCountryCode;
        }
        
        return defaultCountryCode + cleaned;
    }
    
    /**
     * Check message length and split if necessary.
     * 
     * @param message The SMS message
     * @return List of message parts (for long SMS)
     */
    default List<String> splitLongMessage(String message) {
        if (message == null || message.length() <= 160) {
            return List.of(message != null ? message : "");
        }
        
        // Split into 153-character chunks (leaving room for part indicators)
        List<String> parts = new java.util.ArrayList<>();
        int maxLength = 153;
        
        for (int i = 0; i < message.length(); i += maxLength) {
            int end = Math.min(i + maxLength, message.length());
            parts.add(message.substring(i, end));
        }
        
        return parts;
    }
    
    @Override
    default boolean canHandle(NotificationRequest request) {
        return "SMS".equalsIgnoreCase(request.getChannel()) 
                && isValidPhoneNumber(request.getRecipient());
    }
}
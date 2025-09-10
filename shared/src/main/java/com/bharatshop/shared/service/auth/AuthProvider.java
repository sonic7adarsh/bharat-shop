package com.bharatshop.shared.service.auth;

import java.util.Map;

/**
 * Interface for pluggable authentication providers (SMS, Email, etc.)
 */
public interface AuthProvider {
    
    /**
     * Get the unique identifier for this provider
     */
    String getProviderId();
    
    /**
     * Get the display name for this provider
     */
    String getProviderName();
    
    /**
     * Check if this provider supports the given contact method
     * @param contactMethod phone number, email, etc.
     * @return true if supported
     */
    boolean supports(String contactMethod);
    
    /**
     * Send OTP to the specified contact method
     * @param contactMethod phone number or email
     * @param otpCode the OTP code to send
     * @param templateData additional data for message template
     * @return result containing success status and provider-specific data
     */
    AuthProviderResult sendOtp(String contactMethod, String otpCode, Map<String, Object> templateData);
    
    /**
     * Validate the format of contact method for this provider
     * @param contactMethod the contact method to validate
     * @return true if format is valid
     */
    boolean isValidContactMethod(String contactMethod);
    
    /**
     * Get the maximum OTP length supported by this provider
     */
    int getMaxOtpLength();
    
    /**
     * Get the minimum OTP length supported by this provider
     */
    int getMinOtpLength();
    
    /**
     * Check if this provider is currently available/healthy
     */
    boolean isHealthy();
    
    /**
     * Get provider-specific configuration or status
     */
    Map<String, Object> getProviderInfo();
    
    /**
     * Result class for provider operations
     */
    class AuthProviderResult {
        private final boolean success;
        private final String message;
        private final String providerMessageId;
        private final Map<String, Object> metadata;
        private final Exception error;
        
        public AuthProviderResult(boolean success, String message, String providerMessageId, 
                                Map<String, Object> metadata, Exception error) {
            this.success = success;
            this.message = message;
            this.providerMessageId = providerMessageId;
            this.metadata = metadata;
            this.error = error;
        }
        
        public static AuthProviderResult success(String message, String providerMessageId) {
            return new AuthProviderResult(true, message, providerMessageId, null, null);
        }
        
        public static AuthProviderResult success(String message, String providerMessageId, 
                                               Map<String, Object> metadata) {
            return new AuthProviderResult(true, message, providerMessageId, metadata, null);
        }
        
        public static AuthProviderResult failure(String message, Exception error) {
            return new AuthProviderResult(false, message, null, null, error);
        }
        
        public static AuthProviderResult failure(String message) {
            return new AuthProviderResult(false, message, null, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getProviderMessageId() { return providerMessageId; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Exception getError() { return error; }
    }
}
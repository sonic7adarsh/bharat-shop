package com.bharatshop.shared.provider;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all notification providers.
 * Implementations should be thread-safe and handle failures gracefully.
 */
public interface NotificationProvider {
    
    /**
     * Send a notification asynchronously.
     * 
     * @param request The notification request
     * @return CompletableFuture containing the notification response
     */
    CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request);
    
    /**
     * Get the channel this provider supports (EMAIL, SMS, WHATSAPP, etc.)
     * 
     * @return The supported channel
     */
    String getSupportedChannel();
    
    /**
     * Get the provider name/identifier.
     * 
     * @return The provider name
     */
    String getProviderName();
    
    /**
     * Check if the provider is currently available/healthy.
     * 
     * @return true if the provider is available
     */
    boolean isAvailable();
    
    /**
     * Validate if the provider can handle the given request.
     * 
     * @param request The notification request to validate
     * @return true if the request is valid for this provider
     */
    boolean canHandle(NotificationRequest request);
    
    /**
     * Get provider-specific configuration requirements.
     * Used for validation and setup.
     * 
     * @return Configuration schema or requirements
     */
    default String getConfigurationSchema() {
        return "{}";
    }
    
    /**
     * Initialize the provider with configuration.
     * Called during application startup.
     * 
     * @param config Provider-specific configuration
     */
    default void initialize(Object config) {
        // Default implementation does nothing
    }
    
    /**
     * Cleanup resources when shutting down.
     */
    default void shutdown() {
        // Default implementation does nothing
    }
}
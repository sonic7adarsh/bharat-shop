package com.bharatshop.shared.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all domain events in the system.
 * Domain events represent significant business occurrences that other parts of the system may need to react to.
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this event instance
     */
    UUID getEventId();
    
    /**
     * Type of the event (e.g., "OrderPlaced", "PaymentCaptured")
     */
    String getEventType();
    
    /**
     * Timestamp when the event occurred
     */
    Instant getOccurredAt();
    
    /**
     * Tenant ID for multi-tenant support
     */
    String getTenantId();
    
    /**
     * User ID who triggered the event (if applicable)
     */
    String getUserId();
    
    /**
     * Additional event data as key-value pairs
     */
    Map<String, Object> getEventData();
    
    /**
     * Version of the event schema for backward compatibility
     */
    default String getVersion() {
        return "1.0";
    }
}
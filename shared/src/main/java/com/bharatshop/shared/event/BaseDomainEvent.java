package com.bharatshop.shared.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for domain events providing common functionality.
 */
@Getter
@RequiredArgsConstructor
public abstract class BaseDomainEvent implements DomainEvent {
    
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();
    private final String tenantId;
    private final String userId;
    private final Map<String, Object> eventData = new HashMap<>();
    
    protected BaseDomainEvent(String tenantId, String userId, Map<String, Object> additionalData) {
        this.tenantId = tenantId;
        this.userId = userId;
        if (additionalData != null) {
            this.eventData.putAll(additionalData);
        }
    }
    
    /**
     * Add additional data to the event
     */
    protected void addEventData(String key, Object value) {
        this.eventData.put(key, value);
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getUserId() {
        return userId;
    }
    
    @Override
    public String getTenantId() {
        return tenantId;
    }
    
    @Override
    public Map<String, Object> getEventData() {
        return new HashMap<>(eventData);
    }
    
    @Override
    public String toString() {
        return String.format("%s{eventId=%s, tenantId='%s', userId='%s', occurredAt=%s}",
                getEventType(), eventId, tenantId, userId, occurredAt);
    }
}
package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * ShipmentTracking entity to store tracking history and status updates for shipments.
 * Each record represents a tracking event or status change in the shipment lifecycle.
 */
@Entity
@Table(name = "shipment_tracking", indexes = {
        @Index(name = "idx_shipment_tracking_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_shipment_tracking_shipment_id", columnList = "shipment_id"),
        @Index(name = "idx_shipment_tracking_status", columnList = "status"),
        @Index(name = "idx_shipment_tracking_event_date", columnList = "event_date"),
        @Index(name = "idx_shipment_tracking_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShipmentTracking extends BaseEntity {

    @Column(name = "shipment_id", nullable = false)
    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Tracking status is required")
    private Shipment.ShipmentStatus status;

    @Column(name = "event_date", nullable = false)
    @NotNull(message = "Event date is required")
    private LocalDateTime eventDate;

    @Column(name = "location", length = 200)
    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "carrier_status_code", length = 50)
    @Size(max = 50, message = "Carrier status code must not exceed 50 characters")
    private String carrierStatusCode; // Original status code from carrier API

    @Column(name = "carrier_status_message", length = 500)
    @Size(max = 500, message = "Carrier status message must not exceed 500 characters")
    private String carrierStatusMessage; // Original message from carrier API

    @Column(name = "event_type", length = 50)
    @Size(max = 50, message = "Event type must not exceed 50 characters")
    private String eventType; // e.g., "PICKUP", "SCAN", "DELIVERY_ATTEMPT", "DELIVERED"

    @Column(name = "is_milestone", nullable = false)
    private Boolean isMilestone = false; // Important tracking events

    @Column(name = "is_exception", nullable = false)
    private Boolean isException = false; // Exception events (delays, failures, etc.)

    @Column(name = "exception_reason", length = 200)
    @Size(max = 200, message = "Exception reason must not exceed 200 characters")
    private String exceptionReason;

    @Column(name = "source", length = 50)
    @Size(max = 50, message = "Source must not exceed 50 characters")
    private String source = "SYSTEM"; // "SYSTEM", "WEBHOOK", "API_POLL", "MANUAL"

    @Column(name = "raw_data", columnDefinition = "JSON")
    private String rawData; // Original JSON response from carrier API

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)
    private Shipment shipment;

    // Helper methods
    public boolean isDeliveryEvent() {
        return status == Shipment.ShipmentStatus.DELIVERED;
    }

    public boolean isFailureEvent() {
        return status == Shipment.ShipmentStatus.DELIVERY_FAILED || 
               status == Shipment.ShipmentStatus.RETURNED ||
               status == Shipment.ShipmentStatus.LOST ||
               status == Shipment.ShipmentStatus.DAMAGED;
    }

    public boolean isTransitEvent() {
        return status == Shipment.ShipmentStatus.IN_TRANSIT ||
               status == Shipment.ShipmentStatus.PICKED_UP ||
               status == Shipment.ShipmentStatus.OUT_FOR_DELIVERY;
    }

    @PrePersist
    private void prePersist() {
        if (eventDate == null) {
            eventDate = LocalDateTime.now();
        }
        
        // Set milestone flag for important events
        if (status == Shipment.ShipmentStatus.PICKED_UP ||
            status == Shipment.ShipmentStatus.OUT_FOR_DELIVERY ||
            status == Shipment.ShipmentStatus.DELIVERED ||
            status == Shipment.ShipmentStatus.DELIVERY_FAILED) {
            isMilestone = true;
        }
        
        // Set exception flag for problem events
        if (isFailureEvent()) {
            isException = true;
        }
    }
}
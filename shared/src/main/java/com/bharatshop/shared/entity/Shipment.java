package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shipment entity representing the shipping details and tracking information for orders.
 * Contains carrier information, tracking details, and delivery status.
 */
@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipment_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_shipment_order_id", columnList = "order_id"),
        @Index(name = "idx_shipment_tracking_number", columnList = "tracking_number"),
        @Index(name = "idx_shipment_carrier_code", columnList = "carrier_code"),
        @Index(name = "idx_shipment_status", columnList = "status"),
        @Index(name = "idx_shipment_shipped_date", columnList = "shipped_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shipment_order_id", columnNames = {"order_id"}),
        @UniqueConstraint(name = "uk_shipment_tracking_number", columnNames = {"tracking_number"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Shipment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Column(name = "service_zone_id")
    private Long serviceZoneId;

    // Carrier Information
    @Column(name = "carrier_code", nullable = false, length = 50)
    @NotBlank(message = "Carrier code is required")
    @Size(max = 50, message = "Carrier code must not exceed 50 characters")
    private String carrierCode; // e.g., "DTDC", "BLUEDART", "FEDEX"

    @Column(name = "carrier_name", nullable = false, length = 100)
    @NotBlank(message = "Carrier name is required")
    @Size(max = 100, message = "Carrier name must not exceed 100 characters")
    private String carrierName; // e.g., "DTDC Courier", "Blue Dart Express"

    @Column(name = "carrier_service_type", length = 50)
    @Size(max = 50, message = "Carrier service type must not exceed 50 characters")
    private String carrierServiceType; // e.g., "EXPRESS", "STANDARD", "OVERNIGHT"

    // Tracking Information
    @Column(name = "tracking_number", nullable = false, length = 100)
    @NotBlank(message = "Tracking number is required")
    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    private String trackingNumber;

    @Column(name = "tracking_url", length = 500)
    @Size(max = 500, message = "Tracking URL must not exceed 500 characters")
    private String trackingUrl;

    @Column(name = "carrier_tracking_id", length = 100)
    @Size(max = 100, message = "Carrier tracking ID must not exceed 100 characters")
    private String carrierTrackingId; // Internal carrier reference

    // Shipment Details
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Shipment status is required")
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    private BigDecimal weightKg;

    @Column(name = "dimensions", length = 100)
    @Size(max = 100, message = "Dimensions must not exceed 100 characters")
    private String dimensions; // e.g., "30x20x10 cm"

    @Column(name = "package_count", nullable = false)
    @NotNull(message = "Package count is required")
    @Min(value = 1, message = "Package count must be at least 1")
    private Integer packageCount = 1;

    // Shipping Costs
    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Shipping cost must be non-negative")
    private BigDecimal shippingCost;

    @Column(name = "cod_charges", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "COD charges must be non-negative")
    private BigDecimal codCharges;

    @Column(name = "total_charges", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Total charges must be non-negative")
    private BigDecimal totalCharges;

    // Delivery Information
    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    // Address Information (captured at time of shipment)
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_pincode", length = 10)
    @Pattern(regexp = "^[0-9]{6}$", message = "Delivery pincode must be a valid 6-digit pincode")
    private String deliveryPincode;

    @Column(name = "delivery_contact_name", length = 100)
    @Size(max = 100, message = "Delivery contact name must not exceed 100 characters")
    private String deliveryContactName;

    @Column(name = "delivery_contact_phone", length = 20)
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Delivery contact phone must be a valid phone number")
    private String deliveryContactPhone;

    // Tracking Updates
    @Column(name = "last_tracking_update")
    private LocalDateTime lastTrackingUpdate;

    @Column(name = "current_location", length = 200)
    @Size(max = 200, message = "Current location must not exceed 200 characters")
    private String currentLocation;

    @Column(name = "tracking_status_message", length = 500)
    @Size(max = 500, message = "Tracking status message must not exceed 500 characters")
    private String trackingStatusMessage;

    // Additional Information
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "delivery_attempts", nullable = false)
    @Min(value = 0, message = "Delivery attempts must be non-negative")
    private Integer deliveryAttempts = 0;

    @Column(name = "is_cod", nullable = false)
    private Boolean isCod = false;

    @Column(name = "is_fragile", nullable = false)
    private Boolean isFragile = false;

    @Column(name = "requires_signature", nullable = false)
    private Boolean requiresSignature = false;

    // Webhook/API Integration
    @Column(name = "webhook_url", length = 500)
    @Size(max = 500, message = "Webhook URL must not exceed 500 characters")
    private String webhookUrl;

    @Column(name = "api_response", columnDefinition = "JSON")
    private String apiResponse; // Last API response from carrier

    @Column(name = "last_api_sync")
    private LocalDateTime lastApiSync;

    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_zone_id", insertable = false, updatable = false)
    private ServiceZone serviceZone;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipmentTracking> trackingHistory;

    // Enums
    public enum ShipmentStatus {
        CREATED,        // Shipment record created
        LABEL_CREATED,  // Shipping label generated
        PICKED_UP,      // Package picked up by carrier
        IN_TRANSIT,     // Package in transit
        OUT_FOR_DELIVERY, // Out for delivery
        DELIVERED,      // Successfully delivered
        DELIVERY_FAILED, // Delivery attempt failed
        RETURNED,       // Package returned to sender
        CANCELLED,      // Shipment cancelled
        LOST,           // Package lost
        DAMAGED         // Package damaged
    }

    // Helper methods
    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED && deliveredDate != null;
    }

    public boolean isInTransit() {
        return status == ShipmentStatus.IN_TRANSIT || 
               status == ShipmentStatus.PICKED_UP || 
               status == ShipmentStatus.OUT_FOR_DELIVERY;
    }

    public boolean canBeTracked() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty() &&
               status != ShipmentStatus.CREATED && status != ShipmentStatus.CANCELLED;
    }

    public void updateTrackingStatus(ShipmentStatus newStatus, String location, String message) {
        this.status = newStatus;
        this.currentLocation = location;
        this.trackingStatusMessage = message;
        this.lastTrackingUpdate = LocalDateTime.now();
        
        // Update specific dates based on status
        switch (newStatus) {
            case PICKED_UP:
                if (shippedDate == null) {
                    shippedDate = LocalDateTime.now();
                }
                break;
            case DELIVERED:
                if (deliveredDate == null) {
                    deliveredDate = LocalDateTime.now();
                    actualDeliveryDate = LocalDateTime.now();
                }
                break;
            case DELIVERY_FAILED:
                deliveryAttempts++;
                break;
        }
    }

    public long getDaysInTransit() {
        if (shippedDate == null) {
            return 0;
        }
        
        LocalDateTime endDate = deliveredDate != null ? deliveredDate : LocalDateTime.now();
        return java.time.Duration.between(shippedDate, endDate).toDays();
    }

    public boolean isDelayed() {
        if (estimatedDeliveryDate == null || isDelivered()) {
            return false;
        }
        
        return LocalDateTime.now().isAfter(estimatedDeliveryDate);
    }

    @PrePersist
    private void prePersist() {
        if (totalCharges == null) {
            calculateTotalCharges();
        }
        if (trackingUrl == null && trackingNumber != null) {
            generateTrackingUrl();
        }
    }

    @PreUpdate
    private void preUpdate() {
        calculateTotalCharges();
    }

    private void calculateTotalCharges() {
        BigDecimal shipping = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        BigDecimal cod = codCharges != null ? codCharges : BigDecimal.ZERO;
        totalCharges = shipping.add(cod);
    }

    private void generateTrackingUrl() {
        if (carrierCode != null && trackingNumber != null) {
            // Generate carrier-specific tracking URLs
            switch (carrierCode.toUpperCase()) {
                case "DTDC":
                    trackingUrl = "https://www.dtdc.in/tracking/" + trackingNumber;
                    break;
                case "BLUEDART":
                    trackingUrl = "https://www.bluedart.com/web/guest/trackdartresult?trackFor=0&trackNo=" + trackingNumber;
                    break;
                case "FEDEX":
                    trackingUrl = "https://www.fedex.com/fedextrack/?tracknumbers=" + trackingNumber;
                    break;
                default:
                    trackingUrl = "#"; // Generic placeholder
            }
        }
    }
}
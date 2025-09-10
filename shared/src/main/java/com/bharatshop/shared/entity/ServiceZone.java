package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * ServiceZone entity representing shipping zones and rate cards.
 * Vendors can define zones with pincode ranges or explicit lists,
 * along with pricing and service level agreements.
 */
@Entity
@Table(name = "service_zones", indexes = {
        @Index(name = "idx_service_zone_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_service_zone_vendor_id", columnList = "vendor_id"),
        @Index(name = "idx_service_zone_name", columnList = "name"),
        @Index(name = "idx_service_zone_pin_range", columnList = "pin_from, pin_to"),
        @Index(name = "idx_service_zone_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_zone_tenant_vendor_name", 
                         columnNames = {"tenant_id", "vendor_id", "name"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ServiceZone extends BaseEntity {

    @Column(name = "vendor_id", nullable = false)
    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Zone name must not exceed 100 characters")
    private String name;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // Pincode range definition (for range-based zones)
    @Column(name = "pin_from", length = 10)
    @Pattern(regexp = "^[0-9]{6}$", message = "Pin from must be a valid 6-digit pincode")
    private String pinFrom;

    @Column(name = "pin_to", length = 10)
    @Pattern(regexp = "^[0-9]{6}$", message = "Pin to must be a valid 6-digit pincode")
    private String pinTo;

    // Explicit pincode list (JSON format for specific pincodes)
    @Column(name = "explicit_pincodes", columnDefinition = "JSON")
    private String explicitPincodes; // JSON array of pincodes

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false)
    @NotNull(message = "Zone type is required")
    private ZoneType zoneType = ZoneType.RANGE;

    // COD (Cash on Delivery) settings
    @Column(name = "cod_allowed", nullable = false)
    @NotNull(message = "COD allowed flag is required")
    private Boolean codAllowed = false;

    @Column(name = "cod_charges", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "COD charges must be non-negative")
    private BigDecimal codCharges = BigDecimal.ZERO;

    // Pricing structure
    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Base rate is required")
    @DecimalMin(value = "0.0", message = "Base rate must be non-negative")
    private BigDecimal baseRate;

    @Column(name = "per_kg_rate", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Per kg rate is required")
    @DecimalMin(value = "0.0", message = "Per kg rate must be non-negative")
    private BigDecimal perKgRate;

    @Column(name = "min_charge", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Minimum charge is required")
    @DecimalMin(value = "0.0", message = "Minimum charge must be non-negative")
    private BigDecimal minCharge;

    @Column(name = "max_weight_kg", precision = 8, scale = 3)
    @DecimalMin(value = "0.0", message = "Maximum weight must be non-negative")
    private BigDecimal maxWeightKg;

    // Service Level Agreement
    @Column(name = "sla_days", nullable = false)
    @NotNull(message = "SLA days is required")
    @Min(value = 1, message = "SLA days must be at least 1")
    @Max(value = 365, message = "SLA days must not exceed 365")
    private Integer slaDays;

    @Column(name = "sla_description", length = 200)
    @Size(max = 200, message = "SLA description must not exceed 200 characters")
    private String slaDescription;

    // Status and priority
    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Active status is required")
    private Boolean isActive = true;

    @Column(name = "priority", nullable = false)
    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority = 1; // Lower number = higher priority

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", insertable = false, updatable = false)
    private Vendor vendor;

    // Enums
    public enum ZoneType {
        RANGE,      // Uses pinFrom and pinTo
        EXPLICIT    // Uses explicitPincodes JSON
    }

    // Helper methods
    public boolean isPincodeInZone(String pincode) {
        if (pincode == null || pincode.length() != 6) {
            return false;
        }

        if (zoneType == ZoneType.RANGE) {
            return isPincodeInRange(pincode);
        } else if (zoneType == ZoneType.EXPLICIT) {
            return isPincodeInExplicitList(pincode);
        }
        
        return false;
    }

    private boolean isPincodeInRange(String pincode) {
        if (pinFrom == null || pinTo == null) {
            return false;
        }
        
        try {
            int pin = Integer.parseInt(pincode);
            int from = Integer.parseInt(pinFrom);
            int to = Integer.parseInt(pinTo);
            return pin >= from && pin <= to;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPincodeInExplicitList(String pincode) {
        if (explicitPincodes == null || explicitPincodes.trim().isEmpty()) {
            return false;
        }
        
        // Simple JSON array check - in production, use proper JSON parsing
        return explicitPincodes.contains("\"" + pincode + "\"");
    }

    public BigDecimal calculateShippingCost(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return minCharge;
        }

        // Check weight limit
        if (maxWeightKg != null && weightKg.compareTo(maxWeightKg) > 0) {
            throw new IllegalArgumentException("Weight exceeds maximum limit for this zone");
        }

        // Calculate: base rate + (weight * per kg rate)
        BigDecimal calculatedCost = baseRate.add(weightKg.multiply(perKgRate));
        
        // Ensure minimum charge
        return calculatedCost.compareTo(minCharge) < 0 ? minCharge : calculatedCost;
    }

    public BigDecimal getTotalCostWithCOD(BigDecimal shippingCost) {
        if (codAllowed && codCharges != null) {
            return shippingCost.add(codCharges);
        }
        return shippingCost;
    }

    @PrePersist
    @PreUpdate
    private void validateZoneConfiguration() {
        if (zoneType == ZoneType.RANGE) {
            if (pinFrom == null || pinTo == null) {
                throw new IllegalStateException("Pin from and pin to are required for RANGE type zones");
            }
            if (Integer.parseInt(pinFrom) > Integer.parseInt(pinTo)) {
                throw new IllegalStateException("Pin from must be less than or equal to pin to");
            }
        } else if (zoneType == ZoneType.EXPLICIT) {
            if (explicitPincodes == null || explicitPincodes.trim().isEmpty()) {
                throw new IllegalStateException("Explicit pincodes are required for EXPLICIT type zones");
            }
        }
        
        if (baseRate.compareTo(minCharge) > 0) {
            throw new IllegalStateException("Base rate cannot be greater than minimum charge");
        }
    }
}
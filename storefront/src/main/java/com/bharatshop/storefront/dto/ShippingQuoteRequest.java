package com.bharatshop.storefront.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for shipping quote calculation.
 * Contains destination details, package information, and delivery preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteRequest {

    @NotBlank(message = "Destination pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Destination pincode must be a valid 6-digit pincode")
    private String destPincode;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    @DecimalMax(value = "50.0", message = "Weight cannot exceed 50 kg")
    private BigDecimal weight;

    @Builder.Default
    private boolean cod = false; // Cash on Delivery flag

    @DecimalMin(value = "0.0", message = "Order value must be non-negative")
    private BigDecimal orderValue; // Required for COD charge calculation

    @Builder.Default
    private boolean expressDelivery = false; // Express delivery preference

    @Size(max = 100, message = "Dimensions must not exceed 100 characters")
    private String dimensions; // Package dimensions (optional)

    @Min(value = 1, message = "Package count must be at least 1")
    @Builder.Default
    private Integer packageCount = 1;

    @Builder.Default
    private boolean fragile = false; // Fragile item flag

    @Size(max = 200, message = "Special instructions must not exceed 200 characters")
    private String specialInstructions;

    // Helper methods
    public boolean isCod() {
        return cod;
    }

    public boolean isExpressDelivery() {
        return expressDelivery;
    }

    public boolean isFragile() {
        return fragile;
    }

    /**
     * Validate that order value is provided when COD is requested
     */
    public boolean isValid() {
        if (cod && (orderValue == null || orderValue.compareTo(BigDecimal.ZERO) <= 0)) {
            return false;
        }
        return true;
    }

    /**
     * Get effective weight considering package count
     */
    public BigDecimal getEffectiveWeight() {
        if (packageCount == null || packageCount <= 1) {
            return weight;
        }
        return weight.multiply(BigDecimal.valueOf(packageCount));
    }
}
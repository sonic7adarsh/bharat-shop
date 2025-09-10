package com.bharatshop.storefront.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for shipping quote calculation.
 * Contains shipping costs, delivery estimates, and service information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteResponse {

    private boolean available; // Whether shipping is available to the destination

    private Long serviceZoneId; // ID of the service zone used

    private String serviceZoneName; // Name of the service zone

    private BigDecimal shippingCost; // Base shipping cost

    private BigDecimal codCharges; // Cash on Delivery charges

    private BigDecimal totalCost; // Total shipping cost (shipping + COD)

    private Integer slaDays; // Service Level Agreement in days

    private LocalDateTime estimatedDeliveryDate; // Estimated delivery date

    private LocalDateTime promisedDeliveryDate; // Promised delivery date (with buffer)

    private boolean codAllowed; // Whether COD is allowed for this destination

    private boolean expressDeliveryAvailable; // Whether express delivery is available

    private String message; // Status or error message

    private List<String> deliveryTimeSlots; // Available delivery time slots

    private String carrierName; // Preferred carrier for this route

    private String carrierCode; // Carrier code

    private BigDecimal freeShippingThreshold; // Minimum order value for free shipping

    private boolean freeShippingEligible; // Whether order qualifies for free shipping

    // Additional service information
    private boolean signatureRequired; // Whether signature is required on delivery

    private boolean trackingAvailable; // Whether tracking is available

    private String specialInstructions; // Any special delivery instructions

    // Express delivery details (if available)
    private ExpressDeliveryOption expressOption;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpressDeliveryOption {
        private BigDecimal expressCost;
        private BigDecimal expressTotal;
        private Integer expressSlaDays;
        private LocalDateTime expressDeliveryDate;
        private String expressMessage;
    }

    // Helper methods
    public boolean isAvailable() {
        return available;
    }

    public boolean isCodAllowed() {
        return codAllowed;
    }

    public boolean isExpressDeliveryAvailable() {
        return expressDeliveryAvailable;
    }

    public boolean isFreeShippingEligible() {
        return freeShippingEligible;
    }

    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    public boolean isTrackingAvailable() {
        return trackingAvailable;
    }

    /**
     * Get cost savings if free shipping threshold is met
     */
    public BigDecimal getPotentialSavings() {
        if (freeShippingEligible) {
            return shippingCost;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get delivery time range as string
     */
    public String getDeliveryTimeRange() {
        if (slaDays == null) {
            return "Not available";
        }
        
        if (slaDays == 1) {
            return "Next day delivery";
        } else if (slaDays <= 3) {
            return slaDays + " days";
        } else {
            return slaDays + " business days";
        }
    }

    /**
     * Create an unavailable response with message
     */
    public static ShippingQuoteResponse unavailable(String message) {
        return ShippingQuoteResponse.builder()
                .available(false)
                .message(message)
                .build();
    }

    /**
     * Create a successful response with basic information
     */
    public static ShippingQuoteResponse success(Long serviceZoneId, String serviceZoneName, 
                                               BigDecimal shippingCost, BigDecimal codCharges, 
                                               Integer slaDays, LocalDateTime estimatedDelivery) {
        return ShippingQuoteResponse.builder()
                .available(true)
                .serviceZoneId(serviceZoneId)
                .serviceZoneName(serviceZoneName)
                .shippingCost(shippingCost)
                .codCharges(codCharges != null ? codCharges : BigDecimal.ZERO)
                .totalCost(shippingCost.add(codCharges != null ? codCharges : BigDecimal.ZERO))
                .slaDays(slaDays)
                .estimatedDeliveryDate(estimatedDelivery)
                .message("Shipping available")
                .build();
    }
}
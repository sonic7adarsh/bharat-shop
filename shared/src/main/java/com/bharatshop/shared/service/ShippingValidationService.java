package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.ServiceZone;
import com.bharatshop.shared.entity.Shipment;
import com.bharatshop.shared.entity.ShipmentTracking;
// import com.bharatshop.storefront.dto.ShippingQuoteRequest; // Removed - not available in shared module
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Centralized validation service for all shipping-related components.
 * Provides comprehensive validation for entities, DTOs, and business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingValidationService {

    private final Validator validator;
    
    // Validation patterns
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");
    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{8,20}$");
    private static final Pattern CARRIER_CODE_PATTERN = Pattern.compile("^[A-Z]{2,10}$");
    
    // Business rule constants
    private static final BigDecimal MIN_WEIGHT = new BigDecimal("0.1");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("50.0");
    private static final BigDecimal MIN_ORDER_VALUE = new BigDecimal("1.0");
    private static final BigDecimal MAX_ORDER_VALUE = new BigDecimal("100000.0");
    private static final int MIN_SLA_DAYS = 1;
    private static final int MAX_SLA_DAYS = 365;
    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    /**
     * Validate shipping quote request
     * Commented out due to ShippingQuoteRequest not being available in shared module
     */
    /*
    public ValidationResult validateShippingQuoteRequest(ShippingQuoteRequest request) {
        ValidationResult result = new ValidationResult();
        
        if (request == null) {
            result.addError("request", "Shipping quote request is required");
            return result;
        }
        
        // Standard validation annotations
        Set<ConstraintViolation<ShippingQuoteRequest>> violations = validator.validate(request);
        for (ConstraintViolation<ShippingQuoteRequest> violation : violations) {
            result.addError(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        // Business rule validations
        validatePincode(request.getDestPincode(), "destPincode", result);
        validateWeight(request.getWeight(), "weight", result);
        
        
        if (request.isCod()) {
            validateOrderValueForCOD(request.getOrderValue(), "orderValue", result);
        }
        
        if (request.getPackageCount() != null && request.getPackageCount() > 10) {
            result.addError("packageCount", "Package count cannot exceed 10");
        }
        
        // Validate effective weight
        BigDecimal effectiveWeight = request.getEffectiveWeight();
        if (effectiveWeight.compareTo(MAX_WEIGHT) > 0) {
            result.addError("effectiveWeight", "Total effective weight cannot exceed " + MAX_WEIGHT + " kg");
        }
        
        return result;
    }
    */

    /**
     * Validate service zone configuration
     */
    public ValidationResult validateServiceZone(ServiceZone serviceZone) {
        ValidationResult result = new ValidationResult();
        
        if (serviceZone == null) {
            result.addError("serviceZone", "Service zone is required");
            return result;
        }
        
        // Standard validation annotations
        Set<ConstraintViolation<ServiceZone>> violations = validator.validate(serviceZone);
        for (ConstraintViolation<ServiceZone> violation : violations) {
            result.addError(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        // Business rule validations
        validateServiceZoneConfiguration(serviceZone, result);
        validateServiceZonePricing(serviceZone, result);
        validateServiceZoneSLA(serviceZone, result);
        
        return result;
    }

    /**
     * Validate shipment entity
     */
    public ValidationResult validateShipment(Shipment shipment) {
        ValidationResult result = new ValidationResult();
        
        if (shipment == null) {
            result.addError("shipment", "Shipment is required");
            return result;
        }
        
        // Standard validation annotations
        Set<ConstraintViolation<Shipment>> violations = validator.validate(shipment);
        for (ConstraintViolation<Shipment> violation : violations) {
            result.addError(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        // Business rule validations
        validateShipmentTracking(shipment, result);
        validateShipmentDeliveryDetails(shipment, result);
        validateShipmentStatusTransition(shipment, result);
        
        return result;
    }

    /**
     * Validate shipment tracking event
     */
    public ValidationResult validateShipmentTracking(ShipmentTracking tracking) {
        ValidationResult result = new ValidationResult();
        
        if (tracking == null) {
            result.addError("tracking", "Shipment tracking is required");
            return result;
        }
        
        // Standard validation annotations
        Set<ConstraintViolation<ShipmentTracking>> violations = validator.validate(tracking);
        for (ConstraintViolation<ShipmentTracking> violation : violations) {
            result.addError(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        // Business rule validations
        validateTrackingEventDate(tracking, result);
        validateTrackingStatusTransition(tracking, result);
        
        return result;
    }

    /**
     * Validate pincode format and business rules
     */
    public ValidationResult validatePincode(String pincode) {
        ValidationResult result = new ValidationResult();
        validatePincode(pincode, "pincode", result);
        return result;
    }

    /**
     * Validate carrier information
     */
    public ValidationResult validateCarrierInfo(String carrierCode, String carrierName) {
        ValidationResult result = new ValidationResult();
        
        if (!StringUtils.hasText(carrierCode)) {
            result.addError("carrierCode", "Carrier code is required");
        } else if (!CARRIER_CODE_PATTERN.matcher(carrierCode.toUpperCase()).matches()) {
            result.addError("carrierCode", "Carrier code must be 2-10 uppercase letters");
        }
        
        if (!StringUtils.hasText(carrierName)) {
            result.addError("carrierName", "Carrier name is required");
        } else if (carrierName.length() > 100) {
            result.addError("carrierName", "Carrier name must not exceed 100 characters");
        }
        
        return result;
    }

    /**
     * Validate tracking number format
     */
    public ValidationResult validateTrackingNumber(String trackingNumber) {
        ValidationResult result = new ValidationResult();
        
        if (!StringUtils.hasText(trackingNumber)) {
            result.addError("trackingNumber", "Tracking number is required");
        } else if (!TRACKING_NUMBER_PATTERN.matcher(trackingNumber.toUpperCase()).matches()) {
            result.addError("trackingNumber", "Tracking number must be 8-20 alphanumeric characters");
        }
        
        return result;
    }

    // Private helper methods

    private void validatePincode(String pincode, String fieldName, ValidationResult result) {
        if (!StringUtils.hasText(pincode)) {
            result.addError(fieldName, "Pincode is required");
        } else if (!PINCODE_PATTERN.matcher(pincode).matches()) {
            result.addError(fieldName, "Pincode must be a valid 6-digit number");
        }
    }

    private void validateWeight(BigDecimal weight, String fieldName, ValidationResult result) {
        if (weight == null) {
            result.addError(fieldName, "Weight is required");
        } else if (weight.compareTo(MIN_WEIGHT) < 0) {
            result.addError(fieldName, "Weight must be at least " + MIN_WEIGHT + " kg");
        } else if (weight.compareTo(MAX_WEIGHT) > 0) {
            result.addError(fieldName, "Weight cannot exceed " + MAX_WEIGHT + " kg");
        }
    }

    private void validateOrderValueForCOD(BigDecimal orderValue, String fieldName, ValidationResult result) {
        if (orderValue == null) {
            result.addError(fieldName, "Order value is required for COD orders");
        } else if (orderValue.compareTo(MIN_ORDER_VALUE) < 0) {
            result.addError(fieldName, "Order value must be at least " + MIN_ORDER_VALUE);
        } else if (orderValue.compareTo(MAX_ORDER_VALUE) > 0) {
            result.addError(fieldName, "Order value cannot exceed " + MAX_ORDER_VALUE + " for COD");
        }
    }

    private void validateServiceZoneConfiguration(ServiceZone serviceZone, ValidationResult result) {
        if (serviceZone.getZoneType() == ServiceZone.ZoneType.RANGE) {
            if (!StringUtils.hasText(serviceZone.getPinFrom()) || !StringUtils.hasText(serviceZone.getPinTo())) {
                result.addError("pinRange", "Pin from and pin to are required for RANGE type zones");
            } else {
                validatePincode(serviceZone.getPinFrom(), "pinFrom", result);
                validatePincode(serviceZone.getPinTo(), "pinTo", result);
                
                if (result.isValid() && Integer.parseInt(serviceZone.getPinFrom()) > Integer.parseInt(serviceZone.getPinTo())) {
                    result.addError("pinRange", "Pin from must be less than or equal to pin to");
                }
            }
        } else if (serviceZone.getZoneType() == ServiceZone.ZoneType.EXPLICIT) {
            if (!StringUtils.hasText(serviceZone.getExplicitPincodes())) {
                result.addError("explicitPincodes", "Explicit pincodes are required for EXPLICIT type zones");
            } else {
                validateExplicitPincodes(serviceZone.getExplicitPincodes(), result);
            }
        }
    }

    private void validateServiceZonePricing(ServiceZone serviceZone, ValidationResult result) {
        if (serviceZone.getBaseRate() != null && serviceZone.getMinCharge() != null) {
            if (serviceZone.getBaseRate().compareTo(serviceZone.getMinCharge()) > 0) {
                result.addError("pricing", "Base rate cannot be greater than minimum charge");
            }
        }
        
        if (serviceZone.getCodAllowed() && serviceZone.getCodCharges() != null) {
            if (serviceZone.getCodCharges().compareTo(BigDecimal.ZERO) < 0) {
                result.addError("codCharges", "COD charges cannot be negative");
            }
        }
    }

    private void validateServiceZoneSLA(ServiceZone serviceZone, ValidationResult result) {
        if (serviceZone.getSlaDays() != null) {
            if (serviceZone.getSlaDays() < MIN_SLA_DAYS || serviceZone.getSlaDays() > MAX_SLA_DAYS) {
                result.addError("slaDays", "SLA days must be between " + MIN_SLA_DAYS + " and " + MAX_SLA_DAYS);
            }
        }
    }

    private void validateExplicitPincodes(String explicitPincodes, ValidationResult result) {
        try {
            // Basic JSON validation - in production, use proper JSON parsing
            if (!explicitPincodes.trim().startsWith("[") || !explicitPincodes.trim().endsWith("]")) {
                result.addError("explicitPincodes", "Explicit pincodes must be a valid JSON array");
                return;
            }
            
            // Extract pincodes and validate each one
            String[] pincodes = explicitPincodes.replaceAll("[\\[\\]\"\\s]", "").split(",");
            for (String pincode : pincodes) {
                if (StringUtils.hasText(pincode) && !PINCODE_PATTERN.matcher(pincode.trim()).matches()) {
                    result.addError("explicitPincodes", "Invalid pincode in explicit list: " + pincode.trim());
                }
            }
        } catch (Exception e) {
            result.addError("explicitPincodes", "Invalid JSON format for explicit pincodes");
        }
    }

    private void validateShipmentTracking(Shipment shipment, ValidationResult result) {
        if (StringUtils.hasText(shipment.getTrackingNumber())) {
            ValidationResult trackingResult = validateTrackingNumber(shipment.getTrackingNumber());
            result.addErrors(trackingResult.getErrors());
        }
        
        if (StringUtils.hasText(shipment.getCarrierCode())) {
            ValidationResult carrierResult = validateCarrierInfo(shipment.getCarrierCode(), shipment.getCarrierName());
            result.addErrors(carrierResult.getErrors());
        }
    }

    private void validateShipmentDeliveryDetails(Shipment shipment, ValidationResult result) {
        if (StringUtils.hasText(shipment.getDeliveryContactPhone())) {
            if (!PHONE_PATTERN.matcher(shipment.getDeliveryContactPhone()).matches()) {
                result.addError("deliveryContactPhone", "Invalid phone number format");
            }
        }
        
        if (shipment.getDeliveryAttempts() != null && shipment.getDeliveryAttempts() > MAX_DELIVERY_ATTEMPTS) {
            result.addError("deliveryAttempts", "Delivery attempts cannot exceed " + MAX_DELIVERY_ATTEMPTS);
        }
        
        // Validate delivery dates
        if (shipment.getShippedDate() != null && shipment.getDeliveredDate() != null) {
            if (shipment.getDeliveredDate().isBefore(shipment.getShippedDate())) {
                result.addError("deliveredDate", "Delivery date cannot be before shipped date");
            }
        }
    }

    private void validateShipmentStatusTransition(Shipment shipment, ValidationResult result) {
        // Validate status transitions based on business rules
        if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            if (shipment.getDeliveredDate() == null) {
                result.addError("deliveredDate", "Delivered date is required for delivered shipments");
            }
        }
        
        if (shipment.getStatus() == Shipment.ShipmentStatus.PICKED_UP) {
            if (shipment.getShippedDate() == null) {
                result.addError("shippedDate", "Shipped date is required for picked up shipments");
            }
        }
    }

    private void validateTrackingEventDate(ShipmentTracking tracking, ValidationResult result) {
        if (tracking.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (tracking.getEventDate().isAfter(now.plusHours(1))) {
                result.addError("eventDate", "Event date cannot be more than 1 hour in the future");
            }
            
            // Don't allow events older than 1 year
            if (tracking.getEventDate().isBefore(now.minusYears(1))) {
                result.addError("eventDate", "Event date cannot be older than 1 year");
            }
        }
    }

    private void validateTrackingStatusTransition(ShipmentTracking tracking, ValidationResult result) {
        // Add business rules for valid status transitions
        if (tracking.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            if (!StringUtils.hasText(tracking.getLocation())) {
                result.addError("location", "Location is required for delivery events");
            }
        }
    }

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final Map<String, List<String>> errors = new HashMap<>();
        
        public void addError(String field, String message) {
            errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
        }
        
        public void addErrors(Map<String, List<String>> newErrors) {
            for (Map.Entry<String, List<String>> entry : newErrors.entrySet()) {
                for (String error : entry.getValue()) {
                    addError(entry.getKey(), error);
                }
            }
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public Map<String, List<String>> getErrors() {
            return new HashMap<>(errors);
        }
        
        public List<String> getErrorMessages() {
            List<String> messages = new ArrayList<>();
            for (List<String> fieldErrors : errors.values()) {
                messages.addAll(fieldErrors);
            }
            return messages;
        }
        
        public String getFirstError() {
            return errors.values().stream()
                    .flatMap(List::stream)
                    .findFirst()
                    .orElse(null);
        }
        
        @Override
        public String toString() {
            if (isValid()) {
                return "ValidationResult: VALID";
            }
            return "ValidationResult: INVALID - " + String.join(", ", getErrorMessages());
        }
    }
}
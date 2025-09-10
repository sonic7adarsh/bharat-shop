package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.ServiceZone;
import com.bharatshop.shared.entity.Shipment;
import com.bharatshop.shared.entity.ShipmentTracking;
// ShippingQuoteRequest is in storefront module, not accessible from shared
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ShippingValidationService
 * Tests validation logic for shipping components
 */
@ExtendWith(MockitoExtension.class)
class ShippingValidationServiceTest {

    @Mock
    private Validator validator;

    @InjectMocks
    private ShippingValidationService validationService;

    @BeforeEach
    void setUp() {
        // Setup will be done per test as needed
    }

    // ShippingQuoteRequest tests removed - class is in storefront module, not accessible from shared

    @Test
    void testValidateServiceZone_ValidZone() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        
        ServiceZone serviceZone = ServiceZone.builder()
                .vendorId(1L)
                .name("Delhi NCR")
                .pinFrom("110001")
                .pinTo("110096")
                .zoneType(ServiceZone.ZoneType.RANGE)
                .codAllowed(true)
                .baseRate(BigDecimal.valueOf(30.0))
                .perKgRate(BigDecimal.valueOf(10.0))
                .minCharge(BigDecimal.valueOf(50.0))
                .slaDays(2)
                .isActive(true)
                .priority(1)
                .build();

        // When
        ShippingValidationService.ValidationResult result = validationService.validateServiceZone(serviceZone);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateServiceZone_NullZone() {
        // When
        ShippingValidationService.ValidationResult result = validationService.validateServiceZone(null);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().contains("Service zone is required"));
    }

    @Test
    void testValidateServiceZone_InvalidPinRange() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        
        ServiceZone serviceZone = ServiceZone.builder()
                .vendorId(1L)
                .name("Invalid Zone")
                .pinFrom("110096")
                .pinTo("110001") // Invalid: pinTo < pinFrom
                .zoneType(ServiceZone.ZoneType.RANGE)
                .codAllowed(false)
                .baseRate(BigDecimal.valueOf(50.0))
                .perKgRate(BigDecimal.valueOf(10.0))
                .minCharge(BigDecimal.valueOf(30.0))
                .slaDays(2)
                .isActive(true)
                .priority(1)
                .build();

        // When
        ShippingValidationService.ValidationResult result = validationService.validateServiceZone(serviceZone);

        // Then
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidateShipment_ValidShipment() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        
        Shipment shipment = Shipment.builder()
                .orderId(1L)
                .carrierCode("DELHIVERY")
                .carrierName("Delhivery")
                .trackingNumber("DL123456789")
                .status(Shipment.ShipmentStatus.IN_TRANSIT)
                .shippedDate(LocalDateTime.now().minusDays(1))
                .build();

        // When
        ShippingValidationService.ValidationResult result = validationService.validateShipment(shipment);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void testValidateShipment_NullShipment() {
        // When
        ShippingValidationService.ValidationResult result = validationService.validateShipment(null);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().contains("Shipment is required"));
    }

    @Test
    void testValidateShipment_DeliveredWithoutDate() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        
        Shipment shipment = Shipment.builder()
                .orderId(1L)
                .carrierCode("DELHIVERY")
                .carrierName("Delhivery")
                .trackingNumber("DL123456789")
                .status(Shipment.ShipmentStatus.DELIVERED)
                // Missing deliveredAt date
                .build();

        // When
        ShippingValidationService.ValidationResult result = validationService.validateShipment(shipment);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Delivered date is required for delivered shipments")));
    }

    @Test
    void testValidateTrackingNumber_Valid() {
        // When
        ShippingValidationService.ValidationResult result = validationService.validateTrackingNumber("DL123456789");

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void testValidateTrackingNumber_Invalid() {
        // When - too short (only 7 characters, need 8-20)
        ShippingValidationService.ValidationResult result = validationService.validateTrackingNumber("ABC123");

        // Then
        assertFalse(result.isValid());
    }

    @Test
    void testValidatePincode_Valid() {
        // When
        ShippingValidationService.ValidationResult result = validationService.validatePincode("110001");

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void testValidatePincode_Invalid() {
        // When - 5 digits instead of required 6
        ShippingValidationService.ValidationResult result = validationService.validatePincode("12345");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Pincode must be a valid 6-digit number")));
    }

    @Test
    void testValidateCarrierInfo_Valid() {
        // When
        ShippingValidationService.ValidationResult result = validationService.validateCarrierInfo("DELHIVERY", "Delhivery Pvt Ltd");

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void testValidateCarrierInfo_InvalidCode() {
        // When - only 1 character (need 2-10)
        ShippingValidationService.ValidationResult result = validationService.validateCarrierInfo("D", "Delhivery Pvt Ltd");

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Carrier code must be 2-10 uppercase letters")));
    }

    @Test
    void testValidationResult_AddMultipleErrors() {
        // Given
        ShippingValidationService.ValidationResult result = new ShippingValidationService.ValidationResult();

        // When
        result.addError("field1", "Error 1");
        result.addError("field1", "Error 2");
        result.addError("field2", "Error 3");

        // Then
        assertFalse(result.isValid());
        assertEquals(3, result.getErrorMessages().size());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().get("field1").contains("Error 1"));
        assertTrue(result.getErrors().get("field1").contains("Error 2"));
        assertTrue(result.getErrors().get("field2").contains("Error 3"));
    }

    @Test
    void testValidationResult_ToString() {
        // Given
        ShippingValidationService.ValidationResult validResult = new ShippingValidationService.ValidationResult();
        ShippingValidationService.ValidationResult invalidResult = new ShippingValidationService.ValidationResult();
        invalidResult.addError("field", "error message");

        // Then
        assertEquals("ValidationResult: VALID", validResult.toString());
        assertTrue(invalidResult.toString().contains("ValidationResult: INVALID"));
        assertTrue(invalidResult.toString().contains("error message"));
    }
}
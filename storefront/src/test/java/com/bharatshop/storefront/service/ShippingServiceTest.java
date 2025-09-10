package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.ServiceZone;
import com.bharatshop.storefront.dto.ShippingQuoteRequest;
import com.bharatshop.storefront.dto.ShippingQuoteResponse;
import com.bharatshop.storefront.repository.ServiceZoneRepository;
import com.bharatshop.shared.service.ShippingValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShippingService
 * Tests shipping quote calculation and service zone operations
 */
@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ServiceZoneRepository serviceZoneRepository;

    @Mock
    private ShippingValidationService validationService;

    @InjectMocks
    private ShippingService shippingService;

    private ServiceZone testServiceZone;
    private ShippingQuoteRequest testRequest;
    private ShippingValidationService.ValidationResult validValidationResult;

    @BeforeEach
    void setUp() {
        testServiceZone = ServiceZone.builder()
                .id(1L)
                .name("Delhi NCR")
                .zoneType(ServiceZone.ZoneType.RANGE)
                .pinFrom("110001")
                .pinTo("110099")
                .baseRate(new BigDecimal("50"))
                .minCharge(new BigDecimal("30"))
                .codAllowed(true)
                .codCharges(new BigDecimal("25"))
                .slaDays(2)
                .expressDeliveryAvailable(true)
                .tenantId(1L)
                .build();

        testRequest = ShippingQuoteRequest.builder()
                .destPincode("110001")
                .weight(new BigDecimal("2.5"))
                .orderValue(new BigDecimal("1000"))
                .cod(false)
                .expressDelivery(false)
                .fragile(false)
                .build();

        validValidationResult = new ShippingValidationService.ValidationResult();
    }

    @Test
    void testCalculateShippingQuote_ValidRequest_Success() {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals(testServiceZone.getId(), response.getServiceZoneId());
        assertEquals(testServiceZone.getName(), response.getServiceZoneName());
        assertNotNull(response.getShippingCost());
        assertEquals(BigDecimal.ZERO, response.getCodCharges());
        assertEquals(2, response.getSlaDays());
        assertNotNull(response.getEstimatedDeliveryDate());
        assertTrue(response.isCodAllowed());
    }

    @Test
    void testCalculateShippingQuote_WithCOD_Success() {
        // Given
        testRequest = testRequest.toBuilder().cod(true).build();
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals(testServiceZone.getCodCharges(), response.getCodCharges());
        assertTrue(response.getTotalCost().compareTo(response.getShippingCost()) > 0);
    }

    @Test
    void testCalculateShippingQuote_NoServiceZone_NotAvailable() {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(Collections.emptyList());

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertFalse(response.isAvailable());
        assertEquals("Delivery not available to this location", response.getMessage());
    }

    @Test
    void testCalculateShippingQuote_CODNotAllowed_NotAvailable() {
        // Given
        testRequest = testRequest.toBuilder().cod(true).build();
        testServiceZone.setCodAllowed(false);
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertFalse(response.isAvailable());
        assertEquals("Cash on Delivery not available for this location", response.getMessage());
    }

    @Test
    void testCalculateShippingQuote_InvalidRequest_ThrowsException() {
        // Given
        ShippingValidationService.ValidationResult invalidResult = new ShippingValidationService.ValidationResult();
        invalidResult.addError("weight", "Invalid weight");
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(invalidResult);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shippingService.calculateShippingQuote(testRequest, 1L);
        });
        assertTrue(exception.getMessage().contains("Invalid request"));
        assertTrue(exception.getMessage().contains("Invalid weight"));
    }

    @Test
    void testCalculateShippingQuote_InvalidServiceZone_NotAvailable() {
        // Given
        ShippingValidationService.ValidationResult invalidZoneResult = new ShippingValidationService.ValidationResult();
        invalidZoneResult.addError("pricing", "Invalid pricing configuration");
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(invalidZoneResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertFalse(response.isAvailable());
        assertEquals("Service configuration error. Please contact support.", response.getMessage());
    }

    @Test
    void testCalculateExpressShippingQuote_Available() {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateExpressShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertTrue(response.isExpressDeliveryAvailable());
        assertTrue(response.getServiceZoneName().contains("Express"));
        assertEquals(1, response.getSlaDays()); // Half of original 2 days
        
        // Express cost should be 50% higher
        ShippingQuoteResponse standardQuote = shippingService.calculateShippingQuote(testRequest, 1L);
        BigDecimal expectedExpressCost = standardQuote.getShippingCost().multiply(new BigDecimal("1.5"));
        assertEquals(0, expectedExpressCost.compareTo(response.getShippingCost()));
    }

    @Test
    void testCalculateExpressShippingQuote_NotAvailable() {
        // Given
        testServiceZone.setExpressDeliveryAvailable(false);
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateExpressShippingQuote(testRequest, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertFalse(response.isExpressDeliveryAvailable());
        assertFalse(response.getServiceZoneName().contains("Express"));
    }

    @Test
    void testGetServiceZonesByTenant() {
        // Given
        List<ServiceZone> expectedZones = List.of(testServiceZone);
        when(serviceZoneRepository.findByTenantIdAndDeletedAtIsNullOrderByName(1L))
                .thenReturn(expectedZones);

        // When
        List<ServiceZone> result = shippingService.getServiceZonesByTenant(1L);

        // Then
        assertEquals(expectedZones, result);
        verify(serviceZoneRepository).findByTenantIdAndDeletedAtIsNullOrderByName(1L);
    }

    @Test
    void testIsDeliveryAvailable_Available() {
        // Given
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        boolean result = shippingService.isDeliveryAvailable("110001", 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsDeliveryAvailable_NotAvailable() {
        // Given
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "999999"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "999999"))
                .thenReturn(Collections.emptyList());

        // When
        boolean result = shippingService.isDeliveryAvailable("999999", 1L);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetServiceZoneById_Found() {
        // Given
        when(serviceZoneRepository.findByIdAndTenantIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.of(testServiceZone));

        // When
        Optional<ServiceZone> result = shippingService.getServiceZoneById(1L, 1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testServiceZone, result.get());
    }

    @Test
    void testGetServiceZoneById_NotFound() {
        // Given
        when(serviceZoneRepository.findByIdAndTenantIdAndDeletedAtIsNull(999L, 1L))
                .thenReturn(Optional.empty());

        // When
        Optional<ServiceZone> result = shippingService.getServiceZoneById(999L, 1L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindApplicableServiceZone_ExplicitPincode() {
        // Given
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        // Should not call range query since explicit match found
        verify(serviceZoneRepository).findByTenantIdAndExplicitPincodesContaining(1L, "110001");
        verify(serviceZoneRepository, never()).findByTenantIdAndPincodeRange(anyLong(), anyString());
    }

    @Test
    void testCalculateShippingCost_MinimumCharge() {
        // Given
        testServiceZone.setBaseRate(new BigDecimal("10")); // Very low base rate
        testServiceZone.setMinCharge(new BigDecimal("50")); // Higher minimum
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(validationService.validateServiceZone(any())).thenReturn(validValidationResult);
        when(serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(1L, "110001"))
                .thenReturn(Collections.emptyList());
        when(serviceZoneRepository.findByTenantIdAndPincodeRange(1L, "110001"))
                .thenReturn(List.of(testServiceZone));

        // When
        ShippingQuoteResponse response = shippingService.calculateShippingQuote(testRequest, 1L);

        // Then
        assertTrue(response.isAvailable());
        // Should use minimum charge since calculated cost is lower
        assertEquals(0, testServiceZone.getMinCharge().compareTo(response.getShippingCost()));
    }
}
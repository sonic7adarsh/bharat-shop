package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.ShippingQuoteRequest;
import com.bharatshop.storefront.dto.ShippingQuoteResponse;
import com.bharatshop.storefront.service.ShippingService;
import com.bharatshop.storefront.shared.ApiResponse;
import com.bharatshop.shared.service.ShippingValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for StorefrontShippingController
 * Tests REST API endpoints for shipping functionality
 */
@WebMvcTest(StorefrontShippingController.class)
class StorefrontShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShippingService shippingService;

    @MockBean
    private ShippingValidationService validationService;

    private ShippingQuoteRequest validRequest;
    private ShippingQuoteResponse successResponse;
    private ShippingValidationService.ValidationResult validValidationResult;

    @BeforeEach
    void setUp() {
        validRequest = ShippingQuoteRequest.builder()
                .destPincode("110001")
                .weight(new BigDecimal("2.5"))
                .orderValue(new BigDecimal("1000"))
                .cod(false)
                .expressDelivery(false)
                .fragile(false)
                .build();

        successResponse = ShippingQuoteResponse.builder()
                .available(true)
                .serviceZoneId(1L)
                .serviceZoneName("Delhi NCR")
                .shippingCost(new BigDecimal("75.00"))
                .codCharges(BigDecimal.ZERO)
                .totalCost(new BigDecimal("75.00"))
                .slaDays(2)
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(2))
                .promisedDeliveryDate(LocalDateTime.now().plusDays(2))
                .codAllowed(true)
                .expressDeliveryAvailable(true)
                .message("Shipping quote calculated successfully")
                .build();

        validValidationResult = new ShippingValidationService.ValidationResult();
    }

    @Test
    void testGetShippingQuote_ValidRequest_Success() throws Exception {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.serviceZoneId").value(1))
                .andExpect(jsonPath("$.data.serviceZoneName").value("Delhi NCR"))
                .andExpect(jsonPath("$.data.shippingCost").value(75.00))
                .andExpect(jsonPath("$.data.codCharges").value(0))
                .andExpect(jsonPath("$.data.totalCost").value(75.00))
                .andExpect(jsonPath("$.data.slaDays").value(2))
                .andExpect(jsonPath("$.data.codAllowed").value(true))
                .andExpect(jsonPath("$.data.expressDeliveryAvailable").value(true))
                .andExpect(jsonPath("$.message").value("Shipping quote calculated successfully"));

        verify(validationService).validateShippingQuoteRequest(any());
        verify(shippingService).calculateShippingQuote(any(), eq(1L));
    }

    @Test
    void testGetShippingQuote_WithCOD_Success() throws Exception {
        // Given
        ShippingQuoteRequest codRequest = validRequest.toBuilder().cod(true).build();
        ShippingQuoteResponse codResponse = successResponse.toBuilder()
                .codCharges(new BigDecimal("25.00"))
                .totalCost(new BigDecimal("100.00"))
                .build();
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(codResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(codRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codCharges").value(25.00))
                .andExpect(jsonPath("$.data.totalCost").value(100.00));
    }

    @Test
    void testGetShippingQuote_ValidationFailure_BadRequest() throws Exception {
        // Given
        ShippingValidationService.ValidationResult invalidResult = new ShippingValidationService.ValidationResult();
        invalidResult.addError("destPincode", "Invalid pincode format");
        invalidResult.addError("weight", "Weight must be positive");
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(invalidResult);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.message").value(containsString("Invalid pincode format")))
                .andExpect(jsonPath("$.message").value(containsString("Weight must be positive")));

        verify(validationService).validateShippingQuoteRequest(any());
        verify(shippingService, never()).calculateShippingQuote(any(), anyLong());
    }

    @Test
    void testGetShippingQuote_InvalidRequestBody_BadRequest() throws Exception {
        // Given - Invalid JSON request with missing required fields
        String invalidJson = "{\"destPincode\":\"\",\"weight\":-1}";

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetShippingQuote_MissingTenantHeader_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetShippingQuote_ServiceNotAvailable_Success() throws Exception {
        // Given
        ShippingQuoteResponse notAvailableResponse = ShippingQuoteResponse.builder()
                .available(false)
                .message("Delivery not available to this location")
                .build();
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(notAvailableResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("Delivery not available to this location"));
    }

    @Test
    void testGetShippingQuote_ServiceException_InternalServerError() throws Exception {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unable to calculate shipping quote. Please try again later."));
    }

    @Test
    void testGetShippingQuote_IllegalArgumentException_BadRequest() throws Exception {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong()))
                .thenThrow(new IllegalArgumentException("Invalid service zone configuration"));

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid request: Invalid service zone configuration"));
    }

    @Test
    void testGetShippingQuote_ExpressDelivery_Success() throws Exception {
        // Given
        ShippingQuoteRequest expressRequest = validRequest.toBuilder().expressDelivery(true).build();
        ShippingQuoteResponse expressResponse = successResponse.toBuilder()
                .serviceZoneName("Delhi NCR (Express)")
                .shippingCost(new BigDecimal("112.50")) // 50% higher
                .totalCost(new BigDecimal("112.50"))
                .slaDays(1) // Half the time
                .build();
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(expressResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(expressRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceZoneName").value(containsString("Express")))
                .andExpect(jsonPath("$.data.shippingCost").value(112.50))
                .andExpect(jsonPath("$.data.slaDays").value(1));
    }

    @Test
    void testGetShippingQuote_FragilePackage_Success() throws Exception {
        // Given
        ShippingQuoteRequest fragileRequest = validRequest.toBuilder().fragile(true).build();
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(fragileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetShippingQuote_LargeWeight_Success() throws Exception {
        // Given
        ShippingQuoteRequest heavyRequest = validRequest.toBuilder()
                .weight(new BigDecimal("25.0")) // Heavy package
                .build();
        
        ShippingQuoteResponse heavyResponse = successResponse.toBuilder()
                .shippingCost(new BigDecimal("250.00")) // Higher cost for heavy package
                .totalCost(new BigDecimal("250.00"))
                .build();
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(heavyResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(heavyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shippingCost").value(250.00));
    }

    @Test
    void testGetShippingQuote_HighValueOrder_Success() throws Exception {
        // Given
        ShippingQuoteRequest highValueRequest = validRequest.toBuilder()
                .orderValue(new BigDecimal("50000")) // High value order
                .build();
        
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), anyLong())).thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "1")
                        .content(objectMapper.writeValueAsString(highValueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetShippingQuote_DifferentTenant_Success() throws Exception {
        // Given
        when(validationService.validateShippingQuoteRequest(any())).thenReturn(validValidationResult);
        when(shippingService.calculateShippingQuote(any(), eq(2L))).thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/store/shipping/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "2")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(shippingService).calculateShippingQuote(any(), eq(2L));
    }
}
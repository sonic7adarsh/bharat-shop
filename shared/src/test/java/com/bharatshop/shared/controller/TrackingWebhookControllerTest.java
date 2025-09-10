package com.bharatshop.shared.controller;

import com.bharatshop.shared.service.ShipmentTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TrackingWebhookController
 * Tests webhook endpoints for different carriers
 */
@WebMvcTest(TrackingWebhookController.class)
class TrackingWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentTrackingService trackingService;

    private String delhiveryPayload;
    private String blueDartPayload;
    private String dtdcPayload;
    private String genericPayload;

    @BeforeEach
    void setUp() {
        delhiveryPayload = "{\"tracking_number\":\"DEL123456789\",\"status\":\"IN_TRANSIT\",\"location\":\"Delhi Hub\",\"timestamp\":\"2024-01-15T10:30:00Z\",\"description\":\"Package is in transit\"}";
        
        blueDartPayload = "{\"awb_number\":\"BD987654321\",\"status_code\":\"IT\",\"status_description\":\"In Transit\",\"location\":\"Mumbai Hub\",\"event_date\":\"2024-01-15T14:20:00Z\"}";
        
        dtdcPayload = "{\"consignment_no\":\"DTDC555666777\",\"status\":\"Dispatched\",\"location\":\"Bangalore\",\"event_time\":\"2024-01-15T16:45:00Z\",\"remarks\":\"Package dispatched from origin\"}";
        
        genericPayload = "{\"tracking_number\":\"GEN111222333\",\"status\":\"DELIVERED\",\"location\":\"Customer Address\",\"timestamp\":\"2024-01-15T18:00:00Z\",\"message\":\"Package delivered successfully\"}";
    }

    @Test
    void testDelhiveryWebhook_ValidPayload_Success() throws Exception {
        // Given
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(delhiveryPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testDelhiveryWebhook_WithSignature_Success() throws Exception {
        // Given
        String signature = "sha256=test-signature";
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Delhivery-Signature", signature)
                        .content(delhiveryPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testDelhiveryWebhook_ServiceException_InternalServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(trackingService).processTrackingEvent(any(), any());

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(delhiveryPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDelhiveryWebhook_InvalidPayload_BadRequest() throws Exception {
        // Given
        String invalidPayload = "invalid json";

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBlueDartWebhook_ValidPayload_Success() throws Exception {
        // Given
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/bluedart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blueDartPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("BlueDart tracking webhook processed successfully"));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testBlueDartWebhook_WithApiKey_Success() throws Exception {
        // Given
        String apiKey = "test-api-key";
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/bluedart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", apiKey)
                        .content(blueDartPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testBlueDartWebhook_ServiceException_InternalServerError() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Invalid tracking number"))
                .when(trackingService).processTrackingEvent(any(), any());

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/bluedart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blueDartPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to process BlueDart webhook: Invalid tracking number"));
    }

    @Test
    void testDtdcWebhook_ValidPayload_Success() throws Exception {
        // Given
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/dtdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtdcPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("DTDC tracking webhook processed successfully"));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testDtdcWebhook_WithToken_Success() throws Exception {
        // Given
        String token = "Bearer test-token";
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/dtdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(dtdcPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testDtdcWebhook_ServiceException_InternalServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Service unavailable"))
                .when(trackingService).processTrackingEvent(any(), any());

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/dtdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtdcPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to process DTDC webhook: Service unavailable"));
    }

    @Test
    void testGenericWebhook_ValidPayload_Success() throws Exception {
        // Given
        String carrierCode = "FEDEX";
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("carrier", carrierCode)
                        .content(genericPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Generic tracking webhook processed successfully"));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testGenericWebhook_MissingCarrierParam_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(genericPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Carrier parameter is required"));

        verify(trackingService, never()).processTrackingEvent(any(), any());
    }

    @Test
    void testGenericWebhook_EmptyCarrierParam_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("carrier", "")
                        .content(genericPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Carrier parameter is required"));

        verify(trackingService, never()).processTrackingEvent(any(), any());
    }

    @Test
    void testGenericWebhook_UnsupportedCarrier_BadRequest() throws Exception {
        // Given
        String unsupportedCarrier = "UNKNOWN_CARRIER";
        doThrow(new IllegalArgumentException("Unsupported carrier: UNKNOWN_CARRIER"))
                .when(trackingService).processTrackingEvent(any(), any());

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("carrier", unsupportedCarrier)
                        .content(genericPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unsupported carrier: UNKNOWN_CARRIER"));
    }

    @Test
    void testGenericWebhook_ServiceException_InternalServerError() throws Exception {
        // Given
        String carrierCode = "UPS";
        doThrow(new RuntimeException("External API timeout"))
                .when(trackingService).processTrackingEvent(any(), any());

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("carrier", carrierCode)
                        .content(genericPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to process generic webhook: External API timeout"));
    }

    @Test
    void testHealthCheck_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/webhooks/tracking/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tracking webhook service is healthy"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testDelhiveryWebhook_EmptyPayload_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBlueDartWebhook_EmptyPayload_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/bluedart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDtdcWebhook_EmptyPayload_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/dtdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenericWebhook_EmptyPayload_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("carrier", "FEDEX")
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDelhiveryWebhook_WrongContentType_UnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(delhiveryPayload))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testBlueDartWebhook_WrongContentType_UnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/bluedart")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(blueDartPayload))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testDtdcWebhook_WrongContentType_UnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/dtdc")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(dtdcPayload))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testGenericWebhook_WrongContentType_UnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/tracking/generic")
                        .contentType(MediaType.TEXT_PLAIN)
                        .param("carrier", "FEDEX")
                        .content(genericPayload))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testDelhiveryWebhook_LargePayload_Success() throws Exception {
        // Given
        StringBuilder largePayload = new StringBuilder("{");
        largePayload.append("\"tracking_number\":\"DEL123456789\",");
        largePayload.append("\"status\":\"IN_TRANSIT\",");
        largePayload.append("\"location\":\"Delhi Hub\",");
        largePayload.append("\"timestamp\":\"2024-01-15T10:30:00Z\",");
        largePayload.append("\"description\":\"Package is in transit\",");
        largePayload.append("\"additional_data\":\"");
        // Add large string to test payload size handling
        for (int i = 0; i < 1000; i++) {
            largePayload.append("test data ");
        }
        largePayload.append("\"}");
        
        when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/webhooks/tracking/delhivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largePayload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(trackingService).processTrackingEvent(any(), any());
    }

    @Test
    void testGenericWebhook_MultipleCarriers_Success() throws Exception {
        // Test multiple carriers through generic endpoint
        String[] carriers = {"FEDEX", "UPS", "DHL", "ARAMEX", "ECOM"};
        
        for (String carrier : carriers) {
            when(trackingService.processTrackingEvent(any(), any())).thenReturn(true);
            
            mockMvc.perform(post("/webhooks/tracking/generic")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("carrier", carrier)
                            .content(genericPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            
            verify(trackingService).processTrackingEvent(any(), any());
        }
    }
}
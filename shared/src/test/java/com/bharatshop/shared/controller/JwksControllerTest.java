package com.bharatshop.shared.controller;

import com.bharatshop.shared.entity.JwksKey;
import com.bharatshop.shared.service.JwtKeyRotationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JwksController.class)
@DisplayName("JWKS Controller Tests")
class JwksControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private JwtKeyRotationService jwtKeyRotationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private JwksKey activeKey;
    private JwksKey rotatedKey;
    
    @BeforeEach
    void setUp() {
        activeKey = createTestKey("key-active-123", true, null, null);
        rotatedKey = createTestKey("key-rotated-456", false, 
            LocalDateTime.now().minusHours(12), LocalDateTime.now().plusHours(12));
    }
    
    @Test
    @DisplayName("Should return JWKS with active and rotated keys")
    void shouldReturnJwksWithActiveAndRotatedKeys() throws Exception {
        // Given
        List<JwksKey> jwksKeys = List.of(activeKey, rotatedKey);
        when(jwtKeyRotationService.getJwksKeys()).thenReturn(jwksKeys);
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("Cache-Control", "public, max-age=3600"))
            .andExpect(jsonPath("$.keys", hasSize(2)))
            .andExpect(jsonPath("$.keys[0].kid", is("key-active-123")))
            .andExpect(jsonPath("$.keys[0].kty", is("oct")))
            .andExpect(jsonPath("$.keys[0].alg", is("HS256")))
            .andExpect(jsonPath("$.keys[0].use", is("sig")))
            .andExpect(jsonPath("$.keys[0].k", is("dGVzdC1rZXktbWF0ZXJpYWwtZm9yLWp3dC1zaWduaW5nLTEyMzQ1Njc4OTA=")))
            .andExpect(jsonPath("$.keys[1].kid", is("key-rotated-456")))
            .andExpect(jsonPath("$.keys[1].kty", is("oct")))
            .andExpect(jsonPath("$.keys[1].alg", is("HS256")))
            .andExpect(jsonPath("$.keys[1].use", is("sig")));
    }
    
    @Test
    @DisplayName("Should return empty JWKS when no keys available")
    void shouldReturnEmptyJwksWhenNoKeysAvailable() throws Exception {
        // Given
        when(jwtKeyRotationService.getJwksKeys()).thenReturn(List.of());
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("Cache-Control", "public, max-age=3600"))
            .andExpect(jsonPath("$.keys", hasSize(0)));
    }
    
    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceExceptionGracefully() throws Exception {
        // Given
        when(jwtKeyRotationService.getJwksKeys()).thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", is("Failed to retrieve JWKS")));
    }
    
    @Test
    @DisplayName("Should return JWKS health check")
    void shouldReturnJwksHealthCheck() throws Exception {
        // Given
        JwtKeyRotationService.KeyRotationStats stats = JwtKeyRotationService.KeyRotationStats.builder()
            .activeSigningKeys(1L)
            .keysInRollingUpgradeWindow(1)
            .expiredKeys(0)
            .rollingUpgradeWindowHours(24)
            .keyRotationEnabled(true)
            .lastRotationTime(LocalDateTime.now().minusHours(6))
            .build();
        
        when(jwtKeyRotationService.getKeyRotationStats()).thenReturn(stats);
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks/health")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is("healthy")))
            .andExpect(jsonPath("$.activeSigningKeys", is(1)))
            .andExpect(jsonPath("$.keysInRollingUpgradeWindow", is(1)))
            .andExpect(jsonPath("$.expiredKeys", is(0)))
            .andExpect(jsonPath("$.rollingUpgradeWindowHours", is(24)))
            .andExpect(jsonPath("$.keyRotationEnabled", is(true)))
            .andExpect(jsonPath("$.lastRotationTime", notNullValue()));
    }
    
    @Test
    @DisplayName("Should return unhealthy status when no active keys")
    void shouldReturnUnhealthyStatusWhenNoActiveKeys() throws Exception {
        // Given
        JwtKeyRotationService.KeyRotationStats stats = JwtKeyRotationService.KeyRotationStats.builder()
            .activeSigningKeys(0L)
            .keysInRollingUpgradeWindow(0)
            .expiredKeys(5)
            .rollingUpgradeWindowHours(24)
            .keyRotationEnabled(true)
            .lastRotationTime(LocalDateTime.now().minusHours(48))
            .build();
        
        when(jwtKeyRotationService.getKeyRotationStats()).thenReturn(stats);
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks/health")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status", is("unhealthy")))
            .andExpect(jsonPath("$.activeSigningKeys", is(0)))
            .andExpect(jsonPath("$.keysInRollingUpgradeWindow", is(0)))
            .andExpect(jsonPath("$.expiredKeys", is(5)));
    }
    
    @Test
    @DisplayName("Should handle health check service exception")
    void shouldHandleHealthCheckServiceException() throws Exception {
        // Given
        when(jwtKeyRotationService.getKeyRotationStats()).thenThrow(new RuntimeException("Service unavailable"));
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks/health")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", is("Failed to retrieve JWKS health status")));
    }
    
    @Test
    @DisplayName("Should set appropriate cache headers for JWKS endpoint")
    void shouldSetAppropriateCacheHeadersForJwksEndpoint() throws Exception {
        // Given
        when(jwtKeyRotationService.getJwksKeys()).thenReturn(List.of(activeKey));
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "public, max-age=3600"))
            .andExpect(header().exists("ETag"))
            .andExpect(header().string("Vary", "Accept-Encoding"));
    }
    
    @Test
    @DisplayName("Should support conditional requests with ETag")
    void shouldSupportConditionalRequestsWithETag() throws Exception {
        // Given
        when(jwtKeyRotationService.getJwksKeys()).thenReturn(List.of(activeKey));
        
        // First request to get ETag
        String etag = mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("ETag");
        
        // Second request with If-None-Match header
        mockMvc.perform(get("/.well-known/jwks.json")
                .header("If-None-Match", etag)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotModified());
    }
    
    @Test
    @DisplayName("Should handle CORS preflight requests")
    void shouldHandleCorsPreflight() throws Exception {
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .header("Origin", "https://example.com")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "*"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET"))
            .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }
    
    @Test
    @DisplayName("Should convert HMAC key to JWK format correctly")
    void shouldConvertHmacKeyToJwkFormatCorrectly() throws Exception {
        // Given
        JwksKey hmacKey = JwksKey.builder()
            .kid("hmac-key-123")
            .alg("HS256")
            .keyMaterial("dGVzdC1rZXktbWF0ZXJpYWwtZm9yLWp3dC1zaWduaW5nLTEyMzQ1Njc4OTA=")
            .active(true)
            .usage(JwksKey.KeyUsage.SIGNING)
            .keySize(256)
            .description("HMAC test key")
            .build();
        
        when(jwtKeyRotationService.getJwksKeys()).thenReturn(List.of(hmacKey));
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kty", is("oct")))
            .andExpect(jsonPath("$.keys[0].alg", is("HS256")))
            .andExpect(jsonPath("$.keys[0].use", is("sig")))
            .andExpect(jsonPath("$.keys[0].k", is("dGVzdC1rZXktbWF0ZXJpYWwtZm9yLWp3dC1zaWduaW5nLTEyMzQ1Njc4OTA=")))
            .andExpect(jsonPath("$.keys[0].kid", is("hmac-key-123")));
    }
    
    @Test
    @DisplayName("Should filter out invalid keys from JWKS response")
    void shouldFilterOutInvalidKeysFromJwksResponse() throws Exception {
        // Given
        JwksKey validKey = createTestKey("valid-key-123", true, null, null);
        JwksKey invalidKey = JwksKey.builder()
            .kid("invalid-key-456")
            .alg("HS256")
            .keyMaterial(null) // Invalid - no key material
            .active(true)
            .usage(JwksKey.KeyUsage.SIGNING)
            .keySize(256)
            .build();
        
        when(jwtKeyRotationService.getJwksKeys()).thenReturn(List.of(validKey, invalidKey));
        
        // When & Then
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys", hasSize(1)))
            .andExpect(jsonPath("$.keys[0].kid", is("valid-key-123")));
    }
    
    private JwksKey createTestKey(String kid, boolean active, LocalDateTime rotatedAt, LocalDateTime expiresAt) {
        return JwksKey.builder()
            .kid(kid)
            .alg("HS256")
            .keyMaterial("dGVzdC1rZXktbWF0ZXJpYWwtZm9yLWp3dC1zaWduaW5nLTEyMzQ1Njc4OTA=") // Base64 test key
            .active(active)
            .usage(JwksKey.KeyUsage.SIGNING)
            .keySize(256)
            .rotatedAt(rotatedAt)
            .expiresAt(expiresAt)
            .description("Test key for controller tests")
            .build();
    }
}
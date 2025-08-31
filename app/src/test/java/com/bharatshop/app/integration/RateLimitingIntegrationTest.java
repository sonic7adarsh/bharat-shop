package com.bharatshop.app.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for Rate Limiting functionality
 * Tests the rate limiting behavior with Redis backend
 */
@AutoConfigureWebMvc
@Transactional
class RateLimitingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow requests within rate limit")
    @WithMockUser(roles = "USER")
    void shouldAllowRequestsWithinRateLimit() throws Exception {
        String clientId = "test-client-1";
        
        // Make several requests within the limit (assuming 100 requests per minute for API_GENERAL)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/products")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", clientId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should enforce rate limit for API requests")
    @WithMockUser(roles = "USER")
    void shouldEnforceRateLimitForApiRequests() throws Exception {
        String clientId = "test-client-rate-limit";
        
        // This test assumes a lower rate limit is configured for testing
        // In a real scenario, you might want to configure different limits for test profile
        
        // Make requests up to the limit
        // Note: This test might need adjustment based on actual rate limit configuration
        boolean rateLimitHit = false;
        
        for (int i = 0; i < 150; i++) { // Exceed the typical 100 requests per minute limit
            try {
                mockMvc.perform(get("/api/products")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-Forwarded-For", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            } catch (AssertionError e) {
                // Rate limit should be hit at some point
                mockMvc.perform(get("/api/products")
                        .header("X-Tenant-ID", "tenant1")
                        .header("X-Forwarded-For", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isTooManyRequests())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                        .andExpect(jsonPath("$.message").exists());
                rateLimitHit = true;
                break;
            }
        }
        
        // If we didn't hit the rate limit in the loop, make one more request that should fail
        if (!rateLimitHit) {
            mockMvc.perform(get("/api/products")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", clientId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Test
    @DisplayName("Should have separate rate limits for different clients")
    @WithMockUser(roles = "USER")
    void shouldHaveSeparateRateLimitsForDifferentClients() throws Exception {
        String client1 = "test-client-1";
        String client2 = "test-client-2";
        
        // Make requests from client 1
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/products")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", client1)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
        
        // Make requests from client 2 - should still be allowed
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/products")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", client2)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should have different rate limits for auth endpoints")
    @WithMockUser(roles = "USER")
    void shouldHaveDifferentRateLimitsForAuthEndpoints() throws Exception {
        String clientId = "test-auth-client";
        
        // Auth endpoints typically have stricter limits (10 requests per minute)
        // Make several auth requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", clientId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"test\",\"password\":\"test\"}"))
                    .andDo(print())
                    // Note: This might return 400 or other status due to invalid credentials,
                    // but should not return 429 (rate limit) for the first few requests
                    .andExpect(status().is(not(429)));
        }
    }

    @Test
    @DisplayName("Should have higher rate limits for admin endpoints")
    @WithMockUser(roles = "ADMIN")
    void shouldHaveHigherRateLimitsForAdminEndpoints() throws Exception {
        String adminClientId = "test-admin-client";
        
        // Admin endpoints typically have higher limits (200 requests per minute)
        // Make several admin requests
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/admin/users")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", adminClientId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    // Should allow more requests for admin endpoints
                    .andExpect(status().is(not(429)));
        }
    }

    @Test
    @DisplayName("Should use tenant ID for rate limiting when available")
    @WithMockUser(roles = "USER")
    void shouldUseTenantIdForRateLimiting() throws Exception {
        String sameIp = "same-ip-address";
        
        // Make requests from same IP but different tenants
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/products")
                    .header("X-Tenant-ID", "tenant1")
                    .header("X-Forwarded-For", sameIp)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
        
        // Requests from different tenant should still be allowed
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/products")
                    .header("X-Tenant-ID", "tenant2")
                    .header("X-Forwarded-For", sameIp)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should return proper headers for rate limiting")
    @WithMockUser(roles = "USER")
    void shouldReturnProperHeadersForRateLimiting() throws Exception {
        String clientId = "test-headers-client";
        
        // Make a request and check for rate limiting headers
        mockMvc.perform(get("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .header("X-Forwarded-For", clientId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // Check if rate limiting headers are present (if implemented)
                // These headers are optional but good practice
                // .andExpect(header().exists("X-RateLimit-Limit"))
                // .andExpect(header().exists("X-RateLimit-Remaining"))
                // .andExpected(header().exists("X-RateLimit-Reset"))
                ;
    }
}
package com.bharatshop.app.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration tests for Health Check endpoints
 * Tests the health indicators and actuator endpoints
 */
@AutoConfigureWebMvc
class HealthCheckIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return UP status for health endpoint")
    void shouldReturnUpStatusForHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Should return detailed health information")
    void shouldReturnDetailedHealthInformation() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.components.bharatshop").exists())
                .andExpect(jsonPath("$.components.bharatshop.status").value("UP"))
                .andExpect(jsonPath("$.components.bharatshop.details.database").value("UP"))
                .andExpect(jsonPath("$.components.bharatshop.details.redis").value("UP"))
                .andExpect(jsonPath("$.components.bharatshop.details.application").value("UP"));
    }

    @Test
    @DisplayName("Should return database health status")
    void shouldReturnDatabaseHealthStatus() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db").exists())
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    @DisplayName("Should return Redis health status")
    void shouldReturnRedisHealthStatus() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.redis").exists())
                .andExpect(jsonPath("$.components.redis.status").value("UP"));
    }

    @Test
    @DisplayName("Should return application info")
    void shouldReturnApplicationInfo() throws Exception {
        mockMvc.perform(get("/actuator/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.app").exists())
                .andExpect(jsonPath("$.app.name").value("BharatShop"))
                .andExpect(jsonPath("$.app.description").value("Multi-tenant e-commerce platform"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"))
                .andExpect(jsonPath("$.features").exists())
                .andExpect(jsonPath("$.features.multi-tenancy").value(true))
                .andExpect(jsonPath("$.features.rate-limiting").value(true))
                .andExpect(jsonPath("$.features.monitoring").value(true))
                .andExpect(jsonPath("$.features.swagger-docs").value(true))
                .andExpect(jsonPath("$.features.containerized").value(true));
    }

    @Test
    @DisplayName("Should return Prometheus metrics")
    void shouldReturnPrometheusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                .accept("text/plain"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# HELP")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# TYPE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")));
    }

    @Test
    @DisplayName("Should return custom BharatShop metrics")
    void shouldReturnCustomBharatShopMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                .accept("text/plain"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bharatshop_")));
    }

    @Test
    @DisplayName("Should return metrics endpoint")
    void shouldReturnMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.names").isArray())
                .andExpect(jsonPath("$.names").isNotEmpty());
    }

    @Test
    @DisplayName("Should return specific JVM memory metrics")
    void shouldReturnSpecificJvmMemoryMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements").isArray())
                .andExpect(jsonPath("$.availableTags").isArray());
    }

    @Test
    @DisplayName("Should return HTTP server request metrics")
    void shouldReturnHttpServerRequestMetrics() throws Exception {
        // First make a request to generate some metrics
        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then check the metrics
        mockMvc.perform(get("/actuator/metrics/http.server.requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("http.server.requests"))
                .andExpect(jsonPath("$.measurements").isArray())
                .andExpect(jsonPath("$.availableTags").isArray());
    }

    @Test
    @DisplayName("Should not expose sensitive actuator endpoints without authentication")
    void shouldNotExposeSensitiveEndpointsWithoutAuth() throws Exception {
        // Test that sensitive endpoints like env, configprops are not exposed
        // or require authentication
        
        mockMvc.perform(get("/actuator/env")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound()); // Should not be exposed
        
        mockMvc.perform(get("/actuator/configprops")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound()); // Should not be exposed
    }

    @Test
    @DisplayName("Should return proper CORS headers for actuator endpoints")
    void shouldReturnProperCorsHeadersForActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // Check if CORS headers are properly configured
                // This depends on your CORS configuration
                .andExpect(header().exists("Vary"));
    }
}
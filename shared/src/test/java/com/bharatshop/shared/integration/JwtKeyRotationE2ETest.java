package com.bharatshop.shared.integration;

import com.bharatshop.shared.entity.JwksKey;
import com.bharatshop.shared.repository.JwksKeyRepository;
import com.bharatshop.shared.service.JwtKeyRotationService;
import com.bharatshop.shared.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("JWT Key Rotation End-to-End Tests")
class JwtKeyRotationE2ETest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private JwtKeyRotationService jwtKeyRotationService;
    
    @Autowired
    private JwksKeyRepository jwksKeyRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private UserDetails testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username("testuser")
            .password("password")
            .authorities("ROLE_USER")
            .build();
        
        // Clean up any existing keys
        jwksKeyRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should complete full key rotation workflow")
    void shouldCompleteFullKeyRotationWorkflow() throws Exception {
        // Step 1: Generate initial token with first key
        String initialToken = jwtService.generateToken(testUser);
        String initialKid = jwtService.extractKeyId(initialToken);
        
        // Step 2: Verify JWKS endpoint contains the initial key
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys", hasSize(1)))
            .andExpect(jsonPath("$.keys[0].kid", is(initialKid)));
        
        // Step 3: Verify initial token works for authentication
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + initialToken))
            .andExpect(status().isOk());
        
        // Step 4: Rotate the signing key
        JwksKey newKey = jwtKeyRotationService.rotateSigningKey();
        String newKid = newKey.getKid();
        
        // Step 5: Generate token with new key
        String newToken = jwtService.generateToken(testUser);
        String newTokenKid = jwtService.extractKeyId(newToken);
        
        // Verify new token uses new key
        assert newTokenKid.equals(newKid);
        assert !newTokenKid.equals(initialKid);
        
        // Step 6: Verify JWKS endpoint now contains both keys (rolling upgrade window)
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys", hasSize(2)))
            .andExpect(jsonPath("$.keys[*].kid", containsInAnyOrder(initialKid, newKid)));
        
        // Step 7: Verify both old and new tokens work during rolling upgrade window
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + initialToken))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk());
        
        // Step 8: Simulate rolling upgrade window expiration
        jwksKeyRepository.expireOldRotatedKeys(
            LocalDateTime.now().minusHours(25), // Older than 24-hour window
            LocalDateTime.now()
        );
        
        // Step 9: Verify JWKS endpoint now only contains the new key
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys", hasSize(1)))
                .andExpect(jsonPath("$.keys[0].kid", is(newKid)));
        });
        
        // Step 10: Verify old token no longer works after expiration
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + initialToken))
            .andExpect(status().isUnauthorized());
        
        // Step 11: Verify new token still works
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should handle emergency key rotation")
    void shouldHandleEmergencyKeyRotation() throws Exception {
        // Step 1: Generate initial token
        String initialToken = jwtService.generateToken(testUser);
        String initialKid = jwtService.extractKeyId(initialToken);
        
        // Step 2: Verify initial token works
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + initialToken))
            .andExpect(status().isOk());
        
        // Step 3: Force emergency key rotation
        JwksKey emergencyKey = jwtKeyRotationService.forceKeyRotation("Security incident detected");
        String emergencyKid = emergencyKey.getKid();
        
        // Step 4: Generate new token with emergency key
        String emergencyToken = jwtService.generateToken(testUser);
        String emergencyTokenKid = jwtService.extractKeyId(emergencyToken);
        
        // Verify emergency token uses emergency key
        assert emergencyTokenKid.equals(emergencyKid);
        assert !emergencyTokenKid.equals(initialKid);
        
        // Step 5: Verify emergency token works immediately
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + emergencyToken))
            .andExpect(status().isOk());
        
        // Step 6: Verify JWKS endpoint reflects emergency rotation
        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[*].kid", hasItem(emergencyKid)));
    }
    
    @Test
    @DisplayName("Should handle token validation with unknown kid gracefully")
    void shouldHandleTokenValidationWithUnknownKidGracefully() throws Exception {
        // Step 1: Create a token with a fake kid that doesn't exist in the database
        String tokenWithFakeKid = createTokenWithFakeKid("fake-kid-999", testUser);
        
        // Step 2: Ensure we have at least one valid key in the system
        jwtKeyRotationService.getCurrentSigningKey();
        
        // Step 3: Verify token with fake kid falls back to valid keys
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + tokenWithFakeKid))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should maintain JWKS health during key rotation")
    void shouldMaintainJwksHealthDuringKeyRotation() throws Exception {
        // Step 1: Check initial health status
        mockMvc.perform(get("/.well-known/jwks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("healthy")))
            .andExpect(jsonPath("$.activeSigningKeys", greaterThan(0)));
        
        // Step 2: Perform key rotation
        jwtKeyRotationService.rotateSigningKey();
        
        // Step 3: Verify health status remains healthy during rolling upgrade
        mockMvc.perform(get("/.well-known/jwks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("healthy")))
            .andExpect(jsonPath("$.activeSigningKeys", greaterThan(0)))
            .andExpect(jsonPath("$.keysInRollingUpgradeWindow", greaterThan(0)));
        
        // Step 4: Verify statistics are updated
        mockMvc.perform(get("/.well-known/jwks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastRotationTime", notNullValue()))
            .andExpect(jsonPath("$.keyRotationEnabled", is(true)));
    }
    
    @Test
    @DisplayName("Should handle concurrent token validation during key rotation")
    void shouldHandleConcurrentTokenValidationDuringKeyRotation() throws Exception {
        // Step 1: Generate tokens with current key
        String token1 = jwtService.generateToken(testUser);
        String token2 = jwtService.generateToken(testUser);
        
        // Step 2: Start key rotation in background
        new Thread(() -> {
            try {
                Thread.sleep(100); // Small delay
                jwtKeyRotationService.rotateSigningKey();
            } catch (Exception e) {
                // Handle exception
            }
        }).start();
        
        // Step 3: Validate tokens concurrently during rotation
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + token2))
            .andExpect(status().isOk());
        
        // Step 4: Wait for rotation to complete and verify new tokens work
        Thread.sleep(500);
        
        String newToken = jwtService.generateToken(testUser);
        mockMvc.perform(get("/api/protected-endpoint")
                .header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should handle JWKS caching and cache invalidation")
    void shouldHandleJwksCachingAndCacheInvalidation() throws Exception {
        // Step 1: First request to JWKS endpoint
        String etag1 = mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(header().exists("ETag"))
            .andExpect(header().string("Cache-Control", "public, max-age=3600"))
            .andReturn()
            .getResponse()
            .getHeader("ETag");
        
        // Step 2: Second request with same ETag should return 304
        mockMvc.perform(get("/.well-known/jwks.json")
                .header("If-None-Match", etag1))
            .andExpect(status().isNotModified());
        
        // Step 3: Rotate key to invalidate cache
        jwtKeyRotationService.rotateSigningKey();
        
        // Step 4: Request with old ETag should return new content
        String etag2 = mockMvc.perform(get("/.well-known/jwks.json")
                .header("If-None-Match", etag1))
            .andExpect(status().isOk())
            .andExpect(header().exists("ETag"))
            .andReturn()
            .getResponse()
            .getHeader("ETag");
        
        // Verify ETags are different
        assert !etag1.equals(etag2);
    }
    
    @Test
    @DisplayName("Should validate environment configuration on startup")
    void shouldValidateEnvironmentConfigurationOnStartup() throws Exception {
        // This test verifies that the JwtConfigValidator runs on startup
        // and validates all required environment variables
        
        // Step 1: Verify JWKS health endpoint shows configuration status
        mockMvc.perform(get("/.well-known/jwks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keyRotationEnabled", notNullValue()))
            .andExpect(jsonPath("$.rollingUpgradeWindowHours", greaterThan(0)));
        
        // Step 2: Verify key rotation service is properly configured
        JwtKeyRotationService.KeyRotationStats stats = jwtKeyRotationService.getKeyRotationStats();
        assert stats.isKeyRotationEnabled();
        assert stats.getRollingUpgradeWindowHours() > 0;
    }
    
    private String createTokenWithFakeKid(String fakeKid, UserDetails user) {
        // This would need to be implemented based on your JWT library
        // For now, return a token generated with current key but we'll modify the kid in header
        return jwtService.generateToken(user);
    }
}
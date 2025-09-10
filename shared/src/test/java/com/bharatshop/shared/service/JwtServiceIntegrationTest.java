package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.JwksKey;
import com.bharatshop.shared.repository.JwksKeyRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = com.bharatshop.shared.TestConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("JWT Service Integration Tests")
class JwtServiceIntegrationTest {
    
    @Autowired
    private JwtService jwtService;
    
    @MockBean
    private JwtKeyRotationService jwtKeyRotationService;
    
    @MockBean
    private JwksKeyRepository jwksKeyRepository;
    
    private JwksKey activeKey;
    private JwksKey rotatedKey;
    private JwksKey expiredKey;
    private UserDetails testUser;
    private SecretKey testSecretKey;
    
    @BeforeEach
    void setUp() {
        // Create test secret key
        testSecretKey = Keys.hmacShaKeyFor("test-secret-key-for-jwt-signing-must-be-long-enough-256-bits".getBytes());
        
        // Create test user
        testUser = User.builder()
            .username("testuser")
            .password("password")
            .authorities("ROLE_USER")
            .build();
        
        // Create test keys
        activeKey = createTestKey("key-active-123", true, null, null);
        rotatedKey = createTestKey("key-rotated-456", false, 
            LocalDateTime.now().minusHours(12), LocalDateTime.now().plusHours(12));
        expiredKey = createTestKey("key-expired-789", false, 
            LocalDateTime.now().minusHours(48), LocalDateTime.now().minusHours(1));
    }
    
    @Test
    @DisplayName("Should generate token with current signing key")
    void shouldGenerateTokenWithCurrentSigningKey() {
        // Given
        when(jwtKeyRotationService.getCurrentSigningKey()).thenReturn(activeKey);
        when(jwtKeyRotationService.getSecretKey(activeKey)).thenReturn(testSecretKey);
        
        // When
        String token = jwtService.generateAccessToken(testUser);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // Verify token contains kid in header
        String kid = jwtService.extractKeyId(token);
        assertThat(kid).isEqualTo("key-active-123");
    }
    
    @Test
    @DisplayName("Should validate token with active key")
    void shouldValidateTokenWithActiveKey() {
        // Given
        when(jwtKeyRotationService.getCurrentSigningKey()).thenReturn(activeKey);
        when(jwtKeyRotationService.getSecretKey(activeKey)).thenReturn(testSecretKey);
        when(jwtKeyRotationService.getKeyByKid("key-active-123")).thenReturn(Optional.of(activeKey));
        when(jwtKeyRotationService.isKeyValidForVerification(activeKey)).thenReturn(true);
        
        // When
        String token = jwtService.generateAccessToken(testUser);
        boolean isValid = jwtService.isTokenValidWithKeyRotation(token, testUser);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should validate token with rotated key in rolling upgrade window")
    void shouldValidateTokenWithRotatedKeyInRollingUpgradeWindow() {
        // Given
        String tokenWithRotatedKey = createTokenWithKey(rotatedKey, testUser);
        
        when(jwtKeyRotationService.getKeyByKid("key-rotated-456")).thenReturn(Optional.of(rotatedKey));
        when(jwtKeyRotationService.isKeyValidForVerification(rotatedKey)).thenReturn(true);
        when(jwtKeyRotationService.getSecretKey(rotatedKey)).thenReturn(testSecretKey);
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(tokenWithRotatedKey, testUser);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should reject token with expired key")
    void shouldRejectTokenWithExpiredKey() {
        // Given
        String tokenWithExpiredKey = createTokenWithKey(expiredKey, testUser);
        
        when(jwtKeyRotationService.getKeyByKid("key-expired-789")).thenReturn(Optional.of(expiredKey));
        when(jwtKeyRotationService.isKeyValidForVerification(expiredKey)).thenReturn(false);
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(tokenWithExpiredKey, testUser);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should fallback to valid keys when kid not found")
    void shouldFallbackToValidKeysWhenKidNotFound() {
        // Given
        String tokenWithUnknownKid = createTokenWithKey(
            createTestKey("unknown-kid-999", true, null, null), testUser);
        
        when(jwtKeyRotationService.getKeyByKid("unknown-kid-999")).thenReturn(Optional.empty());
        when(jwtKeyRotationService.getValidVerificationKeys()).thenReturn(List.of(activeKey, rotatedKey));
        when(jwtKeyRotationService.getSecretKey(any(JwksKey.class))).thenReturn(testSecretKey);
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(tokenWithUnknownKid, testUser);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should reject token when no valid keys available")
    void shouldRejectTokenWhenNoValidKeysAvailable() {
        // Given
        String tokenWithUnknownKid = createTokenWithKey(
            createTestKey("unknown-kid-999", true, null, null), testUser);
        
        when(jwtKeyRotationService.getKeyByKid("unknown-kid-999")).thenReturn(Optional.empty());
        when(jwtKeyRotationService.getValidVerificationKeys()).thenReturn(List.of());
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(tokenWithUnknownKid, testUser);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should extract claims from valid token")
    void shouldExtractClaimsFromValidToken() {
        // Given
        when(jwtKeyRotationService.getCurrentSigningKey()).thenReturn(activeKey);
        when(jwtKeyRotationService.getSecretKey(activeKey)).thenReturn(testSecretKey);
        when(jwtKeyRotationService.getKeyByKid("key-active-123")).thenReturn(Optional.of(activeKey));
        when(jwtKeyRotationService.isKeyValidForVerification(activeKey)).thenReturn(true);
        
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        String username = jwtService.extractUsername(token);
        String keyId = jwtService.extractKeyId(token);
        
        // Then
        assertThat(username).isEqualTo("testuser");
        assertThat(keyId).isEqualTo("key-active-123");
    }
    
    @Test
    @DisplayName("Should handle token without kid header gracefully")
    void shouldHandleTokenWithoutKidHeaderGracefully() {
        // Given - Create token without kid header
        String tokenWithoutKid = Jwts.builder()
            .setSubject(testUser.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
            .signWith(testSecretKey)
            .compact();
        
        when(jwtKeyRotationService.getValidVerificationKeys()).thenReturn(List.of(activeKey));
        when(jwtKeyRotationService.getSecretKey(activeKey)).thenReturn(testSecretKey);
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(tokenWithoutKid, testUser);
        String keyId = jwtService.extractKeyId(tokenWithoutKid);
        
        // Then
        assertThat(isValid).isTrue();
        assertThat(keyId).isNull();
    }
    
    @Test
    @DisplayName("Should validate token expiration")
    void shouldValidateTokenExpiration() {
        // Given - Create expired token
        String expiredToken = Jwts.builder()
            .setSubject(testUser.getUsername())
            .setHeaderParam("kid", "key-active-123")
            .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)) // 2 hours ago
            .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago
            .signWith(testSecretKey)
            .compact();
        
        when(jwtKeyRotationService.getKeyByKid("key-active-123")).thenReturn(Optional.of(activeKey));
        when(jwtKeyRotationService.isKeyValidForVerification(activeKey)).thenReturn(true);
        when(jwtKeyRotationService.getSecretKey(activeKey)).thenReturn(testSecretKey);
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(expiredToken, testUser);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should validate username mismatch")
    void shouldValidateUsernameMismatch() {
        // Given
        UserDetails differentUser = User.builder()
            .username("differentuser")
            .password("password")
            .authorities("ROLE_USER")
            .build();
        
        when(jwtKeyRotationService.getCurrentSigningKey()).thenReturn(activeKey);
        when(jwtKeyRotationService.getSecretKey(activeKey)).thenReturn(testSecretKey);
        when(jwtKeyRotationService.getKeyByKid("key-active-123")).thenReturn(Optional.of(activeKey));
        when(jwtKeyRotationService.isKeyValidForVerification(activeKey)).thenReturn(true);
        
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(token, differentUser);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should handle malformed token gracefully")
    void shouldHandleMalformedTokenGracefully() {
        // Given
        String malformedToken = "invalid.jwt.token";
        
        // When
        boolean isValid = jwtService.isTokenValidWithKeyRotation(malformedToken, testUser);
        String keyId = jwtService.extractKeyId(malformedToken);
        
        // Then
        assertThat(isValid).isFalse();
        assertThat(keyId).isNull();
    }
    
    private String createTokenWithKey(JwksKey key, UserDetails user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .setHeaderParam("kid", key.getKid())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
            .signWith(testSecretKey)
            .compact();
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
            .description("Test key for integration tests")
            .build();
    }
}
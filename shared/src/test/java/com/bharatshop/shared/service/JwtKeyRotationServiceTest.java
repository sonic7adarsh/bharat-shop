package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.JwksKey;
import com.bharatshop.shared.repository.JwksKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Key Rotation Service Tests")
class JwtKeyRotationServiceTest {
    
    @Mock
    private JwksKeyRepository jwksKeyRepository;
    
    @InjectMocks
    private JwtKeyRotationService jwtKeyRotationService;
    
    private JwksKey activeKey;
    private JwksKey rotatedKey;
    private JwksKey expiredKey;
    
    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(jwtKeyRotationService, "keyRotationEnabled", true);
        ReflectionTestUtils.setField(jwtKeyRotationService, "rollingUpgradeWindowHours", 24);
        ReflectionTestUtils.setField(jwtKeyRotationService, "cleanupExpiredKeys", true);
        ReflectionTestUtils.setField(jwtKeyRotationService, "keySize", 256);
        ReflectionTestUtils.setField(jwtKeyRotationService, "defaultAlgorithm", "HS256");
        
        // Create test keys
        activeKey = createTestKey("key-active-123", true, null, null);
        rotatedKey = createTestKey("key-rotated-456", false, 
            LocalDateTime.now().minusHours(12), LocalDateTime.now().plusHours(12));
        expiredKey = createTestKey("key-expired-789", false, 
            LocalDateTime.now().minusHours(48), LocalDateTime.now().minusHours(1));
    }
    
    @Test
    @DisplayName("Should get current active signing key")
    void shouldGetCurrentActiveSigningKey() {
        // Given
        when(jwksKeyRepository.findActiveSigningKey()).thenReturn(Optional.of(activeKey));
        
        // When
        JwksKey result = jwtKeyRotationService.getCurrentSigningKey();
        
        // Then
        assertThat(result).isEqualTo(activeKey);
        assertThat(result.getActive()).isTrue();
        verify(jwksKeyRepository).findActiveSigningKey();
    }
    
    @Test
    @DisplayName("Should create new signing key when none exists")
    void shouldCreateNewSigningKeyWhenNoneExists() {
        // Given
        when(jwksKeyRepository.findActiveSigningKey()).thenReturn(Optional.empty());
        when(jwksKeyRepository.existsByKid(anyString())).thenReturn(false);
        when(jwksKeyRepository.save(any(JwksKey.class))).thenReturn(activeKey);
        
        // When
        JwksKey result = jwtKeyRotationService.getCurrentSigningKey();
        
        // Then
        assertThat(result).isNotNull();
        verify(jwksKeyRepository).findActiveSigningKey();
        verify(jwksKeyRepository).save(any(JwksKey.class));
    }
    
    @Test
    @DisplayName("Should get key by kid")
    void shouldGetKeyByKid() {
        // Given
        String kid = "key-test-123";
        when(jwksKeyRepository.findByKid(kid)).thenReturn(Optional.of(activeKey));
        
        // When
        Optional<JwksKey> result = jwtKeyRotationService.getKeyByKid(kid);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(activeKey);
        verify(jwksKeyRepository).findByKid(kid);
    }
    
    @Test
    @DisplayName("Should get valid verification keys")
    void shouldGetValidVerificationKeys() {
        // Given
        List<JwksKey> validKeys = List.of(activeKey, rotatedKey);
        when(jwksKeyRepository.findValidVerificationKeys(any(LocalDateTime.class)))
            .thenReturn(validKeys);
        
        // When
        List<JwksKey> result = jwtKeyRotationService.getValidVerificationKeys();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(activeKey, rotatedKey);
        verify(jwksKeyRepository).findValidVerificationKeys(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should get JWKS keys for endpoint")
    void shouldGetJwksKeys() {
        // Given
        List<JwksKey> jwksKeys = List.of(activeKey, rotatedKey);
        when(jwksKeyRepository.findJwksKeys(any(LocalDateTime.class))).thenReturn(jwksKeys);
        
        // When
        List<JwksKey> result = jwtKeyRotationService.getJwksKeys();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(activeKey, rotatedKey);
        verify(jwksKeyRepository).findJwksKeys(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should rotate signing key successfully")
    void shouldRotateSigningKeySuccessfully() {
        // Given
        JwksKey newKey = createTestKey("key-new-999", true, null, null);
        when(jwksKeyRepository.deactivateAllKeys(any(LocalDateTime.class))).thenReturn(1);
        when(jwksKeyRepository.existsByKid(anyString())).thenReturn(false);
        when(jwksKeyRepository.save(any(JwksKey.class))).thenReturn(newKey);
        when(jwksKeyRepository.expireOldRotatedKeys(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(0);
        
        // When
        JwksKey result = jwtKeyRotationService.rotateSigningKey();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();
        verify(jwksKeyRepository).deactivateAllKeys(any(LocalDateTime.class));
        verify(jwksKeyRepository).save(any(JwksKey.class));
        verify(jwksKeyRepository).expireOldRotatedKeys(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should validate active key for verification")
    void shouldValidateActiveKeyForVerification() {
        // When
        boolean result = jwtKeyRotationService.isKeyValidForVerification(activeKey);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("Should validate rotated key in rolling upgrade window")
    void shouldValidateRotatedKeyInRollingUpgradeWindow() {
        // When
        boolean result = jwtKeyRotationService.isKeyValidForVerification(rotatedKey);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("Should reject expired key for verification")
    void shouldRejectExpiredKeyForVerification() {
        // When
        boolean result = jwtKeyRotationService.isKeyValidForVerification(expiredKey);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("Should reject null key for verification")
    void shouldRejectNullKeyForVerification() {
        // When
        boolean result = jwtKeyRotationService.isKeyValidForVerification(null);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("Should get secret key for HMAC algorithm")
    void shouldGetSecretKeyForHmacAlgorithm() {
        // When
        SecretKey result = jwtKeyRotationService.getSecretKey(activeKey);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAlgorithm()).isEqualTo("HmacSHA256");
    }
    
    @Test
    @DisplayName("Should throw exception for non-HMAC algorithm")
    void shouldThrowExceptionForNonHmacAlgorithm() {
        // Given
        JwksKey rsaKey = createTestKey("key-rsa-123", true, null, null);
        rsaKey.setAlg("RS256");
        
        // When & Then
        assertThatThrownBy(() -> jwtKeyRotationService.getSecretKey(rsaKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Key is not suitable for HMAC algorithms");
    }
    
    @Test
    @DisplayName("Should cleanup expired keys")
    void shouldCleanupExpiredKeys() {
        // Given
        List<JwksKey> expiredKeys = List.of(expiredKey);
        when(jwksKeyRepository.findExpiredKeys(any(LocalDateTime.class))).thenReturn(expiredKeys);
        when(jwksKeyRepository.deleteExpiredKeys(any(LocalDateTime.class))).thenReturn(1);
        
        // When
        jwtKeyRotationService.cleanupExpiredKeys();
        
        // Then
        verify(jwksKeyRepository).findExpiredKeys(any(LocalDateTime.class));
        verify(jwksKeyRepository).deleteExpiredKeys(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should skip cleanup when disabled")
    void shouldSkipCleanupWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(jwtKeyRotationService, "cleanupExpiredKeys", false);
        
        // When
        jwtKeyRotationService.cleanupExpiredKeys();
        
        // Then
        verify(jwksKeyRepository, never()).findExpiredKeys(any(LocalDateTime.class));
        verify(jwksKeyRepository, never()).deleteExpiredKeys(any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should expire old rotated keys")
    void shouldExpireOldRotatedKeys() {
        // Given
        when(jwksKeyRepository.expireOldRotatedKeys(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(2);
        
        // When
        jwtKeyRotationService.expireOldRotatedKeys();
        
        // Then
        verify(jwksKeyRepository).expireOldRotatedKeys(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should skip expiration when key rotation disabled")
    void shouldSkipExpirationWhenKeyRotationDisabled() {
        // Given
        ReflectionTestUtils.setField(jwtKeyRotationService, "keyRotationEnabled", false);
        
        // When
        jwtKeyRotationService.expireOldRotatedKeys();
        
        // Then
        verify(jwksKeyRepository, never()).expireOldRotatedKeys(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    @DisplayName("Should force key rotation with reason")
    void shouldForceKeyRotationWithReason() {
        // Given
        String reason = "Security incident";
        JwksKey newKey = createTestKey("key-emergency-123", true, null, null);
        when(jwksKeyRepository.deactivateAllKeys(any(LocalDateTime.class))).thenReturn(1);
        when(jwksKeyRepository.existsByKid(anyString())).thenReturn(false);
        when(jwksKeyRepository.save(any(JwksKey.class))).thenReturn(newKey);
        when(jwksKeyRepository.expireOldRotatedKeys(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(0);
        
        // When
        JwksKey result = jwtKeyRotationService.forceKeyRotation(reason);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();
        verify(jwksKeyRepository).deactivateAllKeys(any(LocalDateTime.class));
        verify(jwksKeyRepository).save(any(JwksKey.class));
    }
    
    @Test
    @DisplayName("Should get key rotation statistics")
    void shouldGetKeyRotationStatistics() {
        // Given
        when(jwksKeyRepository.countActiveSigningKeys()).thenReturn(1L);
        when(jwksKeyRepository.findKeysInRollingUpgradeWindow(any(LocalDateTime.class)))
            .thenReturn(List.of(rotatedKey));
        when(jwksKeyRepository.findExpiredKeys(any(LocalDateTime.class)))
            .thenReturn(List.of(expiredKey));
        
        // When
        JwtKeyRotationService.KeyRotationStats result = jwtKeyRotationService.getKeyRotationStats();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActiveSigningKeys()).isEqualTo(1L);
        assertThat(result.getKeysInRollingUpgradeWindow()).isEqualTo(1);
        assertThat(result.getExpiredKeys()).isEqualTo(1);
        assertThat(result.getRollingUpgradeWindowHours()).isEqualTo(24);
        assertThat(result.isKeyRotationEnabled()).isTrue();
    }
    
    @Test
    @DisplayName("Should create new signing key with unique kid")
    void shouldCreateNewSigningKeyWithUniqueKid() {
        // Given
        when(jwksKeyRepository.existsByKid(anyString()))
            .thenReturn(true)  // First attempt conflicts
            .thenReturn(false); // Second attempt is unique
        when(jwksKeyRepository.save(any(JwksKey.class))).thenReturn(activeKey);
        
        // When
        JwksKey result = jwtKeyRotationService.createNewSigningKey();
        
        // Then
        assertThat(result).isNotNull();
        verify(jwksKeyRepository, times(2)).existsByKid(anyString());
        verify(jwksKeyRepository).save(any(JwksKey.class));
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
            .description("Test key for unit tests")
            .build();
    }
}
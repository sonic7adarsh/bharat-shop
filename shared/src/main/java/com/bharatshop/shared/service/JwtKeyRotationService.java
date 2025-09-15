package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.JwksKey;
import com.bharatshop.shared.repository.JwksKeyRepository;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for JWT key rotation and management
 * Handles key generation, rotation, and cleanup with rolling upgrade support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtKeyRotationService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtKeyRotationService.class);
    
    private final JwksKeyRepository jwksKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${jwt.key-rotation.enabled:true}")
    private boolean keyRotationEnabled;
    
    @Value("${jwt.key-rotation.rolling-upgrade-window-hours:24}")
    private int rollingUpgradeWindowHours;
    
    @Value("${jwt.key-rotation.cleanup-expired-keys:true}")
    private boolean cleanupExpiredKeys;
    
    @Value("${jwt.key-rotation.key-size:256}")
    private int keySize;
    
    @Value("${jwt.algorithm:HS256}")
    private String defaultAlgorithm;
    
    /**
     * Get the current active signing key
     * Creates a new key if none exists
     */
    @Transactional
    public JwksKey getCurrentSigningKey() {
        Optional<JwksKey> activeKey = jwksKeyRepository.findActiveSigningKey();
        
        if (activeKey.isPresent()) {
            return activeKey.get();
        }
        
        // No active key found, create a new one
        log.warn("No active signing key found, creating new key");
        return createNewSigningKey();
    }
    
    /**
     * Get a key by its key ID for verification
     */
    public Optional<JwksKey> getKeyByKid(String kid) {
        return jwksKeyRepository.findByKid(kid);
    }
    
    /**
     * Get all keys valid for verification (active + rolling upgrade window)
     */
    public List<JwksKey> getValidVerificationKeys() {
        return jwksKeyRepository.findValidVerificationKeys(LocalDateTime.now());
    }
    
    /**
     * Get all keys for JWKS endpoint
     */
    public List<JwksKey> getJwksKeys() {
        return jwksKeyRepository.findJwksKeys(LocalDateTime.now());
    }
    
    /**
     * Rotate the current signing key
     * Creates a new key and marks the current one as rotated
     */
    @Transactional
    public JwksKey rotateSigningKey() {
        log.info("Starting JWT key rotation");
        
        // Deactivate all current active keys
        int deactivatedCount = jwksKeyRepository.deactivateAllKeys(LocalDateTime.now());
        log.info("Deactivated {} keys during rotation", deactivatedCount);
        
        // Create new signing key
        JwksKey newKey = createNewSigningKey();
        
        // Schedule expiration of old rotated keys after rolling upgrade window
        scheduleKeyExpiration();
        
        log.info("JWT key rotation completed. New key ID: {}", newKey.getKid());
        return newKey;
    }
    
    /**
     * Create a new signing key
     */
    @Transactional
    public JwksKey createNewSigningKey() {
        String kid = generateKeyId();
        String keyMaterial = generateKeyMaterial();
        
        JwksKey newKey = JwksKey.builder()
            .kid(kid)
            .alg(defaultAlgorithm)
            .keyMaterial(keyMaterial)
            .active(true)
            .usage(JwksKey.KeyUsage.SIGNING)
            .keySize(keySize)
            .description("Auto-generated signing key")
            .build();
        
        JwksKey savedKey = jwksKeyRepository.save(newKey);
        log.info("Created new signing key with ID: {}", kid);
        
        return savedKey;
    }
    
    /**
     * Validate that a key is suitable for verification
     */
    public boolean isKeyValidForVerification(JwksKey key) {
        if (key == null) {
            return false;
        }
        
        // Active keys are always valid
        if (key.getActive()) {
            return true;
        }
        
        // Check if key is in rolling upgrade window
        return key.isValidForVerification();
    }
    
    /**
     * Get key material as SecretKey for HMAC algorithms
     */
    public SecretKey getSecretKey(JwksKey key) {
        if (!key.getAlg().startsWith("HS")) {
            throw new IllegalArgumentException("Key is not suitable for HMAC algorithms: " + key.getAlg());
        }
        
        byte[] keyBytes = Decoders.BASE64.decode(key.getKeyMaterial());
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Scheduled task to clean up expired keys
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void cleanupExpiredKeys() {
        if (!cleanupExpiredKeys) {
            return;
        }
        
        log.info("Starting cleanup of expired JWT keys");
        
        List<JwksKey> expiredKeys = jwksKeyRepository.findExpiredKeys(LocalDateTime.now());
        
        if (!expiredKeys.isEmpty()) {
            int deletedCount = jwksKeyRepository.deleteExpiredKeys(LocalDateTime.now());
            log.info("Cleaned up {} expired JWT keys", deletedCount);
        } else {
            log.debug("No expired JWT keys found for cleanup");
        }
    }
    
    /**
     * Scheduled task to expire old rotated keys after rolling upgrade window
     */
    @Scheduled(cron = "0 30 1 * * ?") // Run daily at 1:30 AM
    @Transactional
    public void expireOldRotatedKeys() {
        if (!keyRotationEnabled) {
            return;
        }
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(rollingUpgradeWindowHours);
        LocalDateTime expirationTime = LocalDateTime.now();
        
        int expiredCount = jwksKeyRepository.expireOldRotatedKeys(expirationTime, cutoffTime);
        
        if (expiredCount > 0) {
            log.info("Expired {} old rotated keys that exceeded rolling upgrade window", expiredCount);
        }
    }
    
    /**
     * Manual key rotation endpoint (for emergency situations)
     */
    @Transactional
    public JwksKey forceKeyRotation(String reason) {
        log.warn("Force key rotation requested. Reason: {}", reason);
        return rotateSigningKey();
    }
    
    /**
     * Get key rotation statistics
     */
    public KeyRotationStats getKeyRotationStats() {
        long activeKeys = jwksKeyRepository.countActiveSigningKeys();
        List<JwksKey> rollingUpgradeKeys = jwksKeyRepository.findKeysInRollingUpgradeWindow(LocalDateTime.now());
        List<JwksKey> expiredKeys = jwksKeyRepository.findExpiredKeys(LocalDateTime.now());
        
        // Find the most recent rotation time
        LocalDateTime lastRotationTime = rollingUpgradeKeys.stream()
            .filter(key -> key.getRotatedAt() != null)
            .map(JwksKey::getRotatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        return KeyRotationStats.builder()
            .activeSigningKeys(activeKeys)
            .keysInRollingUpgradeWindow(rollingUpgradeKeys.size())
            .expiredKeys(expiredKeys.size())
            .rollingUpgradeWindowHours(rollingUpgradeWindowHours)
            .keyRotationEnabled(keyRotationEnabled)
            .lastRotationTime(lastRotationTime)
            .build();
    }
    
    /**
     * Generate a unique key ID
     */
    private String generateKeyId() {
        String kid;
        do {
            kid = "key-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        } while (jwksKeyRepository.existsByKid(kid));
        
        return kid;
    }
    
    /**
     * Generate secure key material for HMAC
     */
    private String generateKeyMaterial() {
        byte[] keyBytes = new byte[keySize / 8]; // Convert bits to bytes
        secureRandom.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
    
    /**
     * Schedule expiration of rotated keys after rolling upgrade window
     */
    private void scheduleKeyExpiration() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(rollingUpgradeWindowHours);
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(rollingUpgradeWindowHours);
        
        int scheduledCount = jwksKeyRepository.expireOldRotatedKeys(expirationTime, cutoffTime);
        
        if (scheduledCount > 0) {
            log.info("Scheduled {} old rotated keys for expiration", scheduledCount);
        }
    }
    
    /**
     * Key rotation statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class KeyRotationStats {
        

        private long activeSigningKeys;
        private int keysInRollingUpgradeWindow;
        private int expiredKeys;
        private int rollingUpgradeWindowHours;
        private boolean keyRotationEnabled;
        private LocalDateTime lastRotationTime;
        
        public long getActiveSigningKeys() {
            return activeSigningKeys;
        }
        
        public int getKeysInRollingUpgradeWindow() {
            return keysInRollingUpgradeWindow;
        }
        
        public int getExpiredKeys() {
            return expiredKeys;
        }
        
        public int getRollingUpgradeWindowHours() {
            return rollingUpgradeWindowHours;
        }
        
        public boolean isKeyRotationEnabled() {
            return keyRotationEnabled;
        }
        
        public LocalDateTime getLastRotationTime() {
            return lastRotationTime;
        }
    }
}
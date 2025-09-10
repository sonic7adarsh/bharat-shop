package com.bharatshop.shared.controller;

import com.bharatshop.shared.entity.JwksKey;
import com.bharatshop.shared.service.JwtKeyRotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for JWKS (JSON Web Key Set) endpoints
 * Provides public key information for JWT verification
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class JwksController {
    
    private final JwtKeyRotationService jwtKeyRotationService;
    
    /**
     * JWKS endpoint for public key distribution
     * Returns all keys that are valid for JWT verification
     */
    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {
        try {
            List<JwksKey> jwksKeys = jwtKeyRotationService.getJwksKeys();
            
            List<Map<String, Object>> keys = jwksKeys.stream()
                .map(this::convertToJwkFormat)
                .collect(Collectors.toList());
            
            Map<String, Object> jwks = new HashMap<>();
            jwks.put("keys", keys);
            
            log.debug("Serving JWKS with {} keys", keys.size());
            
            // Cache for 1 hour to reduce load, but allow refresh for key rotation
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1))
                    .mustRevalidate()
                    .cachePublic())
                .header("Content-Type", "application/json")
                .body(jwks);
                
        } catch (Exception e) {
            log.error("Error serving JWKS endpoint", e);
            
            // Return empty key set on error to prevent service disruption
            Map<String, Object> emptyJwks = new HashMap<>();
            emptyJwks.put("keys", List.of());
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("Content-Type", "application/json")
                .body(emptyJwks);
        }
    }
    
    /**
     * Health check endpoint for JWKS service
     */
    @GetMapping("/.well-known/jwks/health")
    public ResponseEntity<Map<String, Object>> getJwksHealth() {
        try {
            JwtKeyRotationService.KeyRotationStats stats = jwtKeyRotationService.getKeyRotationStats();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("activeSigningKeys", stats.getActiveSigningKeys());
            health.put("keysInRollingUpgradeWindow", stats.getKeysInRollingUpgradeWindow());
            health.put("expiredKeys", stats.getExpiredKeys());
            health.put("keyRotationEnabled", stats.isKeyRotationEnabled());
            health.put("rollingUpgradeWindowHours", stats.getRollingUpgradeWindowHours());
            
            return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(health);
                
        } catch (Exception e) {
            log.error("Error checking JWKS health", e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            
            return ResponseEntity.status(500)
                .cacheControl(CacheControl.noCache())
                .body(health);
        }
    }
    
    /**
     * Convert JwksKey entity to JWK format for JWKS endpoint
     */
    private Map<String, Object> convertToJwkFormat(JwksKey key) {
        Map<String, Object> jwk = new HashMap<>();
        
        // Standard JWK fields
        jwk.put("kid", key.getKid());
        jwk.put("kty", getKeyType(key.getAlg()));
        jwk.put("alg", key.getAlg());
        jwk.put("use", "sig"); // Signature use
        
        // For HMAC keys, we don't expose the key material in JWKS
        // This is for symmetric keys used in server-to-server communication
        if (key.getAlg().startsWith("HS")) {
            // For HMAC, we only provide metadata, not the actual key
            jwk.put("k", "[PROTECTED]"); // Indicate key is protected
        }
        
        // Additional metadata
        jwk.put("key_ops", List.of("verify"));
        
        if (key.getKeySize() != null) {
            jwk.put("key_size", key.getKeySize());
        }
        
        return jwk;
    }
    
    /**
     * Get JWK key type from algorithm
     */
    private String getKeyType(String algorithm) {
        if (algorithm.startsWith("HS")) {
            return "oct"; // Octet sequence (symmetric key)
        } else if (algorithm.startsWith("RS") || algorithm.startsWith("PS")) {
            return "RSA";
        } else if (algorithm.startsWith("ES")) {
            return "EC"; // Elliptic Curve
        }
        
        return "oct"; // Default to symmetric
    }
}
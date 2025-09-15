package com.bharatshop.shared.config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates JWT configuration on application startup
 * Ensures all required environment variables are properly set
 */
@Component
@Slf4j
public class JwtConfigValidator {
    
    @Value("${jwt.access-token.expiration:86400000}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration:604800000}")
    private long refreshTokenExpiration;
    
    @Value("${jwt.algorithm:HS256}")
    private String algorithm;
    
    @Value("${jwt.key-rotation.enabled:true}")
    private boolean keyRotationEnabled;
    
    @Value("${jwt.key-rotation.rolling-upgrade-window-hours:24}")
    private int rollingUpgradeWindowHours;
    
    @Value("${jwt.key-rotation.cleanup-expired-keys:true}")
    private boolean cleanupExpiredKeys;
    
    @Value("${jwt.key-rotation.key-size:256}")
    private int keySize;
    
    // Database configuration
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username:}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password:}")
    private String datasourcePassword;
    
    // Redis configuration (optional)
    @Value("${spring.data.redis.host:}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    // CAPTCHA configuration
    @Value("${captcha.secret-key:}")
    private String captchaSecretKey;
    
    // Razorpay configuration
    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;
    
    /**
     * Validate configuration immediately after bean construction
     */
    @PostConstruct
    public void validateConfiguration() {
        System.out.println("Starting JWT configuration validation...");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate JWT settings
        validateJwtSettings(errors, warnings);
        
        // Validate database configuration
        validateDatabaseConfig(errors, warnings);
        
        // Validate external service configurations
        validateExternalServices(warnings);
        
        // Log results
        if (!errors.isEmpty()) {
            System.out.println("JWT Configuration validation failed with " + errors.size() + " errors:");
            errors.forEach(error -> System.out.println("  - " + error));
            throw new IllegalStateException("JWT configuration validation failed. Check logs for details.");
        }
        
        if (!warnings.isEmpty()) {
            System.out.println("JWT Configuration validation completed with " + warnings.size() + " warnings:");
            warnings.forEach(warning -> System.out.println("  - " + warning));
        } else {
            System.out.println("JWT configuration validation completed successfully");
        }
    }
    
    /**
     * Additional validation after application is fully ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("Application ready - JWT configuration summary:");
        System.out.println("  - Algorithm: " + algorithm);
        System.out.println("  - Access token expiration: " + accessTokenExpiration + " ms (" + (accessTokenExpiration / 3600000) + " hours)");
        System.out.println("  - Refresh token expiration: " + refreshTokenExpiration + " ms (" + (refreshTokenExpiration / 86400000) + " days)");
        System.out.println("  - Key rotation enabled: " + keyRotationEnabled);
        
        if (keyRotationEnabled) {
            System.out.println("  - Rolling upgrade window: " + rollingUpgradeWindowHours + " hours");
            System.out.println("  - Key size: " + keySize + " bits");
            System.out.println("  - Cleanup expired keys: " + cleanupExpiredKeys);
        }
    }
    
    private void validateJwtSettings(List<String> errors, List<String> warnings) {
        // Validate algorithm
        if (!isValidAlgorithm(algorithm)) {
            errors.add("Invalid JWT algorithm: " + algorithm + ". Supported: HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512");
        }
        
        // Validate token expiration times
        if (accessTokenExpiration <= 0) {
            errors.add("Access token expiration must be positive: " + accessTokenExpiration);
        } else if (accessTokenExpiration < 300000) { // Less than 5 minutes
            warnings.add("Access token expiration is very short: " + accessTokenExpiration + "ms (" + (accessTokenExpiration/1000) + "s)");
        } else if (accessTokenExpiration > 86400000) { // More than 24 hours
            warnings.add("Access token expiration is very long: " + accessTokenExpiration + "ms (" + (accessTokenExpiration/3600000) + "h)");
        }
        
        if (refreshTokenExpiration <= 0) {
            errors.add("Refresh token expiration must be positive: " + refreshTokenExpiration);
        } else if (refreshTokenExpiration <= accessTokenExpiration) {
            warnings.add("Refresh token expiration should be longer than access token expiration");
        }
        
        // Validate key rotation settings
        if (keyRotationEnabled) {
            if (rollingUpgradeWindowHours <= 0) {
                errors.add("Rolling upgrade window must be positive: " + rollingUpgradeWindowHours);
            } else if (rollingUpgradeWindowHours < 1) {
                warnings.add("Rolling upgrade window is very short: " + rollingUpgradeWindowHours + " hours");
            }
            
            if (keySize < 256) {
                warnings.add("Key size is below recommended minimum: " + keySize + " bits (recommended: 256+)");
            }
            
            if (!algorithm.startsWith("HS") && keySize != 256) {
                warnings.add("Key size setting may not apply to algorithm: " + algorithm);
            }
        }
    }
    
    private void validateDatabaseConfig(List<String> errors, List<String> warnings) {
        if (datasourceUrl == null || datasourceUrl.trim().isEmpty()) {
            errors.add("Database URL is not configured (spring.datasource.url)");
        }
        
        if (datasourceUsername == null || datasourceUsername.trim().isEmpty()) {
            warnings.add("Database username is not configured (spring.datasource.username)");
        }
        
        // Don't log password, just check if it's set
        if (datasourcePassword == null || datasourcePassword.trim().isEmpty()) {
            warnings.add("Database password is not configured (spring.datasource.password)");
        }
    }
    
    private void validateExternalServices(List<String> warnings) {
        // Redis validation (optional service)
        if (redisHost != null && !redisHost.trim().isEmpty()) {
            System.out.println("Redis configuration detected - host: " + redisHost + ", port: " + redisPort);
        } else {
            warnings.add("Redis is not configured - rate limiting will use in-memory fallback");
        }
        
        // CAPTCHA validation
        if (captchaSecretKey == null || captchaSecretKey.trim().isEmpty()) {
            warnings.add("CAPTCHA secret key is not configured (captcha.secret-key)");
        }
        
        // Razorpay validation
        if (razorpayKeyId == null || razorpayKeyId.trim().isEmpty()) {
            warnings.add("Razorpay key ID is not configured (razorpay.key-id)");
        }
        
        if (razorpayKeySecret == null || razorpayKeySecret.trim().isEmpty()) {
            warnings.add("Razorpay key secret is not configured (razorpay.key-secret)");
        }
    }
    
    private boolean isValidAlgorithm(String alg) {
        return alg != null && (
            alg.equals("HS256") || alg.equals("HS384") || alg.equals("HS512") ||
            alg.equals("RS256") || alg.equals("RS384") || alg.equals("RS512") ||
            alg.equals("ES256") || alg.equals("ES384") || alg.equals("ES512")
        );
    }
    
    /**
     * Get configuration summary for health checks
     */
    public ConfigSummary getConfigSummary() {
        ConfigSummary summary = new ConfigSummary();
        summary.algorithm = algorithm;
        summary.accessTokenExpirationHours = accessTokenExpiration / 3600000;
        summary.refreshTokenExpirationDays = refreshTokenExpiration / 86400000;
        summary.keyRotationEnabled = keyRotationEnabled;
        summary.rollingUpgradeWindowHours = rollingUpgradeWindowHours;
        summary.keySize = keySize;
        summary.cleanupExpiredKeys = cleanupExpiredKeys;
        summary.databaseConfigured = !datasourceUrl.isEmpty();
        summary.redisConfigured = !redisHost.isEmpty();
        summary.captchaConfigured = !captchaSecretKey.isEmpty();
        summary.razorpayConfigured = !razorpayKeyId.isEmpty() && !razorpayKeySecret.isEmpty();
        return summary;
    }
    
    public static class ConfigSummary {
        public String algorithm;
        public long accessTokenExpirationHours;
        public long refreshTokenExpirationDays;
        public boolean keyRotationEnabled;
        public int rollingUpgradeWindowHours;
        public int keySize;
        public boolean cleanupExpiredKeys;
        public boolean databaseConfigured;
        public boolean redisConfigured;
        public boolean captchaConfigured;
        public boolean razorpayConfigured;
    }
}
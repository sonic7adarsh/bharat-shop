package com.bharatshop.shared.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for CAPTCHA validation with abuse detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaService {
    
    // Manual logger since Lombok is not working properly
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CaptchaService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${app.captcha.enabled:true}")
    private boolean captchaEnabled;
    
    @Value("${app.captcha.provider:recaptcha}")
    private String captchaProvider;
    
    @Value("${app.captcha.recaptcha.secret-key:}")
    private String recaptchaSecretKey;
    
    @Value("${app.captcha.recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String recaptchaVerifyUrl;
    
    @Value("${app.captcha.hcaptcha.secret-key:}")
    private String hcaptchaSecretKey;
    
    @Value("${app.captcha.hcaptcha.verify-url:https://hcaptcha.com/siteverify}")
    private String hcaptchaVerifyUrl;
    
    @Value("${app.captcha.abuse-threshold:3}")
    private int abuseThreshold;
    
    @Value("${app.captcha.abuse-window-minutes:15}")
    private int abuseWindowMinutes;
    
    @Value("${app.captcha.min-score:0.5}")
    private double minScore;
    
    // In-memory tracking for abuse detection (in production, use Redis or database)
    private final Map<String, AttemptTracker> attemptTrackers = new ConcurrentHashMap<>();
    
    /**
     * Validate CAPTCHA token
     */
    public CaptchaValidationResult validateCaptcha(CaptchaValidationRequest request) {
        try {
            // Check if CAPTCHA is enabled
            if (!captchaEnabled) {
                log.debug("CAPTCHA validation disabled, allowing request");
                return CaptchaValidationResult.success("CAPTCHA validation disabled");
            }
            
            // Validate input
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return CaptchaValidationResult.failure("CAPTCHA token is required");
            }
            
            // Check if CAPTCHA is required based on abuse detection
            if (!isCaptchaRequired(request.getIdentifier())) {
                log.debug("CAPTCHA not required for identifier: {}", 
                    maskIdentifier(request.getIdentifier()));
                return CaptchaValidationResult.success("CAPTCHA not required");
            }
            
            // Validate based on provider
            CaptchaValidationResult result;
            switch (captchaProvider.toLowerCase()) {
                case "recaptcha":
                    result = validateRecaptcha(request);
                    break;
                case "hcaptcha":
                    result = validateHcaptcha(request);
                    break;
                case "mock":
                    result = validateMockCaptcha(request);
                    break;
                default:
                    log.error("Unknown CAPTCHA provider: {}", captchaProvider);
                    return CaptchaValidationResult.failure("Invalid CAPTCHA provider configuration");
            }
            
            // Update attempt tracking
            updateAttemptTracking(request.getIdentifier(), result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error validating CAPTCHA for identifier {}: {}", 
                maskIdentifier(request.getIdentifier()), e.getMessage(), e);
            return CaptchaValidationResult.failure("CAPTCHA validation failed");
        }
    }
    
    /**
     * Check if CAPTCHA is required based on abuse detection
     */
    public boolean isCaptchaRequired(String identifier) {
        if (!captchaEnabled) {
            return false;
        }
        
        AttemptTracker tracker = attemptTrackers.get(identifier);
        if (tracker == null) {
            return false;
        }
        
        // Clean up expired attempts
        tracker.cleanupExpiredAttempts();
        
        // Check if attempts exceed threshold
        return tracker.getFailedAttempts() >= abuseThreshold;
    }
    
    /**
     * Record failed authentication attempt for abuse detection
     */
    public void recordFailedAttempt(String identifier) {
        AttemptTracker tracker = attemptTrackers.computeIfAbsent(
            identifier, k -> new AttemptTracker());
        tracker.recordFailedAttempt();
        
        log.debug("Recorded failed attempt for identifier: {}, total: {}", 
            maskIdentifier(identifier), tracker.getFailedAttempts());
    }
    
    /**
     * Record successful authentication attempt
     */
    public void recordSuccessfulAttempt(String identifier) {
        AttemptTracker tracker = attemptTrackers.get(identifier);
        if (tracker != null) {
            tracker.recordSuccessfulAttempt();
            
            // Remove tracker if no recent failed attempts
            if (tracker.getFailedAttempts() == 0) {
                attemptTrackers.remove(identifier);
            }
        }
        
        log.debug("Recorded successful attempt for identifier: {}", 
            maskIdentifier(identifier));
    }
    
    /**
     * Reset attempt tracking for identifier
     */
    public void resetAttemptTracking(String identifier) {
        attemptTrackers.remove(identifier);
        log.debug("Reset attempt tracking for identifier: {}", 
            maskIdentifier(identifier));
    }
    
    /**
     * Validate Google reCAPTCHA
     */
    private CaptchaValidationResult validateRecaptcha(CaptchaValidationRequest request) {
        if (recaptchaSecretKey == null || recaptchaSecretKey.trim().isEmpty()) {
            log.error("reCAPTCHA secret key not configured");
            return CaptchaValidationResult.failure("CAPTCHA service not configured");
        }
        
        try {
            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", recaptchaSecretKey);
            params.add("response", request.getToken());
            if (request.getRemoteIp() != null) {
                params.add("remoteip", request.getRemoteIp());
            }
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            
            // Make request
            ResponseEntity<Map> response = restTemplate.exchange(
                recaptchaVerifyUrl, HttpMethod.POST, entity, Map.class);
            
            if (response.getBody() == null) {
                return CaptchaValidationResult.failure("Invalid CAPTCHA response");
            }
            
            Map<String, Object> responseBody = response.getBody();
            Boolean success = (Boolean) responseBody.get("success");
            
            if (Boolean.TRUE.equals(success)) {
                // Check score for reCAPTCHA v3
                Object scoreObj = responseBody.get("score");
                if (scoreObj instanceof Number) {
                    double score = ((Number) scoreObj).doubleValue();
                    if (score < minScore) {
                        log.warn("reCAPTCHA score {} below minimum {}", score, minScore);
                        return CaptchaValidationResult.failure("CAPTCHA score too low");
                    }
                }
                
                return CaptchaValidationResult.success("reCAPTCHA validation successful");
            } else {
                Object errorCodes = responseBody.get("error-codes");
                log.warn("reCAPTCHA validation failed: {}", errorCodes);
                return CaptchaValidationResult.failure("CAPTCHA validation failed");
            }
            
        } catch (Exception e) {
            log.error("Error validating reCAPTCHA: {}", e.getMessage(), e);
            return CaptchaValidationResult.failure("CAPTCHA service error");
        }
    }
    
    /**
     * Validate hCaptcha
     */
    private CaptchaValidationResult validateHcaptcha(CaptchaValidationRequest request) {
        if (hcaptchaSecretKey == null || hcaptchaSecretKey.trim().isEmpty()) {
            log.error("hCaptcha secret key not configured");
            return CaptchaValidationResult.failure("CAPTCHA service not configured");
        }
        
        try {
            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", hcaptchaSecretKey);
            params.add("response", request.getToken());
            if (request.getRemoteIp() != null) {
                params.add("remoteip", request.getRemoteIp());
            }
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            
            // Make request
            ResponseEntity<Map> response = restTemplate.exchange(
                hcaptchaVerifyUrl, HttpMethod.POST, entity, Map.class);
            
            if (response.getBody() == null) {
                return CaptchaValidationResult.failure("Invalid CAPTCHA response");
            }
            
            Map<String, Object> responseBody = response.getBody();
            Boolean success = (Boolean) responseBody.get("success");
            
            if (Boolean.TRUE.equals(success)) {
                return CaptchaValidationResult.success("hCaptcha validation successful");
            } else {
                Object errorCodes = responseBody.get("error-codes");
                log.warn("hCaptcha validation failed: {}", errorCodes);
                return CaptchaValidationResult.failure("CAPTCHA validation failed");
            }
            
        } catch (Exception e) {
            log.error("Error validating hCaptcha: {}", e.getMessage(), e);
            return CaptchaValidationResult.failure("CAPTCHA service error");
        }
    }
    
    /**
     * Mock CAPTCHA validation for testing
     */
    private CaptchaValidationResult validateMockCaptcha(CaptchaValidationRequest request) {
        // For testing purposes - accept specific test tokens
        String token = request.getToken();
        
        if ("test-success".equals(token) || "valid-captcha".equals(token)) {
            return CaptchaValidationResult.success("Mock CAPTCHA validation successful");
        } else if ("test-failure".equals(token) || "invalid-captcha".equals(token)) {
            return CaptchaValidationResult.failure("Mock CAPTCHA validation failed");
        } else {
            // Default behavior for unknown tokens
            return CaptchaValidationResult.success("Mock CAPTCHA validation (default success)");
        }
    }
    
    /**
     * Mask identifier for logging
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 4) {
            return "****";
        }
        return identifier.substring(0, 2) + "****" + identifier.substring(identifier.length() - 2);
    }
    
    /**
     * Attempt tracker for abuse detection
     */
    private class AttemptTracker {
        private final AtomicInteger failedAttempts = new AtomicInteger(0);
        private volatile long lastFailedAttempt = 0;
        private volatile long lastSuccessfulAttempt = 0;
        
        public void recordFailedAttempt() {
            failedAttempts.incrementAndGet();
            lastFailedAttempt = System.currentTimeMillis();
        }
        
        public void recordSuccessfulAttempt() {
            failedAttempts.set(0);
            lastSuccessfulAttempt = System.currentTimeMillis();
        }
        
        public int getFailedAttempts() {
            cleanupExpiredAttempts();
            return failedAttempts.get();
        }
        
        public void cleanupExpiredAttempts() {
            long now = System.currentTimeMillis();
            long windowMs = abuseWindowMinutes * 60 * 1000L;
            
            if (now - lastFailedAttempt > windowMs) {
                failedAttempts.set(0);
            }
        }
    }
    
    /**
     * Update attempt tracking based on validation result
     */
    private void updateAttemptTracking(String identifier, boolean success) {
        if (success) {
            recordSuccessfulAttempt(identifier);
        } else {
            recordFailedAttempt(identifier);
        }
    }
    
    // Result and request classes
    public static class CaptchaValidationResult {
        private final boolean success;
        private final String message;
        private final String errorCode;
        
        private CaptchaValidationResult(boolean success, String message, String errorCode) {
            this.success = success;
            this.message = message;
            this.errorCode = errorCode;
        }
        
        public static CaptchaValidationResult success(String message) {
            return new CaptchaValidationResult(true, message, null);
        }
        
        public static CaptchaValidationResult failure(String message) {
            return new CaptchaValidationResult(false, message, "CAPTCHA_VALIDATION_FAILED");
        }
        
        public static CaptchaValidationResult failure(String message, String errorCode) {
            return new CaptchaValidationResult(false, message, errorCode);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
    }
    
    public static class CaptchaValidationRequest {
        private final String token;
        private final String identifier;
        private final String remoteIp;
        private final String userAgent;
        
        public CaptchaValidationRequest(String token, String identifier, String remoteIp, String userAgent) {
            this.token = token;
            this.identifier = identifier;
            this.remoteIp = remoteIp;
            this.userAgent = userAgent;
        }
        
        // Getters
        public String getToken() { return token; }
        public String getIdentifier() { return identifier; }
        public String getRemoteIp() { return remoteIp; }
        public String getUserAgent() { return userAgent; }
    }
}
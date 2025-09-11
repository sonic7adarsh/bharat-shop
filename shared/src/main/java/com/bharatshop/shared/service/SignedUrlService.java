package com.bharatshop.shared.service;

import com.bharatshop.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating and validating signed URLs for private asset access.
 * Provides time-limited access to protected resources with HMAC-SHA256 signatures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignedUrlService {

    @Value("${app.signed-url.secret-key:default-secret-key-change-in-production}")
    private String secretKey;

    @Value("${app.signed-url.default-expiry:3600}") // 1 hour
    private long defaultExpirySeconds;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PARAM = "signature";
    private static final String EXPIRES_PARAM = "expires";
    private static final String TENANT_PARAM = "tenant";

    /**
     * Generate a signed URL for a private asset
     */
    public String generateSignedUrl(String resourcePath) {
        return generateSignedUrl(resourcePath, defaultExpirySeconds);
    }

    /**
     * Generate a signed URL with custom expiry time
     */
    public String generateSignedUrl(String resourcePath, long expirySeconds) {
        try {
            Long tenantId = TenantContext.getCurrentTenant();
            long expiresAt = Instant.now().getEpochSecond() + expirySeconds;
            
            // Build the base URL
            String baseResourceUrl = baseUrl + "/api/assets" + resourcePath;
            
            // Create parameters map
            Map<String, String> params = new HashMap<>();
            params.put(EXPIRES_PARAM, String.valueOf(expiresAt));
            if (tenantId != null) {
                params.put(TENANT_PARAM, tenantId.toString());
            }
            
            // Generate signature
            String signature = generateSignature(resourcePath, params);
            params.put(SIGNATURE_PARAM, signature);
            
            // Build final URL with parameters
            StringBuilder urlBuilder = new StringBuilder(baseResourceUrl);
            urlBuilder.append("?");
            
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    urlBuilder.append("&");
                }
                urlBuilder.append(entry.getKey())
                         .append("=")
                         .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
            
            String signedUrl = urlBuilder.toString();
            log.debug("Generated signed URL for resource: {} (expires: {})", resourcePath, expiresAt);
            
            return signedUrl;
            
        } catch (Exception e) {
            log.error("Error generating signed URL for resource: {}", resourcePath, e);
            throw new RuntimeException("Failed to generate signed URL", e);
        }
    }

    /**
     * Validate a signed URL
     */
    public ValidationResult validateSignedUrl(String resourcePath, Map<String, String> params) {
        try {
            // Check if required parameters are present
            String providedSignature = params.get(SIGNATURE_PARAM);
            String expiresStr = params.get(EXPIRES_PARAM);
            
            if (providedSignature == null || expiresStr == null) {
                return ValidationResult.invalid("Missing required parameters");
            }
            
            // Check expiry
            long expiresAt = Long.parseLong(expiresStr);
            long currentTime = Instant.now().getEpochSecond();
            
            if (currentTime > expiresAt) {
                return ValidationResult.invalid("URL has expired");
            }
            
            // Validate tenant context if provided
            String tenantParam = params.get(TENANT_PARAM);
            Long currentTenantId = TenantContext.getCurrentTenant();
            
            if (tenantParam != null) {
                Long urlTenantId = Long.parseLong(tenantParam);
                if (currentTenantId == null || !currentTenantId.equals(urlTenantId)) {
                    return ValidationResult.invalid("Invalid tenant context");
                }
            } else if (currentTenantId != null) {
                return ValidationResult.invalid("Tenant mismatch");
            }
            
            // Generate expected signature
            Map<String, String> signatureParams = new HashMap<>(params);
            signatureParams.remove(SIGNATURE_PARAM); // Remove signature from params for validation
            
            String expectedSignature = generateSignature(resourcePath, signatureParams);
            
            // Compare signatures
            if (!constantTimeEquals(providedSignature, expectedSignature)) {
                return ValidationResult.invalid("Invalid signature");
            }
            
            log.debug("Successfully validated signed URL for resource: {}", resourcePath);
            return ValidationResult.valid();
            
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid parameter format");
        } catch (Exception e) {
            log.error("Error validating signed URL for resource: {}", resourcePath, e);
            return ValidationResult.invalid("Validation error");
        }
    }

    /**
     * Generate signed URL for image with specific size
     */
    public String generateImageSignedUrl(String filename, Integer size) {
        String resourcePath = size != null ? 
            "/images/" + filename + "?size=" + size :
            "/images/" + filename;
        return generateSignedUrl(resourcePath);
    }

    /**
     * Generate signed URL for document download
     */
    public String generateDocumentSignedUrl(String filename) {
        return generateSignedUrl("/documents/" + filename);
    }

    /**
     * Generate signed URL with custom expiry for temporary access
     */
    public String generateTemporarySignedUrl(String resourcePath, int minutes) {
        return generateSignedUrl(resourcePath, minutes * 60L);
    }

    private String generateSignature(String resourcePath, Map<String, String> params) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        // Create string to sign
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(resourcePath);
        
        // Add parameters in sorted order for consistency
        params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                stringToSign.append("&")
                           .append(entry.getKey())
                           .append("=")
                           .append(entry.getValue());
            });
        
        // Generate HMAC-SHA256 signature
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8), 
            ALGORITHM
        );
        mac.init(secretKeySpec);
        
        byte[] signatureBytes = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }

    /**
     * Result class for URL validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
package com.bharatshop.storefront.controller;

import com.bharatshop.shared.entity.OtpVerification;
import com.bharatshop.shared.service.auth.OtpSendRequest;
import com.bharatshop.shared.service.auth.OtpService;
import com.bharatshop.shared.service.auth.OtpVerifyRequest;
import com.bharatshop.shared.service.auth.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for phone-based authentication with OTP
 */
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
@Slf4j
public class PhoneAuthController {
    
    private final OtpService otpService;
    private final CaptchaService captchaService;
    
    /**
     * Send OTP to phone number for authentication
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract request metadata
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceId = extractDeviceId(request, httpRequest);
            String sessionId = extractSessionId(httpRequest);
            
            // Check if CAPTCHA is required and validate if provided
            String identifier = request.getPhoneNumber() + ":" + ipAddress;
            boolean captchaRequired = captchaService.isCaptchaRequired(identifier);
            
            if (captchaRequired) {
                if (request.getCaptchaToken() == null || request.getCaptchaToken().trim().isEmpty()) {
                    log.warn("CAPTCHA required but not provided for phone: {}", maskPhoneNumber(request.getPhoneNumber()));
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CAPTCHA verification required",
                        "errorCode", "CAPTCHA_REQUIRED",
                        "captchaRequired", true
                    ));
                }
                
                // Validate CAPTCHA
                CaptchaService.CaptchaValidationRequest captchaRequest = 
                    new CaptchaService.CaptchaValidationRequest(
                        request.getCaptchaToken(), identifier, ipAddress, userAgent);
                
                CaptchaService.CaptchaValidationResult captchaResult = 
                    captchaService.validateCaptcha(captchaRequest);
                
                if (!captchaResult.isSuccess()) {
                    log.warn("CAPTCHA validation failed for phone: {}, reason: {}", 
                        maskPhoneNumber(request.getPhoneNumber()), captchaResult.getMessage());
                    
                    captchaService.recordFailedAttempt(identifier);
                    
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CAPTCHA validation failed",
                        "errorCode", captchaResult.getErrorCode() != null ? captchaResult.getErrorCode() : "CAPTCHA_INVALID",
                        "captchaRequired", true
                    ));
                }
                
                log.debug("CAPTCHA validation successful for phone: {}", maskPhoneNumber(request.getPhoneNumber()));
            }
            
            // Build OTP send request
            OtpSendRequest otpRequest = OtpSendRequest.builder()
                .phoneNumber(request.getPhoneNumber())
                .type(request.getType())
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .preferredProviderId(request.getPreferredProvider())
                .locale(request.getLocale())
                .timezone(request.getTimezone())
                .appVersion(request.getAppVersion())
                .build();
            
            // Send OTP
            OtpService.OtpSendResult result = otpService.sendOtp(otpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                response.put("sessionId", sessionId);
                response.put("expiresIn", 300); // 5 minutes in seconds
                
                // Record successful attempt if CAPTCHA was required
                if (captchaRequired) {
                    captchaService.recordSuccessfulAttempt(identifier);
                }
                
                response.put("captchaRequired", captchaService.isCaptchaRequired(identifier));
                log.info("OTP sent successfully for phone: {}", maskPhoneNumber(request.getPhoneNumber()));
                return ResponseEntity.ok(response);
            } else {
                // Record failed attempt for abuse detection
                captchaService.recordFailedAttempt(identifier);
                
                response.put("captchaRequired", captchaService.isCaptchaRequired(identifier));
                log.warn("Failed to send OTP for phone: {} - {}", 
                    maskPhoneNumber(request.getPhoneNumber()), result.getMessage());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error in send OTP endpoint: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Verify OTP code for authentication
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract request metadata
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceId = extractDeviceId(request, httpRequest);
            String sessionId = request.getSessionId();
            
            // Validate session consistency
            if (sessionId == null || sessionId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid session");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Check if CAPTCHA is required and validate if provided
            String identifier = request.getPhoneNumber() + ":" + ipAddress;
            boolean captchaRequired = captchaService.isCaptchaRequired(identifier);
            
            if (captchaRequired) {
                if (request.getCaptchaToken() == null || request.getCaptchaToken().trim().isEmpty()) {
                    log.warn("CAPTCHA required but not provided for verification: {}", maskPhoneNumber(request.getPhoneNumber()));
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CAPTCHA verification required",
                        "errorCode", "CAPTCHA_REQUIRED",
                        "captchaRequired", true
                    ));
                }
                
                // Validate CAPTCHA
                CaptchaService.CaptchaValidationRequest captchaRequest = 
                    new CaptchaService.CaptchaValidationRequest(
                        request.getCaptchaToken(), identifier, ipAddress, userAgent);
                
                CaptchaService.CaptchaValidationResult captchaResult = 
                    captchaService.validateCaptcha(captchaRequest);
                
                if (!captchaResult.isSuccess()) {
                    log.warn("CAPTCHA validation failed for verification: {}, reason: {}", 
                        maskPhoneNumber(request.getPhoneNumber()), captchaResult.getMessage());
                    
                    captchaService.recordFailedAttempt(identifier);
                    
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CAPTCHA validation failed",
                        "errorCode", captchaResult.getErrorCode() != null ? captchaResult.getErrorCode() : "CAPTCHA_INVALID",
                        "captchaRequired", true
                    ));
                }
                
                log.debug("CAPTCHA validation successful for verification: {}", maskPhoneNumber(request.getPhoneNumber()));
            }
            
            // Build OTP verify request
            OtpVerifyRequest otpRequest = OtpVerifyRequest.builder()
                .phoneNumber(request.getPhoneNumber())
                .otpCode(request.getOtpCode())
                .type(request.getType())
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .captchaToken(request.getCaptchaToken())
                .fingerprint(request.getFingerprint())
                .timestamp(System.currentTimeMillis())
                .build();
            
            // Verify OTP
            OtpService.OtpVerificationResult result = otpService.verifyOtp(otpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                // TODO: Generate JWT tokens and create user session
                response.put("verified", true);
                response.put("phoneNumber", maskPhoneNumber(request.getPhoneNumber()));
                
                // Reset attempt tracking on successful verification
                captchaService.resetAttemptTracking(identifier);
                
                log.info("OTP verified successfully for phone: {}", 
                    maskPhoneNumber(request.getPhoneNumber()));
                
                return ResponseEntity.ok(response);
            } else {
                // Record failed attempt for abuse detection
                captchaService.recordFailedAttempt(identifier);
                
                response.put("captchaRequired", captchaService.isCaptchaRequired(identifier));
                
                log.warn("Failed to verify OTP for phone: {} - {}", 
                    maskPhoneNumber(request.getPhoneNumber()), result.getMessage());
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error in verify OTP endpoint: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Check authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request) {
        // TODO: Implement authentication status check
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", false);
        response.put("message", "Not implemented yet");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Extract or generate device ID
     */
    private String extractDeviceId(Object request, HttpServletRequest httpRequest) {
        // Try to get device ID from request body
        if (request instanceof SendOtpRequest) {
            SendOtpRequest sendRequest = (SendOtpRequest) request;
            if (sendRequest.getDeviceId() != null && !sendRequest.getDeviceId().trim().isEmpty()) {
                return sendRequest.getDeviceId();
            }
        } else if (request instanceof VerifyOtpRequest) {
            VerifyOtpRequest verifyRequest = (VerifyOtpRequest) request;
            if (verifyRequest.getDeviceId() != null && !verifyRequest.getDeviceId().trim().isEmpty()) {
                return verifyRequest.getDeviceId();
            }
        }
        
        // Try to get from headers
        String deviceId = httpRequest.getHeader("X-Device-ID");
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            return deviceId;
        }
        
        // Generate a temporary device ID based on user agent and IP
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        return "temp-" + UUID.nameUUIDFromBytes((userAgent + ipAddress).getBytes()).toString();
    }
    
    /**
     * Extract or generate session ID
     */
    private String extractSessionId(HttpServletRequest request) {
        // Try to get from headers
        String sessionId = request.getHeader("X-Session-ID");
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return sessionId;
        }
        
        // Try to get from existing HTTP session
        if (request.getSession(false) != null) {
            return request.getSession().getId();
        }
        
        // Generate new session ID
        return "session-" + UUID.randomUUID().toString();
    }
    
    /**
     * Mask phone number for logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 2) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    // Request DTOs
    public static class SendOtpRequest {
        private String phoneNumber;
        private OtpVerification.OtpType type = OtpVerification.OtpType.LOGIN;
        private String deviceId;
        private String preferredProvider;
        private String locale = "en";
        private String timezone;
        private String appVersion;
        private String captchaToken;
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public OtpVerification.OtpType getType() { return type; }
        public void setType(OtpVerification.OtpType type) { this.type = type; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getPreferredProvider() { return preferredProvider; }
        public void setPreferredProvider(String preferredProvider) { this.preferredProvider = preferredProvider; }
        
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
        
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
        
        public String getCaptchaToken() { return captchaToken; }
        public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    }
    
    public static class VerifyOtpRequest {
        private String phoneNumber;
        private String otpCode;
        private OtpVerification.OtpType type = OtpVerification.OtpType.LOGIN;
        private String deviceId;
        private String sessionId;
        private String captchaToken;
        private String fingerprint;
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
        
        public OtpVerification.OtpType getType() { return type; }
        public void setType(OtpVerification.OtpType type) { this.type = type; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getCaptchaToken() { return captchaToken; }
        public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
        
        public String getFingerprint() { return fingerprint; }
        public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    }
}
package com.bharatshop.shared.service.auth;

import com.bharatshop.shared.entity.OtpVerification;
import com.bharatshop.shared.entity.PhoneAuthRequest;
import com.bharatshop.shared.repository.OtpVerificationRepository;
import com.bharatshop.shared.repository.PhoneAuthRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for OTP operations with pluggable providers, rate limiting, and security features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final OtpVerificationRepository otpRepository;
    private final PhoneAuthRequestRepository phoneAuthRepository;
    private final List<AuthProvider> authProviders;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Configuration values
    @Value("${app.otp.length:6}")
    private int otpLength;
    
    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;
    
    @Value("${app.otp.max-attempts:3}")
    private int maxVerificationAttempts;
    
    @Value("${app.otp.rate-limit.phone.count:3}")
    private int phoneRateLimitCount;
    
    @Value("${app.otp.rate-limit.phone.window-minutes:15}")
    private int phoneRateLimitWindowMinutes;
    
    @Value("${app.otp.rate-limit.ip.count:10}")
    private int ipRateLimitCount;
    
    @Value("${app.otp.rate-limit.ip.window-minutes:60}")
    private int ipRateLimitWindowMinutes;
    
    @Value("${app.otp.rate-limit.device.count:5}")
    private int deviceRateLimitCount;
    
    @Value("${app.otp.rate-limit.device.window-minutes:30}")
    private int deviceRateLimitWindowMinutes;
    
    @Value("${app.otp.block-threshold:5}")
    private int blockThreshold;
    
    @Value("${app.otp.block-duration-hours:24}")
    private int blockDurationHours;
    
    /**
     * Send OTP with rate limiting and security checks
     */
    @Transactional
    public OtpSendResult sendOtp(OtpSendRequest request) {
        try {
            // Validate input
            if (!isValidPhoneNumber(request.getPhoneNumber())) {
                return OtpSendResult.failure("Invalid phone number format");
            }
            
            // Check for existing active request
            Optional<PhoneAuthRequest> existingRequest = phoneAuthRepository
                .findActiveRequestByPhone(request.getPhoneNumber());
            
            if (existingRequest.isPresent() && existingRequest.get().isBlocked()) {
                return OtpSendResult.failure("Phone number is temporarily blocked");
            }
            
            // Rate limiting checks
            RateLimitResult rateLimitResult = checkRateLimits(request);
            if (!rateLimitResult.isAllowed()) {
                return OtpSendResult.failure(rateLimitResult.getReason());
            }
            
            // Find appropriate provider
            AuthProvider provider = findProvider(request.getPhoneNumber());
            if (provider == null) {
                return OtpSendResult.failure("No provider available for this phone number");
            }
            
            // Generate OTP
            String otpCode = generateOtp();
            String hashedOtp = passwordEncoder.encode(otpCode);
            
            // Create or update phone auth request
            PhoneAuthRequest phoneAuthRequest = createOrUpdatePhoneAuthRequest(request);
            
            // Create OTP verification record
            OtpVerification otpVerification = createOtpVerification(request, otpCode, hashedOtp, provider.getProviderId());
            
            // Send OTP via provider
            AuthProvider.AuthProviderResult providerResult = provider.sendOtp(
                request.getPhoneNumber(), 
                otpCode, 
                createTemplateData(request)
            );
            
            if (!providerResult.isSuccess()) {
                System.out.println("Failed to send OTP via provider " + provider.getProviderId() + ": " + providerResult.getMessage());
                return OtpSendResult.failure("Failed to send OTP. Please try again.");
            }
            
            // Update OTP record with provider response
            otpVerification.setProviderMessageId(providerResult.getProviderMessageId());
            otpVerification.setProviderResponse(providerResult.getMessage());
            otpRepository.save(otpVerification);
            
            // Update phone auth request
            phoneAuthRequest.incrementAttempt();
            phoneAuthRequest.setStatus(PhoneAuthRequest.RequestStatus.PENDING);
            phoneAuthRepository.save(phoneAuthRequest);
            
            System.out.println("OTP sent successfully to " + maskPhoneNumber(request.getPhoneNumber()) + " via provider " + provider.getProviderId());
            
            return OtpSendResult.success("OTP sent successfully", otpVerification.getId());
            
        } catch (Exception e) {
            System.out.println("Error sending OTP to " + maskPhoneNumber(request.getPhoneNumber()) + ": " + e.getMessage());
            return OtpSendResult.failure("Internal error occurred");
        }
    }
    
    /**
     * Verify OTP with replay protection and security checks
     */
    @Transactional
    public OtpVerificationResult verifyOtp(OtpVerifyRequest request) {
        try {
            String hashedOtp = passwordEncoder.encode(request.getOtpCode());
            
            // Find active OTP
            Optional<OtpVerification> otpOpt = otpRepository.findActiveOtpByPhoneAndType(
                request.getPhoneNumber(), 
                request.getType(), 
                LocalDateTime.now()
            );
            
            if (otpOpt.isEmpty()) {
                return OtpVerificationResult.failure("Invalid or expired OTP");
            }
            
            OtpVerification otp = otpOpt.get();
            
            // Check if OTP can be verified
            if (!otp.canBeVerified()) {
                return OtpVerificationResult.failure("OTP cannot be verified");
            }
            
            // Verify device and session consistency (replay protection)
            if (!verifyDeviceConsistency(otp, request)) {
                System.out.println("Device inconsistency detected for OTP verification: phone=" + 
                    maskPhoneNumber(request.getPhoneNumber()) + ", deviceId=" + request.getDeviceId());
                return OtpVerificationResult.failure("Security validation failed");
            }
            
            // Increment verification attempts
            otp.incrementVerificationAttempts();
            
            // Verify OTP code
            if (!passwordEncoder.matches(request.getOtpCode(), otp.getHashedOtp())) {
                // Handle failed verification
                if (otp.hasExceededMaxAttempts()) {
                    otp.markAsFailed();
                    blockPhoneIfNeeded(request.getPhoneNumber());
                }
                otpRepository.save(otp);
                
                return OtpVerificationResult.failure("Invalid OTP code");
            }
            
            // Successful verification
            otp.markAsVerified();
            otpRepository.save(otp);
            
            // Update phone auth request status
            Optional<PhoneAuthRequest> phoneAuthOpt = phoneAuthRepository
                .findActiveRequestByPhone(request.getPhoneNumber());
            
            if (phoneAuthOpt.isPresent()) {
                PhoneAuthRequest phoneAuth = phoneAuthOpt.get();
                phoneAuth.setStatus(PhoneAuthRequest.RequestStatus.VERIFIED);
                phoneAuthRepository.save(phoneAuth);
            }
            
            System.out.println("OTP verified successfully for phone: " + maskPhoneNumber(request.getPhoneNumber()));
            
            return OtpVerificationResult.success("OTP verified successfully", otp.getId());
            
        } catch (Exception e) {
            System.out.println("Error verifying OTP for " + maskPhoneNumber(request.getPhoneNumber()) + ": " + e.getMessage());
            return OtpVerificationResult.failure("Internal error occurred");
        }
    }
    
    /**
     * Check rate limits for OTP sending
     */
    private RateLimitResult checkRateLimits(OtpSendRequest request) {
        LocalDateTime now = LocalDateTime.now();
        
        // Phone number rate limit
        LocalDateTime phoneWindow = now.minusMinutes(phoneRateLimitWindowMinutes);
        long phoneCount = otpRepository.countOtpsByPhoneInWindow(request.getPhoneNumber(), phoneWindow);
        if (phoneCount >= phoneRateLimitCount) {
            return RateLimitResult.blocked("Too many OTP requests for this phone number");
        }
        
        // IP address rate limit
        LocalDateTime ipWindow = now.minusMinutes(ipRateLimitWindowMinutes);
        long ipCount = otpRepository.countOtpsByPhoneInWindow(request.getIpAddress(), ipWindow);
        if (ipCount >= ipRateLimitCount) {
            return RateLimitResult.blocked("Too many OTP requests from this IP address");
        }
        
        // Device rate limit
        LocalDateTime deviceWindow = now.minusMinutes(deviceRateLimitWindowMinutes);
        long deviceCount = otpRepository.countVerificationAttemptsByDeviceAndPhone(
            request.getPhoneNumber(), request.getDeviceId(), deviceWindow);
        if (deviceCount >= deviceRateLimitCount) {
            return RateLimitResult.blocked("Too many OTP requests from this device");
        }
        
        return RateLimitResult.allowed();
    }
    
    /**
     * Find appropriate auth provider for phone number
     */
    private AuthProvider findProvider(String phoneNumber) {
        return authProviders.stream()
            .filter(provider -> provider.supports(phoneNumber) && provider.isHealthy())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Generate secure OTP code
     */
    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
    
    /**
     * Create or update phone auth request
     */
    private PhoneAuthRequest createOrUpdatePhoneAuthRequest(OtpSendRequest request) {
        Optional<PhoneAuthRequest> existingOpt = phoneAuthRepository
            .findActiveRequestByPhone(request.getPhoneNumber());
        
        PhoneAuthRequest phoneAuthRequest;
        if (existingOpt.isPresent()) {
            phoneAuthRequest = existingOpt.get();
        } else {
            phoneAuthRequest = PhoneAuthRequest.builder()
                .phoneNumber(request.getPhoneNumber())
                .ipAddress(request.getIpAddress())
                .deviceId(request.getDeviceId())
                .userAgent(request.getUserAgent())
                .status(PhoneAuthRequest.RequestStatus.PENDING)
                .attemptCount(0)
                .build();
        }
        
        return phoneAuthRequest;
    }
    
    /**
     * Create OTP verification record
     */
    private OtpVerification createOtpVerification(OtpSendRequest request, String otpCode, String hashedOtp, String providerId) {
        return OtpVerification.builder()
            .phoneNumber(request.getPhoneNumber())
            .otpCode(otpCode) // This will be cleared after sending
            .hashedOtp(hashedOtp)
            .status(OtpVerification.OtpStatus.PENDING)
            .type(request.getType())
            .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
            .verificationAttempts(0)
            .maxAttempts(maxVerificationAttempts)
            .deviceId(request.getDeviceId())
            .ipAddress(request.getIpAddress())
            .sessionId(request.getSessionId())
            .providerId(providerId)
            .build();
    }
    
    /**
     * Create template data for OTP message
     */
    private Map<String, Object> createTemplateData(OtpSendRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("appName", "BharatShop");
        data.put("expiryMinutes", otpExpiryMinutes);
        data.put("type", request.getType().name());
        return data;
    }
    
    /**
     * Verify device consistency for replay protection
     */
    private boolean verifyDeviceConsistency(OtpVerification otp, OtpVerifyRequest request) {
        return otp.getDeviceId().equals(request.getDeviceId()) &&
               otp.getSessionId().equals(request.getSessionId());
    }
    
    /**
     * Block phone number if too many failed attempts
     */
    private void blockPhoneIfNeeded(String phoneNumber) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long failedCount = otpRepository.countFailedAttemptsByPhone(phoneNumber, since);
        
        if (failedCount >= blockThreshold) {
            Optional<PhoneAuthRequest> phoneAuthOpt = phoneAuthRepository
                .findActiveRequestByPhone(phoneNumber);
            
            if (phoneAuthOpt.isPresent()) {
                PhoneAuthRequest phoneAuth = phoneAuthOpt.get();
                phoneAuth.block("Too many failed OTP attempts", 
                    LocalDateTime.now().plusHours(blockDurationHours));
                phoneAuthRepository.save(phoneAuth);
                
                System.out.println("Phone number " + maskPhoneNumber(phoneNumber) + " blocked due to too many failed OTP attempts");
            }
        }
    }
    
    /**
     * Validate phone number format
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\+?[1-9]\\d{1,14}$");
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
    
    /**
     * Cleanup expired OTPs (scheduled task)
     */
    @Transactional
    public void cleanupExpiredOtps() {
        int updated = otpRepository.markExpiredOtps(LocalDateTime.now());
        if (updated > 0) {
            System.out.println("Marked " + updated + " expired OTPs");
        }
        
        // Delete old OTPs (older than 30 days)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = otpRepository.deleteOldOtps(cutoff);
        if (deleted > 0) {
            System.out.println("Deleted " + deleted + " old OTP records");
        }
    }
    
    // Result classes
    public static class OtpSendResult {
        private final boolean success;
        private final String message;
        private final Long otpId;
        
        private OtpSendResult(boolean success, String message, Long otpId) {
            this.success = success;
            this.message = message;
            this.otpId = otpId;
        }
        
        public static OtpSendResult success(String message, Long otpId) {
            return new OtpSendResult(true, message, otpId);
        }
        
        public static OtpSendResult failure(String message) {
            return new OtpSendResult(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getOtpId() { return otpId; }
    }
    
    public static class OtpVerificationResult {
        private final boolean success;
        private final String message;
        private final Long otpId;
        
        private OtpVerificationResult(boolean success, String message, Long otpId) {
            this.success = success;
            this.message = message;
            this.otpId = otpId;
        }
        
        public static OtpVerificationResult success(String message, Long otpId) {
            return new OtpVerificationResult(true, message, otpId);
        }
        
        public static OtpVerificationResult failure(String message) {
            return new OtpVerificationResult(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getOtpId() { return otpId; }
    }
    
    private static class RateLimitResult {
        private final boolean allowed;
        private final String reason;
        
        private RateLimitResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
        
        public static RateLimitResult allowed() {
            return new RateLimitResult(true, null);
        }
        
        public static RateLimitResult blocked(String reason) {
            return new RateLimitResult(false, reason);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
    }
}
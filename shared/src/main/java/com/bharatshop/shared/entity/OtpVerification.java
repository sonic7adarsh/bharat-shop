package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store OTP verification codes with security and replay protection
 */
@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_phone_status_created", columnList = "phoneNumber, status, createdAt"),
    @Index(name = "idx_otp_code", columnList = "otpCode"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @Column(nullable = false)
    @NotBlank(message = "OTP code is required")
    private String otpCode;
    
    @Column(nullable = false)
    private String hashedOtp; // Store hashed version for security
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime verifiedAt;
    
    @Column
    private Integer verificationAttempts;
    
    @Column
    private Integer maxAttempts;
    
    @Column
    private String deviceId;
    
    @Column
    private String ipAddress;
    
    @Column
    private String sessionId;
    
    // Provider information
    @Column
    private String providerId;
    
    @Column
    private String providerMessageId;
    
    @Column
    private String providerResponse;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum OtpStatus {
        PENDING,
        VERIFIED,
        EXPIRED,
        FAILED,
        BLOCKED
    }
    
    public enum OtpType {
        LOGIN,
        REGISTRATION,
        PASSWORD_RESET,
        PHONE_VERIFICATION
    }
    
    // Manual setter methods
    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }
    
    public void setProviderResponse(String providerResponse) {
        this.providerResponse = providerResponse;
    }
    
    // Manual getter methods
    public Long getId() {
        return id;
    }
    
    public String getHashedOtp() {
        return hashedOtp;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public static OtpVerificationBuilder builder() {
        return new OtpVerificationBuilder();
    }
    
    public static class OtpVerificationBuilder {
        private String phoneNumber;
        private String otpCode;
        private String hashedOtp;
        private OtpStatus status;
        private OtpType type;
        private LocalDateTime expiresAt;
        private Integer maxAttempts;
        private String deviceId;
        private String ipAddress;
        private String sessionId;
        private String providerId;
        
        public OtpVerificationBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }
        
        public OtpVerificationBuilder otpCode(String otpCode) {
            this.otpCode = otpCode;
            return this;
        }
        
        public OtpVerificationBuilder hashedOtp(String hashedOtp) {
            this.hashedOtp = hashedOtp;
            return this;
        }
        
        public OtpVerificationBuilder status(OtpStatus status) {
            this.status = status;
            return this;
        }
        
        public OtpVerificationBuilder type(OtpType type) {
            this.type = type;
            return this;
        }
        
        public OtpVerificationBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public OtpVerificationBuilder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public OtpVerificationBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        
        public OtpVerificationBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public OtpVerificationBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public OtpVerificationBuilder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }
        
        public OtpVerificationBuilder verificationAttempts(int verificationAttempts) {
            // This method is needed for builder pattern compatibility
            return this;
        }
        
        public OtpVerification build() {
            OtpVerification otp = new OtpVerification();
            otp.phoneNumber = this.phoneNumber;
            otp.otpCode = this.otpCode;
            otp.hashedOtp = this.hashedOtp;
            otp.status = this.status;
            otp.type = this.type;
            otp.expiresAt = this.expiresAt;
            otp.maxAttempts = this.maxAttempts;
            otp.deviceId = this.deviceId;
            otp.ipAddress = this.ipAddress;
            otp.sessionId = this.sessionId;
            otp.providerId = this.providerId;
            otp.verificationAttempts = 0;
            return otp;
        }
    }
    
    /**
     * Check if OTP is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if OTP can be verified (not expired, not already used, attempts available)
     */
    public boolean canBeVerified() {
        return status == OtpStatus.PENDING && 
               !isExpired() && 
               (verificationAttempts == null || verificationAttempts < maxAttempts);
    }
    
    /**
     * Increment verification attempts
     */
    public void incrementVerificationAttempts() {
        this.verificationAttempts = (this.verificationAttempts == null) ? 1 : this.verificationAttempts + 1;
    }
    
    /**
     * Mark as verified
     */
    public void markAsVerified() {
        this.status = OtpStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as expired
     */
    public void markAsExpired() {
        this.status = OtpStatus.EXPIRED;
    }
    
    /**
     * Mark as failed (too many attempts)
     */
    public void markAsFailed() {
        this.status = OtpStatus.FAILED;
    }
    
    /**
     * Check if max attempts reached
     */
    public boolean hasExceededMaxAttempts() {
        return verificationAttempts != null && maxAttempts != null && 
               verificationAttempts >= maxAttempts;
    }
}
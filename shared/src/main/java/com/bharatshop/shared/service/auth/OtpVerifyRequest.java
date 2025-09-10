package com.bharatshop.shared.service.auth;

import com.bharatshop.shared.entity.OtpVerification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for verifying OTP
 */
@Data
@Builder
public class OtpVerifyRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotBlank(message = "OTP code is required")
    @Size(min = 4, max = 8, message = "OTP code must be between 4 and 8 digits")
    @Pattern(regexp = "^\\d+$", message = "OTP code must contain only digits")
    private String otpCode;
    
    @NotNull(message = "OTP type is required")
    private OtpVerification.OtpType type;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "IP address is required")
    private String ipAddress;
    
    @NotBlank(message = "User agent is required")
    private String userAgent;
    
    private String sessionId;
    
    // Additional security context
    private String captchaToken;
    private String fingerprint;
    private Long timestamp;
}
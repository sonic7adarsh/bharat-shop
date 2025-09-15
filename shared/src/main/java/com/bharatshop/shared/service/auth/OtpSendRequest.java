package com.bharatshop.shared.service.auth;

import com.bharatshop.shared.entity.OtpVerification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for sending OTP
 */
@Data
@Builder
public class OtpSendRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotNull(message = "OTP type is required")
    private OtpVerification.OtpType type;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "IP address is required")
    private String ipAddress;
    
    @NotBlank(message = "User agent is required")
    private String userAgent;
    
    private String sessionId;
    
    private String preferredProviderId;
    
    // Additional context data
    private String locale;
    private String timezone;
    private String appVersion;
    
    // Manual getter methods
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public OtpVerification.OtpType getType() {
        return type;
    }
    
    public String getSessionId() {
        return sessionId;
    }
}
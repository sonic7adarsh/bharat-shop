package com.bharatshop.shared.service.auth;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating refresh tokens
 */
@Data
@Builder
@Jacksonized
public class RefreshTokenRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Device ID is required")
    @Size(min = 1, max = 255, message = "Device ID must be between 1 and 255 characters")
    private String deviceId;
    
    /**
     * Parent token ID for token family tracking (null for initial tokens)
     */
    private Long parentTokenId;
    
    @Size(max = 255, message = "Device name must not exceed 255 characters")
    private String deviceName;
    
    @Size(max = 100, message = "Device type must not exceed 100 characters")
    private String deviceType;
    
    @Size(max = 1000, message = "User agent must not exceed 1000 characters")
    private String userAgent;
    
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
    
    /**
     * Additional context for security tracking
     */
    @Size(max = 100, message = "Session ID must not exceed 100 characters")
    private String sessionId;
    
    @Size(max = 100, message = "App version must not exceed 100 characters")
    private String appVersion;
    
    @Size(max = 10, message = "Locale must not exceed 10 characters")
    private String locale;
    
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;
    
    /**
     * Security fingerprint for additional validation
     */
    @Size(max = 500, message = "Fingerprint must not exceed 500 characters")
    private String fingerprint;
    
    /**
     * Check if this is an initial token (no parent)
     */
    public boolean isInitialToken() {
        return parentTokenId == null;
    }
    
    /**
     * Check if device information is complete
     */
    public boolean hasCompleteDeviceInfo() {
        return deviceName != null && !deviceName.trim().isEmpty() &&
               deviceType != null && !deviceType.trim().isEmpty();
    }
    
    /**
     * Check if location tracking is enabled
     */
    public boolean hasLocationInfo() {
        return location != null && !location.trim().isEmpty();
    }
    
    /**
     * Get masked device ID for logging
     */
    public String getMaskedDeviceId() {
        if (deviceId == null || deviceId.length() <= 8) {
            return "****";
        }
        return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
    }
    
    /**
     * Get masked IP address for logging
     */
    public String getMaskedIpAddress() {
        if (ipAddress == null) {
            return "unknown";
        }
        
        // IPv4 masking
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.**";
            }
        }
        
        // IPv6 or other format masking
        if (ipAddress.length() > 8) {
            return ipAddress.substring(0, 4) + "****";
        }
        
        return "****";
    }
    
    // Manual getters since Lombok is not working properly
    public Long getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public Long getParentTokenId() { return parentTokenId; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceType() { return deviceType; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
    public String getLocation() { return location; }
    

}
package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to manage device-based refresh tokens with reuse detection and security features
 */
@Entity
@Table(name = "device_refresh_tokens", indexes = {
    @Index(name = "idx_user_device", columnList = "userId, deviceId"),
    @Index(name = "idx_token_hash", columnList = "tokenHash"),
    @Index(name = "idx_parent_token", columnList = "parentTokenId"),
    @Index(name = "idx_expires_at", columnList = "expiresAt"),
    @Index(name = "idx_device_last_used", columnList = "deviceId, lastUsedAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Column(nullable = false)
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Token hash is required")
    private String tokenHash; // SHA-256 hash of the actual token
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private LocalDateTime lastUsedAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;
    
    @Column
    private LocalDateTime revokedAt;
    
    @Column
    private String revokedReason;
    
    // For token family tracking (reuse detection)
    @Column
    private Long parentTokenId;
    
    @Column
    @Builder.Default
    private Integer generationNumber = 1;
    
    // Device information for security
    @Column
    private String deviceName;
    
    @Column
    private String deviceType; // mobile, desktop, tablet, etc.
    
    @Column
    private String userAgent;
    
    @Column
    private String ipAddress;
    
    @Column
    private String location; // City, Country based on IP
    
    // Security flags
    @Column
    @Builder.Default
    private Boolean suspicious = false;
    
    @Column
    @Builder.Default
    private Boolean reused = false;
    
    @Column
    private LocalDateTime reuseDetectedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if token is valid (not revoked, not expired, not reused)
     */
    public boolean isValid() {
        return !revoked && !isExpired() && !reused;
    }
    
    /**
     * Revoke this token with reason
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }
    
    /**
     * Mark as reused (for reuse detection)
     */
    public void markAsReused() {
        this.reused = true;
        this.reuseDetectedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as suspicious
     */
    public void markAsSuspicious() {
        this.suspicious = true;
    }
    
    /**
     * Update last used timestamp
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * Check if token belongs to the same device family
     */
    public boolean isSameDeviceFamily(String deviceId) {
        return this.deviceId.equals(deviceId);
    }
    
    /**
     * Get token age in hours
     */
    public long getAgeInHours() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }
    
    /**
     * Check if token is stale (not used for a long time)
     */
    public boolean isStale(int staleHours) {
        return java.time.Duration.between(lastUsedAt, LocalDateTime.now()).toHours() > staleHours;
    }
    
    // Manual methods in case Lombok isn't working
    public static DeviceRefreshTokenBuilder builder() {
        return new DeviceRefreshTokenBuilder();
    }
    
    public Long getId() {
        return this.id;
    }
    
    public String getDeviceId() {
        return this.deviceId;
    }
    
    public Long getUserId() {
        return this.userId;
    }
    
    public String getDeviceName() {
        return this.deviceName;
    }
    
    public String getDeviceType() {
        return this.deviceType;
    }
    
    public String getUserAgent() {
        return this.userAgent;
    }
    
    public String getLocation() {
        return this.location;
    }
    
    public Boolean getRevoked() {
        return revoked;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getGenerationNumber() {
        return generationNumber;
    }
    
    public static class DeviceRefreshTokenBuilder {
        private Long id;
        private Long userId;
        private String deviceId;
        private String tokenHash;
        private LocalDateTime expiresAt;
        private LocalDateTime lastUsedAt;
        private Boolean revoked = false;
        private LocalDateTime revokedAt;
        private String revokedReason;
        private Long parentTokenId;
        private Integer generationNumber = 1;
        private String deviceName;
        private String deviceType;
        private String userAgent;
        private String ipAddress;
        private String location;
        private Boolean suspicious = false;
        private Boolean reused = false;
        private LocalDateTime reuseDetectedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public DeviceRefreshTokenBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public DeviceRefreshTokenBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public DeviceRefreshTokenBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        
        public DeviceRefreshTokenBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }
        
        public DeviceRefreshTokenBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public DeviceRefreshTokenBuilder lastUsedAt(LocalDateTime lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }
        
        public DeviceRefreshTokenBuilder revoked(Boolean revoked) {
            this.revoked = revoked;
            return this;
        }
        
        public DeviceRefreshTokenBuilder revokedAt(LocalDateTime revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }
        
        public DeviceRefreshTokenBuilder revokedReason(String revokedReason) {
            this.revokedReason = revokedReason;
            return this;
        }
        
        public DeviceRefreshTokenBuilder parentTokenId(Long parentTokenId) {
            this.parentTokenId = parentTokenId;
            return this;
        }
        
        public DeviceRefreshTokenBuilder generationNumber(Integer generationNumber) {
            this.generationNumber = generationNumber;
            return this;
        }
        
        public DeviceRefreshTokenBuilder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }
        
        public DeviceRefreshTokenBuilder deviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }
        
        public DeviceRefreshTokenBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public DeviceRefreshTokenBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public DeviceRefreshTokenBuilder location(String location) {
            this.location = location;
            return this;
        }
        
        public DeviceRefreshTokenBuilder suspicious(Boolean suspicious) {
            this.suspicious = suspicious;
            return this;
        }
        
        public DeviceRefreshTokenBuilder reused(Boolean reused) {
            this.reused = reused;
            return this;
        }
        
        public DeviceRefreshTokenBuilder reuseDetectedAt(LocalDateTime reuseDetectedAt) {
            this.reuseDetectedAt = reuseDetectedAt;
            return this;
        }
        
        public DeviceRefreshTokenBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public DeviceRefreshTokenBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public DeviceRefreshToken build() {
            DeviceRefreshToken token = new DeviceRefreshToken();
            token.id = this.id;
            token.userId = this.userId;
            token.deviceId = this.deviceId;
            token.tokenHash = this.tokenHash;
            token.expiresAt = this.expiresAt;
            token.lastUsedAt = this.lastUsedAt;
            token.revoked = this.revoked != null ? this.revoked : false;
            token.revokedAt = this.revokedAt;
            token.revokedReason = this.revokedReason;
            token.parentTokenId = this.parentTokenId;
            token.generationNumber = this.generationNumber != null ? this.generationNumber : 1;
            token.deviceName = this.deviceName;
            token.deviceType = this.deviceType;
            token.userAgent = this.userAgent;
            token.ipAddress = this.ipAddress;
            token.location = this.location;
            token.suspicious = this.suspicious != null ? this.suspicious : false;
            token.reused = this.reused != null ? this.reused : false;
            token.reuseDetectedAt = this.reuseDetectedAt;
            token.createdAt = this.createdAt;
            token.updatedAt = this.updatedAt;
            return token;
        }
    }
}
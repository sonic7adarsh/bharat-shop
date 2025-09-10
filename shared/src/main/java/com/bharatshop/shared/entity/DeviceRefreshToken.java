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
}
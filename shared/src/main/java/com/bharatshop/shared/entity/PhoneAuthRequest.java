package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track phone authentication requests with rate limiting and security features
 */
@Entity
@Table(name = "phone_auth_requests", indexes = {
    @Index(name = "idx_phone_created_at", columnList = "phoneNumber, createdAt"),
    @Index(name = "idx_ip_created_at", columnList = "ipAddress, createdAt"),
    @Index(name = "idx_device_created_at", columnList = "deviceId, createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneAuthRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(nullable = false)
    private String deviceId;
    
    @Column(nullable = false)
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;
    
    @Column(nullable = false)
    private Integer attemptCount;
    
    @Column
    private LocalDateTime lastAttemptAt;
    
    @Column
    private LocalDateTime blockedUntil;
    
    @Column
    private String blockReason;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum RequestStatus {
        PENDING,
        VERIFIED,
        EXPIRED,
        BLOCKED,
        RATE_LIMITED
    }
    
    /**
     * Check if this request is currently blocked
     */
    public boolean isBlocked() {
        return status == RequestStatus.BLOCKED || 
               (blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil));
    }
    
    /**
     * Check if this request has exceeded rate limits
     */
    public boolean isRateLimited() {
        return status == RequestStatus.RATE_LIMITED;
    }
    
    /**
     * Increment attempt count and update last attempt time
     */
    public void incrementAttempt() {
        this.attemptCount = (this.attemptCount == null) ? 1 : this.attemptCount + 1;
        this.lastAttemptAt = LocalDateTime.now();
    }
    
    /**
     * Block this request with reason
     */
    public void block(String reason, LocalDateTime until) {
        this.status = RequestStatus.BLOCKED;
        this.blockReason = reason;
        this.blockedUntil = until;
    }
}
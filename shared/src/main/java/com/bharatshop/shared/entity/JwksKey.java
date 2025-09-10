package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a JWT signing key in the JWKS (JSON Web Key Set)
 * Supports key rotation and rolling upgrade scenarios
 */
@Entity
@Table(name = "jwks_keys", indexes = {
    @Index(name = "idx_jwks_kid", columnList = "kid", unique = true),
    @Index(name = "idx_jwks_active", columnList = "active"),
    @Index(name = "idx_jwks_rotated_at", columnList = "rotatedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwksKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Key ID - unique identifier for this key
     * Used in JWT header to identify which key was used for signing
     */
    @Column(nullable = false, unique = true, length = 64)
    private String kid;
    
    /**
     * Algorithm used with this key (e.g., "HS256", "RS256")
     */
    @Column(nullable = false, length = 10)
    private String alg;
    
    /**
     * The actual key material (base64 encoded)
     * For HMAC: the secret key
     * For RSA: the private key (PEM format)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String keyMaterial;
    
    /**
     * Public key material for asymmetric algorithms (base64 encoded PEM)
     * For RSA: the public key
     * For HMAC: null (symmetric key)
     */
    @Column(columnDefinition = "TEXT")
    private String publicKeyMaterial;
    
    /**
     * Whether this key is currently active for signing new tokens
     * Only one key should be active at a time for signing
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;
    
    /**
     * Timestamp when this key was rotated (became inactive)
     * Used to determine the rolling upgrade window
     */
    @Column
    private LocalDateTime rotatedAt;
    
    /**
     * Timestamp when this key expires and should no longer be used for validation
     * Allows for graceful key retirement after rolling upgrade window
     */
    @Column
    private LocalDateTime expiresAt;
    
    /**
     * Key usage type
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KeyUsage usage = KeyUsage.SIGNING;
    
    /**
     * Key size in bits (for informational purposes)
     */
    @Column
    private Integer keySize;
    
    /**
     * Description or notes about this key
     */
    @Column(length = 500)
    private String description;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Key usage enumeration
     */
    public enum KeyUsage {
        SIGNING,      // Used for signing tokens
        VERIFICATION, // Used only for verification (rotated keys)
        RETIRED       // No longer used, kept for audit purposes
    }
    
    /**
     * Check if this key is currently valid for token verification
     * A key is valid if it's not expired
     */
    public boolean isValidForVerification() {
        return expiresAt == null || LocalDateTime.now().isBefore(expiresAt);
    }
    
    /**
     * Check if this key is in the rolling upgrade window
     * Keys in this window can still be used for verification but not signing
     */
    public boolean isInRollingUpgradeWindow() {
        return rotatedAt != null && isValidForVerification() && !active;
    }
    
    /**
     * Mark this key as rotated (inactive)
     */
    public void rotate() {
        this.active = false;
        this.rotatedAt = LocalDateTime.now();
        this.usage = KeyUsage.VERIFICATION;
    }
    
    /**
     * Mark this key as retired
     */
    public void retire() {
        this.active = false;
        this.usage = KeyUsage.RETIRED;
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now();
        }
    }
}
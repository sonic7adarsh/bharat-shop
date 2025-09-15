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
    
    // Manual builder method for compilation compatibility
    public static JwksKeyBuilder builder() {
        return new JwksKeyBuilder();
    }
    
    public static class JwksKeyBuilder {
        private Long id;
        private String kid;
        private String alg;
        private String keyMaterial;
        private Boolean active = false;
        private LocalDateTime rotatedAt;
        private LocalDateTime expiresAt;
        private KeyUsage usage = KeyUsage.SIGNING;
        private Integer keySize;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public JwksKeyBuilder id(Long id) { this.id = id; return this; }
        public JwksKeyBuilder kid(String kid) { this.kid = kid; return this; }
        public JwksKeyBuilder alg(String alg) { this.alg = alg; return this; }
        public JwksKeyBuilder keyMaterial(String keyMaterial) { this.keyMaterial = keyMaterial; return this; }
        public JwksKeyBuilder active(Boolean active) { this.active = active; return this; }
        public JwksKeyBuilder rotatedAt(LocalDateTime rotatedAt) { this.rotatedAt = rotatedAt; return this; }
        public JwksKeyBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public JwksKeyBuilder usage(KeyUsage usage) { this.usage = usage; return this; }
        public JwksKeyBuilder keySize(Integer keySize) { this.keySize = keySize; return this; }
        public JwksKeyBuilder description(String description) { this.description = description; return this; }
        public JwksKeyBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public JwksKeyBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public JwksKey build() {
            JwksKey key = new JwksKey();
            key.id = this.id;
            key.kid = this.kid;
            key.alg = this.alg;
            key.keyMaterial = this.keyMaterial;
            key.active = this.active;
            key.rotatedAt = this.rotatedAt;
            key.expiresAt = this.expiresAt;
            key.usage = this.usage;
            key.keySize = this.keySize;
            key.description = this.description;
            key.createdAt = this.createdAt;
            key.updatedAt = this.updatedAt;
            return key;
        }
    }
    
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
    
    // Manual getter methods (in case Lombok is not working)
    public String getKid() {
        return kid;
    }
    
    public String getAlg() {
        return alg;
    }
    
    public String getKeyMaterial() {
        return keyMaterial;
    }
    
    public Integer getKeySize() {
        return keySize;
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
    
    // Manual getter methods for compilation compatibility
    public Boolean getActive() {
        return active;
    }
    
    public LocalDateTime getRotatedAt() {
        return rotatedAt;
    }
}
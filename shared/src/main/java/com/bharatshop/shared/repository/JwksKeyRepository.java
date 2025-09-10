package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.JwksKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for JWKS key management
 * Supports key rotation, validation, and JWKS endpoint operations
 */
@Repository
public interface JwksKeyRepository extends JpaRepository<JwksKey, Long> {
    
    /**
     * Find the currently active signing key
     * There should only be one active key at any time
     */
    @Query("SELECT k FROM JwksKey k WHERE k.active = true AND k.usage = 'SIGNING'")
    Optional<JwksKey> findActiveSigningKey();
    
    /**
     * Find a key by its key ID (kid)
     */
    Optional<JwksKey> findByKid(String kid);
    
    /**
     * Find all keys that are valid for verification
     * Includes active keys and keys in rolling upgrade window
     */
    @Query("SELECT k FROM JwksKey k WHERE k.active = true OR " +
           "(k.rotatedAt IS NOT NULL AND (k.expiresAt IS NULL OR k.expiresAt > :now))")
    List<JwksKey> findValidVerificationKeys(@Param("now") LocalDateTime now);
    
    /**
     * Find all keys that should be included in JWKS endpoint
     * Includes active keys and keys in rolling upgrade window
     */
    @Query("SELECT k FROM JwksKey k WHERE k.active = true OR " +
           "(k.usage = 'VERIFICATION' AND (k.expiresAt IS NULL OR k.expiresAt > :now)) " +
           "ORDER BY k.active DESC, k.createdAt DESC")
    List<JwksKey> findJwksKeys(@Param("now") LocalDateTime now);
    
    /**
     * Find keys that are expired and can be cleaned up
     */
    @Query("SELECT k FROM JwksKey k WHERE k.expiresAt IS NOT NULL AND k.expiresAt < :now")
    List<JwksKey> findExpiredKeys(@Param("now") LocalDateTime now);
    
    /**
     * Find keys in rolling upgrade window (rotated but not expired)
     */
    @Query("SELECT k FROM JwksKey k WHERE k.rotatedAt IS NOT NULL AND k.active = false AND " +
           "(k.expiresAt IS NULL OR k.expiresAt > :now)")
    List<JwksKey> findKeysInRollingUpgradeWindow(@Param("now") LocalDateTime now);
    
    /**
     * Deactivate all currently active keys
     * Used during key rotation to ensure only one active key
     */
    @Modifying
    @Query("UPDATE JwksKey k SET k.active = false, k.rotatedAt = :rotatedAt, k.usage = 'VERIFICATION' " +
           "WHERE k.active = true")
    int deactivateAllKeys(@Param("rotatedAt") LocalDateTime rotatedAt);
    
    /**
     * Set expiration time for keys rotated before a certain date
     * Used to clean up old keys after rolling upgrade window
     */
    @Modifying
    @Query("UPDATE JwksKey k SET k.expiresAt = :expiresAt, k.usage = 'RETIRED' " +
           "WHERE k.rotatedAt IS NOT NULL AND k.rotatedAt < :rotatedBefore AND k.expiresAt IS NULL")
    int expireOldRotatedKeys(@Param("expiresAt") LocalDateTime expiresAt, 
                            @Param("rotatedBefore") LocalDateTime rotatedBefore);
    
    /**
     * Count active signing keys (should always be 0 or 1)
     */
    @Query("SELECT COUNT(k) FROM JwksKey k WHERE k.active = true AND k.usage = 'SIGNING'")
    long countActiveSigningKeys();
    
    /**
     * Find keys by algorithm
     */
    List<JwksKey> findByAlgAndActiveTrue(String alg);
    
    /**
     * Find all keys ordered by creation date (newest first)
     */
    List<JwksKey> findAllByOrderByCreatedAtDesc();
    
    /**
     * Check if a key with the given kid already exists
     */
    boolean existsByKid(String kid);
    
    /**
     * Delete expired keys (cleanup operation)
     */
    @Modifying
    @Query("DELETE FROM JwksKey k WHERE k.expiresAt IS NOT NULL AND k.expiresAt < :now")
    int deleteExpiredKeys(@Param("now") LocalDateTime now);
    
    /**
     * Find keys created within a time range
     */
    @Query("SELECT k FROM JwksKey k WHERE k.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY k.createdAt DESC")
    List<JwksKey> findKeysCreatedBetween(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);
}
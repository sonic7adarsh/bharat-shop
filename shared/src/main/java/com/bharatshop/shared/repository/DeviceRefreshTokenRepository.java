package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.DeviceRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DeviceRefreshToken entity with security-focused queries
 */
@Repository
public interface DeviceRefreshTokenRepository extends JpaRepository<DeviceRefreshToken, Long> {
    
    /**
     * Find valid token by hash
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND t.reused = false AND t.expiresAt > :now")
    Optional<DeviceRefreshToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    /**
     * Find all tokens for a user and device
     */
    List<DeviceRefreshToken> findByUserIdAndDeviceIdOrderByCreatedAtDesc(Long userId, String deviceId);
    
    /**
     * Find all active tokens for a user
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE t.userId = :userId AND t.revoked = false AND t.expiresAt > :now")
    List<DeviceRefreshToken> findActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Find tokens in the same family (for reuse detection)
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE (t.id = :tokenId OR t.parentTokenId = :tokenId) AND t.userId = :userId")
    List<DeviceRefreshToken> findTokenFamily(@Param("tokenId") Long tokenId, @Param("userId") Long userId);
    
    /**
     * Find child tokens (tokens created from this token)
     */
    List<DeviceRefreshToken> findByParentTokenIdOrderByCreatedAtDesc(Long parentTokenId);
    
    /**
     * Revoke all tokens in a family (for reuse attack mitigation)
     */
    @Modifying
    @Query("UPDATE DeviceRefreshToken t SET t.revoked = true, t.revokedAt = :revokedAt, t.revokedReason = :reason WHERE (t.id = :tokenId OR t.parentTokenId = :tokenId) AND t.userId = :userId")
    int revokeTokenFamily(@Param("tokenId") Long tokenId, @Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    /**
     * Revoke all tokens for a user and device
     */
    @Modifying
    @Query("UPDATE DeviceRefreshToken t SET t.revoked = true, t.revokedAt = :revokedAt, t.revokedReason = :reason WHERE t.userId = :userId AND t.deviceId = :deviceId")
    int revokeAllUserDeviceTokens(@Param("userId") Long userId, @Param("deviceId") String deviceId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE DeviceRefreshToken t SET t.revoked = true, t.revokedAt = :revokedAt, t.revokedReason = :reason WHERE t.userId = :userId")
    int revokeAllUserTokens(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    /**
     * Find expired tokens for cleanup
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE t.expiresAt < :now")
    List<DeviceRefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Find stale tokens (not used for a long time)
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE t.lastUsedAt < :staleTime AND t.revoked = false")
    List<DeviceRefreshToken> findStaleTokens(@Param("staleTime") LocalDateTime staleTime);
    
    /**
     * Count active tokens for a user
     */
    @Query("SELECT COUNT(t) FROM DeviceRefreshToken t WHERE t.userId = :userId AND t.revoked = false AND t.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Count active tokens for a user and device
     */
    @Query("SELECT COUNT(t) FROM DeviceRefreshToken t WHERE t.userId = :userId AND t.deviceId = :deviceId AND t.revoked = false AND t.expiresAt > :now")
    long countActiveTokensByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId, @Param("now") LocalDateTime now);
    
    /**
     * Find suspicious tokens (marked as suspicious or reused)
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE t.userId = :userId AND (t.suspicious = true OR t.reused = true)")
    List<DeviceRefreshToken> findSuspiciousTokensByUserId(@Param("userId") Long userId);
    
    /**
     * Delete expired tokens (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM DeviceRefreshToken t WHERE t.expiresAt < :cutoffTime")
    int deleteExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find tokens by IP address (for security analysis)
     */
    List<DeviceRefreshToken> findByIpAddressAndCreatedAtAfterOrderByCreatedAtDesc(String ipAddress, LocalDateTime after);
    
    /**
     * Find recent tokens for a device (for anomaly detection)
     */
    @Query("SELECT t FROM DeviceRefreshToken t WHERE t.deviceId = :deviceId AND t.createdAt > :after ORDER BY t.createdAt DESC")
    List<DeviceRefreshToken> findRecentTokensByDevice(@Param("deviceId") String deviceId, @Param("after") LocalDateTime after);
}
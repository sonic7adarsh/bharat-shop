package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OtpVerification entity with security and cleanup queries
 */
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    
    /**
     * Find active OTP by phone number and type
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.type = :type AND o.status = 'PENDING' AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findActiveOtpByPhoneAndType(@Param("phoneNumber") String phoneNumber, @Param("type") OtpVerification.OtpType type, @Param("now") LocalDateTime now);
    
    /**
     * Find OTP by hashed code for verification
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.hashedOtp = :hashedOtp AND o.status = 'PENDING' AND o.expiresAt > :now")
    Optional<OtpVerification> findByHashedOtp(@Param("hashedOtp") String hashedOtp, @Param("now") LocalDateTime now);
    
    /**
     * Find recent OTPs by phone number for rate limiting
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.createdAt > :since ORDER BY o.createdAt DESC")
    List<OtpVerification> findRecentOtpsByPhone(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);
    
    /**
     * Find recent OTPs by IP address for rate limiting
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.ipAddress = :ipAddress AND o.createdAt > :since ORDER BY o.createdAt DESC")
    List<OtpVerification> findRecentOtpsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    /**
     * Find recent OTPs by device ID for rate limiting
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.deviceId = :deviceId AND o.createdAt > :since ORDER BY o.createdAt DESC")
    List<OtpVerification> findRecentOtpsByDevice(@Param("deviceId") String deviceId, @Param("since") LocalDateTime since);
    
    /**
     * Count OTPs by phone number in time window
     */
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.createdAt > :since")
    long countOtpsByPhoneInWindow(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);
    
    /**
     * Count failed verification attempts by phone number
     */
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.status = 'FAILED' AND o.createdAt > :since")
    long countFailedAttemptsByPhone(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);
    
    /**
     * Find expired OTPs for cleanup
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.expiresAt < :now AND o.status = 'PENDING'")
    List<OtpVerification> findExpiredOtps(@Param("now") LocalDateTime now);
    
    /**
     * Mark expired OTPs as expired
     */
    @Modifying
    @Query("UPDATE OtpVerification o SET o.status = 'EXPIRED' WHERE o.expiresAt < :now AND o.status = 'PENDING'")
    int markExpiredOtps(@Param("now") LocalDateTime now);
    
    /**
     * Find OTPs by session ID (for session-based validation)
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.sessionId = :sessionId AND o.status = 'PENDING' AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    List<OtpVerification> findActiveOtpsBySession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    /**
     * Find successful verifications by phone number
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.status = 'VERIFIED' ORDER BY o.verifiedAt DESC")
    List<OtpVerification> findSuccessfulVerificationsByPhone(@Param("phoneNumber") String phoneNumber);
    
    /**
     * Delete old OTPs for cleanup (older than specified time)
     */
    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.createdAt < :cutoffTime")
    int deleteOldOtps(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find OTPs by provider for monitoring
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.providerId = :providerId AND o.createdAt > :since ORDER BY o.createdAt DESC")
    List<OtpVerification> findOtpsByProvider(@Param("providerId") String providerId, @Param("since") LocalDateTime since);
    
    /**
     * Count verification attempts by device and phone
     */
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.deviceId = :deviceId AND o.createdAt > :since")
    long countVerificationAttemptsByDeviceAndPhone(@Param("phoneNumber") String phoneNumber, @Param("deviceId") String deviceId, @Param("since") LocalDateTime since);
    
    /**
     * Find blocked OTPs (too many failed attempts)
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.status = 'BLOCKED' ORDER BY o.createdAt DESC")
    List<OtpVerification> findBlockedOtps();
}
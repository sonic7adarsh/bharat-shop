package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.PhoneAuthRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PhoneAuthRequest entity with rate limiting queries
 */
@Repository
public interface PhoneAuthRequestRepository extends JpaRepository<PhoneAuthRequest, Long> {
    
    /**
     * Find recent requests by phone number for rate limiting
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.phoneNumber = :phoneNumber AND r.createdAt > :since ORDER BY r.createdAt DESC")
    List<PhoneAuthRequest> findRecentRequestsByPhone(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);
    
    /**
     * Find recent requests by IP address for rate limiting
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.ipAddress = :ipAddress AND r.createdAt > :since ORDER BY r.createdAt DESC")
    List<PhoneAuthRequest> findRecentRequestsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    /**
     * Find recent requests by device ID for rate limiting
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.deviceId = :deviceId AND r.createdAt > :since ORDER BY r.createdAt DESC")
    List<PhoneAuthRequest> findRecentRequestsByDevice(@Param("deviceId") String deviceId, @Param("since") LocalDateTime since);
    
    /**
     * Find active request for phone number
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.phoneNumber = :phoneNumber AND r.status IN ('PENDING', 'RATE_LIMITED') ORDER BY r.createdAt DESC")
    Optional<PhoneAuthRequest> findActiveRequestByPhone(@Param("phoneNumber") String phoneNumber);
    
    /**
     * Count requests by phone number in time window
     */
    @Query("SELECT COUNT(r) FROM PhoneAuthRequest r WHERE r.phoneNumber = :phoneNumber AND r.createdAt > :since")
    long countRequestsByPhoneInWindow(@Param("phoneNumber") String phoneNumber, @Param("since") LocalDateTime since);
    
    /**
     * Count requests by IP address in time window
     */
    @Query("SELECT COUNT(r) FROM PhoneAuthRequest r WHERE r.ipAddress = :ipAddress AND r.createdAt > :since")
    long countRequestsByIpInWindow(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    /**
     * Count requests by device ID in time window
     */
    @Query("SELECT COUNT(r) FROM PhoneAuthRequest r WHERE r.deviceId = :deviceId AND r.createdAt > :since")
    long countRequestsByDeviceInWindow(@Param("deviceId") String deviceId, @Param("since") LocalDateTime since);
    
    /**
     * Find blocked requests
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.status = 'BLOCKED' AND (r.blockedUntil IS NULL OR r.blockedUntil > :now)")
    List<PhoneAuthRequest> findBlockedRequests(@Param("now") LocalDateTime now);
    
    /**
     * Find requests that can be unblocked
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.status = 'BLOCKED' AND r.blockedUntil IS NOT NULL AND r.blockedUntil <= :now")
    List<PhoneAuthRequest> findRequestsToUnblock(@Param("now") LocalDateTime now);
    
    /**
     * Find suspicious activity (multiple failed attempts)
     */
    @Query("SELECT r FROM PhoneAuthRequest r WHERE r.phoneNumber = :phoneNumber AND r.attemptCount >= :threshold AND r.createdAt > :since")
    List<PhoneAuthRequest> findSuspiciousActivity(@Param("phoneNumber") String phoneNumber, @Param("threshold") Integer threshold, @Param("since") LocalDateTime since);
}
package com.bharatshop.shared.service.auth;

import com.bharatshop.shared.entity.DeviceRefreshToken;
import com.bharatshop.shared.repository.DeviceRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing device-based refresh tokens with reuse detection and security features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final DeviceRefreshTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${app.refresh-token.expiry-days:30}")
    private int refreshTokenExpiryDays;
    
    @Value("${app.refresh-token.max-tokens-per-user:10}")
    private int maxTokensPerUser;
    
    @Value("${app.refresh-token.max-tokens-per-device:3}")
    private int maxTokensPerDevice;
    
    @Value("${app.refresh-token.stale-hours:168}") // 7 days
    private int staleHours;
    
    @Value("${app.refresh-token.cleanup-days:90}")
    private int cleanupDays;
    
    /**
     * Create a new refresh token for a user and device
     */
    @Transactional
    public RefreshTokenResult createRefreshToken(RefreshTokenRequest request) {
        try {
            // Validate input
            if (request.getUserId() == null || request.getDeviceId() == null) {
                return RefreshTokenResult.failure("User ID and Device ID are required");
            }
            
            // Check token limits
            TokenLimitResult limitResult = checkTokenLimits(request.getUserId(), request.getDeviceId());
            if (!limitResult.isAllowed()) {
                return RefreshTokenResult.failure(limitResult.getReason());
            }
            
            // Generate token
            String rawToken = generateSecureToken();
            String tokenHash = passwordEncoder.encode(rawToken);
            
            // Create token entity
            DeviceRefreshToken token = DeviceRefreshToken.builder()
                .userId(request.getUserId())
                .deviceId(request.getDeviceId())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .lastUsedAt(LocalDateTime.now())
                .revoked(false)
                .parentTokenId(request.getParentTokenId())
                .generationNumber(calculateGenerationNumber(request.getParentTokenId()))
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .userAgent(request.getUserAgent())
                .ipAddress(request.getIpAddress())
                .location(request.getLocation())
                .suspicious(false)
                .reused(false)
                .build();
            
            // Save token
            DeviceRefreshToken savedToken = tokenRepository.save(token);
            
            log.info("Created refresh token for user {} on device {}", 
                request.getUserId(), request.getDeviceId());
            
            return RefreshTokenResult.success("Refresh token created", savedToken.getId(), rawToken);
            
        } catch (Exception e) {
            log.error("Error creating refresh token for user {}: {}", 
                request.getUserId(), e.getMessage(), e);
            return RefreshTokenResult.failure("Internal error occurred");
        }
    }
    
    /**
     * Refresh access token using refresh token with reuse detection
     */
    @Transactional
    public RefreshTokenResult refreshAccessToken(String refreshToken, String deviceId, String ipAddress) {
        try {
            String tokenHash = passwordEncoder.encode(refreshToken);
            
            // Find valid token
            Optional<DeviceRefreshToken> tokenOpt = tokenRepository
                .findValidTokenByHash(tokenHash, LocalDateTime.now());
            
            if (tokenOpt.isEmpty()) {
                // Check if token exists but is invalid (potential reuse attack)
                return handlePotentialReuseAttack(refreshToken, deviceId, ipAddress);
            }
            
            DeviceRefreshToken token = tokenOpt.get();
            
            // Verify device consistency
            if (!token.isSameDeviceFamily(deviceId)) {
                log.warn("Device mismatch detected for token refresh: expected={}, actual={}", 
                    token.getDeviceId(), deviceId);
                
                // Mark as suspicious and revoke
                token.markAsSuspicious();
                token.revoke("Device mismatch detected");
                tokenRepository.save(token);
                
                return RefreshTokenResult.failure("Invalid device for token");
            }
            
            // Check for suspicious activity
            if (isSuspiciousActivity(token, ipAddress)) {
                log.warn("Suspicious activity detected for token refresh: userId={}, deviceId={}", 
                    token.getUserId(), token.getDeviceId());
                
                token.markAsSuspicious();
                tokenRepository.save(token);
                
                return RefreshTokenResult.failure("Suspicious activity detected");
            }
            
            // Mark current token as used (for reuse detection)
            token.markAsReused();
            token.updateLastUsed();
            tokenRepository.save(token);
            
            // Create new token in the same family
            RefreshTokenRequest newTokenRequest = RefreshTokenRequest.builder()
                .userId(token.getUserId())
                .deviceId(token.getDeviceId())
                .parentTokenId(token.getId())
                .deviceName(token.getDeviceName())
                .deviceType(token.getDeviceType())
                .userAgent(token.getUserAgent())
                .ipAddress(ipAddress)
                .location(token.getLocation())
                .build();
            
            RefreshTokenResult newTokenResult = createRefreshToken(newTokenRequest);
            
            if (newTokenResult.isSuccess()) {
                log.info("Successfully refreshed token for user {} on device {}", 
                    token.getUserId(), token.getDeviceId());
            }
            
            return newTokenResult;
            
        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage(), e);
            return RefreshTokenResult.failure("Internal error occurred");
        }
    }
    
    /**
     * Handle potential reuse attack by checking token history
     */
    private RefreshTokenResult handlePotentialReuseAttack(String refreshToken, String deviceId, String ipAddress) {
        // Try to find any token (including revoked/reused) that matches
        // This is a simplified approach - in production, you might want to store token hashes differently
        
        log.warn("Potential token reuse attack detected from device {} and IP {}", deviceId, ipAddress);
        
        // Find recent tokens for this device to check for attack patterns
        LocalDateTime recentWindow = LocalDateTime.now().minusHours(24);
        List<DeviceRefreshToken> recentTokens = tokenRepository.findRecentTokensByDevice(deviceId, recentWindow);
        
        // If we find suspicious patterns, revoke all tokens for this device
        if (recentTokens.size() > 5) { // Threshold for suspicious activity
            log.warn("Revoking all tokens for device {} due to suspected attack", deviceId);
            
            for (DeviceRefreshToken token : recentTokens) {
                if (!token.getRevoked()) {
                    revokeTokenFamily(token.getId(), token.getUserId(), "Suspected reuse attack");
                }
            }
        }
        
        return RefreshTokenResult.failure("Invalid or expired refresh token");
    }
    
    /**
     * Revoke a token and its entire family (for reuse attack mitigation)
     */
    @Transactional
    public boolean revokeTokenFamily(Long tokenId, Long userId, String reason) {
        try {
            int revokedCount = tokenRepository.revokeTokenFamily(
                tokenId, userId, LocalDateTime.now(), reason);
            
            if (revokedCount > 0) {
                log.info("Revoked {} tokens in family for token {} due to: {}", 
                    revokedCount, tokenId, reason);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error revoking token family {}: {}", tokenId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Revoke all tokens for a user
     */
    @Transactional
    public boolean revokeAllUserTokens(Long userId, String reason) {
        try {
            int revokedCount = tokenRepository.revokeAllUserTokens(
                userId, LocalDateTime.now(), reason);
            
            log.info("Revoked {} tokens for user {} due to: {}", revokedCount, userId, reason);
            return true;
            
        } catch (Exception e) {
            log.error("Error revoking all tokens for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Revoke all tokens for a user and device
     */
    @Transactional
    public boolean revokeUserDeviceTokens(Long userId, String deviceId, String reason) {
        try {
            int revokedCount = tokenRepository.revokeAllUserDeviceTokens(
                userId, deviceId, LocalDateTime.now(), reason);
            
            log.info("Revoked {} tokens for user {} on device {} due to: {}", 
                revokedCount, userId, deviceId, reason);
            return true;
            
        } catch (Exception e) {
            log.error("Error revoking tokens for user {} on device {}: {}", 
                userId, deviceId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check token limits for user and device
     */
    private TokenLimitResult checkTokenLimits(Long userId, String deviceId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check user token limit
        long userTokenCount = tokenRepository.countActiveTokensByUserId(userId, now);
        if (userTokenCount >= maxTokensPerUser) {
            // Clean up stale tokens first
            cleanupStaleTokens(userId);
            
            // Recheck after cleanup
            userTokenCount = tokenRepository.countActiveTokensByUserId(userId, now);
            if (userTokenCount >= maxTokensPerUser) {
                return TokenLimitResult.blocked("Too many active tokens for user");
            }
        }
        
        // Check device token limit
        long deviceTokenCount = tokenRepository.countActiveTokensByUserIdAndDeviceId(userId, deviceId, now);
        if (deviceTokenCount >= maxTokensPerDevice) {
            return TokenLimitResult.blocked("Too many active tokens for device");
        }
        
        return TokenLimitResult.allowed();
    }
    
    /**
     * Check for suspicious activity patterns
     */
    private boolean isSuspiciousActivity(DeviceRefreshToken token, String currentIpAddress) {
        // Check for rapid IP changes
        if (token.getIpAddress() != null && !token.getIpAddress().equals(currentIpAddress)) {
            // Allow some IP changes but flag rapid changes
            LocalDateTime recentWindow = LocalDateTime.now().minusHours(1);
            List<DeviceRefreshToken> recentTokens = tokenRepository
                .findRecentTokensByDevice(token.getDeviceId(), recentWindow);
            
            long uniqueIps = recentTokens.stream()
                .map(DeviceRefreshToken::getIpAddress)
                .distinct()
                .count();
            
            if (uniqueIps > 3) { // More than 3 different IPs in 1 hour
                return true;
            }
        }
        
        // Check for token age and usage patterns
        if (token.getAgeInHours() < 1 && token.getGenerationNumber() > 5) {
            // Too many refreshes in short time
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculate generation number for token family
     */
    private Integer calculateGenerationNumber(Long parentTokenId) {
        if (parentTokenId == null) {
            return 1;
        }
        
        Optional<DeviceRefreshToken> parentOpt = tokenRepository.findById(parentTokenId);
        if (parentOpt.isPresent()) {
            return parentOpt.get().getGenerationNumber() + 1;
        }
        
        return 1;
    }
    
    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Cleanup stale tokens for a user
     */
    @Transactional
    public void cleanupStaleTokens(Long userId) {
        LocalDateTime staleTime = LocalDateTime.now().minusHours(staleHours);
        List<DeviceRefreshToken> staleTokens = tokenRepository.findStaleTokens(staleTime);
        
        int revokedCount = 0;
        for (DeviceRefreshToken token : staleTokens) {
            if (token.getUserId().equals(userId)) {
                token.revoke("Stale token cleanup");
                tokenRepository.save(token);
                revokedCount++;
            }
        }
        
        if (revokedCount > 0) {
            log.info("Cleaned up {} stale tokens for user {}", revokedCount, userId);
        }
    }
    
    /**
     * Scheduled cleanup of expired tokens
     */
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupDays);
        int deletedCount = tokenRepository.deleteExpiredTokens(cutoff);
        
        if (deletedCount > 0) {
            log.info("Deleted {} expired refresh tokens", deletedCount);
        }
    }
    
    // Result classes
    public static class RefreshTokenResult {
        private final boolean success;
        private final String message;
        private final Long tokenId;
        private final String token;
        
        private RefreshTokenResult(boolean success, String message, Long tokenId, String token) {
            this.success = success;
            this.message = message;
            this.tokenId = tokenId;
            this.token = token;
        }
        
        public static RefreshTokenResult success(String message, Long tokenId, String token) {
            return new RefreshTokenResult(true, message, tokenId, token);
        }
        
        public static RefreshTokenResult failure(String message) {
            return new RefreshTokenResult(false, message, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getTokenId() { return tokenId; }
        public String getToken() { return token; }
    }
    
    private static class TokenLimitResult {
        private final boolean allowed;
        private final String reason;
        
        private TokenLimitResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
        
        public static TokenLimitResult allowed() {
            return new TokenLimitResult(true, null);
        }
        
        public static TokenLimitResult blocked(String reason) {
            return new TokenLimitResult(false, reason);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
    }
}
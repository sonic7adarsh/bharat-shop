package com.bharatshop.app.interceptor;

import com.bharatshop.app.config.RateLimitingConfig;
import com.bharatshop.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for rate limiting API requests
 */
@Slf4j
// @Component - Temporarily disabled due to Redis dependency
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingConfig.RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientIdentifier(request);
        String path = request.getRequestURI();
        
        RateLimitingConfig.RateLimitType limitType = determineLimitType(path);
        String rateLimitKey = String.format("rate_limit:%s:%s", limitType.name().toLowerCase(), clientId);
        
        boolean allowed = rateLimitService.tryConsume(rateLimitKey, limitType);
        
        if (!allowed) {
            log.warn("Rate limit exceeded for client {} on path {}", clientId, path);
            throw BusinessException.rateLimitExceeded();
        }
        
        log.debug("Rate limit check passed for client {} on path {}", clientId, path);
        return true;
    }

    /**
     * Get client identifier for rate limiting
     * Priority: User ID > Tenant ID > IP Address
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get user ID from request attributes (set by JWT filter)
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId;
        }
        
        // Try to get tenant ID from header
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return "tenant:" + tenantId;
        }
        
        // Fall back to IP address
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
    }

    /**
     * Determine rate limit type based on request path
     */
    private RateLimitingConfig.RateLimitType determineLimitType(String path) {
        if (path.startsWith("/api/auth/")) {
            return RateLimitingConfig.RateLimitType.API_AUTH;
        } else if (path.startsWith("/api/admin/")) {
            return RateLimitingConfig.RateLimitType.API_ADMIN;
        } else {
            return RateLimitingConfig.RateLimitType.API_GENERAL;
        }
    }

    /**
     * Get client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded;
        }
        
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Forwarded header format: for=192.0.2.60;proto=http;by=203.0.113.43
            String[] parts = forwarded.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("for=")) {
                    return part.trim().substring(4);
                }
            }
        }
        
        return request.getRemoteAddr();
    }
}
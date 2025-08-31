package com.bharatshop.app.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to populate MDC (Mapped Diagnostic Context) with request-specific information
 * for structured logging including tenant ID, user ID, trace ID, and request details.
 */
@Component
@Order(1)
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    // MDC Keys
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_ID = "requestId";
    public static final String SESSION_ID = "sessionId";
    public static final String REMOTE_ADDR = "remoteAddr";
    public static final String USER_AGENT = "userAgent";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Generate unique request ID if not present
            String requestId = httpRequest.getHeader("X-Request-ID");
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }

            // Generate trace ID if not present (for distributed tracing)
            String traceId = httpRequest.getHeader("X-Trace-ID");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            // Generate span ID
            String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            // Extract tenant ID from header
            String tenantId = httpRequest.getHeader("X-Tenant-ID");
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = "unknown";
            }

            // Extract user ID from authentication context
            String userId = "anonymous";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                userId = authentication.getName();
            }

            // Populate MDC
            MDC.put(TRACE_ID, traceId);
            MDC.put(SPAN_ID, spanId);
            MDC.put(TENANT_ID, tenantId);
            MDC.put(USER_ID, userId);
            MDC.put(REQUEST_ID, requestId);
            MDC.put(SESSION_ID, httpRequest.getSession(false) != null ? 
                    httpRequest.getSession().getId() : "no-session");
            MDC.put(REMOTE_ADDR, getClientIpAddress(httpRequest));
            MDC.put(USER_AGENT, httpRequest.getHeader("User-Agent"));
            MDC.put(REQUEST_URI, httpRequest.getRequestURI());
            MDC.put(REQUEST_METHOD, httpRequest.getMethod());

            // Add trace ID to response headers for client correlation
            httpResponse.setHeader("X-Trace-ID", traceId);
            httpResponse.setHeader("X-Request-ID", requestId);

            // Log request start
            logger.info("Request started: {} {} from {}", 
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    getClientIpAddress(httpRequest));

            long startTime = System.currentTimeMillis();

            // Continue with the filter chain
            chain.doFilter(request, response);

            // Log request completion
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    httpResponse.getStatus(), 
                    duration);

        } catch (Exception e) {
            logger.error("Error in logging filter", e);
            throw e;
        } finally {
            // Clear MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Extract the real client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("LoggingFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("LoggingFilter destroyed");
    }
}
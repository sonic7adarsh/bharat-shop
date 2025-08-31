package com.bharatshop.app.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

/**
 * Utility class for managing logging context (MDC) operations.
 * Provides convenient methods for setting and retrieving logging context information.
 */
public class LoggingContext {

    // MDC Keys - same as in LoggingFilter
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

    private LoggingContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the current trace ID from MDC
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Get the current tenant ID from MDC
     */
    public static String getTenantId() {
        return MDC.get(TENANT_ID);
    }

    /**
     * Get the current user ID from MDC
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * Get the current request ID from MDC
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /**
     * Set tenant ID in MDC
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            MDC.put(TENANT_ID, tenantId);
        }
    }

    /**
     * Set user ID in MDC
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Set trace ID in MDC
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * Generate and set a new trace ID
     */
    public static String generateAndSetTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    /**
     * Generate and set a new span ID
     */
    public static String generateAndSetSpanId() {
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put(SPAN_ID, spanId);
        return spanId;
    }

    /**
     * Execute a runnable with specific logging context
     */
    public static void withContext(String tenantId, String userId, Runnable runnable) {
        Map<String, String> originalContext = MDC.getCopyOfContextMap();
        try {
            if (tenantId != null) {
                MDC.put(TENANT_ID, tenantId);
            }
            if (userId != null) {
                MDC.put(USER_ID, userId);
            }
            runnable.run();
        } finally {
            if (originalContext != null) {
                MDC.setContextMap(originalContext);
            } else {
                MDC.clear();
            }
        }
    }

    /**
     * Execute a runnable with a new trace context
     */
    public static void withNewTrace(Runnable runnable) {
        Map<String, String> originalContext = MDC.getCopyOfContextMap();
        try {
            generateAndSetTraceId();
            generateAndSetSpanId();
            runnable.run();
        } finally {
            if (originalContext != null) {
                MDC.setContextMap(originalContext);
            } else {
                MDC.clear();
            }
        }
    }

    /**
     * Clear all MDC context
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Get all current MDC context as a map
     */
    public static Map<String, String> getContext() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * Set multiple context values at once
     */
    public static void setContext(Map<String, String> context) {
        if (context != null) {
            context.forEach(MDC::put);
        }
    }

    /**
     * Create a formatted log message with context information
     */
    public static String formatMessage(String message) {
        String traceId = getTraceId();
        String tenantId = getTenantId();
        String userId = getUserId();
        
        StringBuilder sb = new StringBuilder();
        if (traceId != null) {
            sb.append("[trace:").append(traceId).append("] ");
        }
        if (tenantId != null) {
            sb.append("[tenant:").append(tenantId).append("] ");
        }
        if (userId != null) {
            sb.append("[user:").append(userId).append("] ");
        }
        sb.append(message);
        
        return sb.toString();
    }
}
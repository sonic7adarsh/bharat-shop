package com.bharatshop.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing HTTP caching headers including ETag and Cache-Control.
 * Provides methods for generating ETags, setting cache headers, and handling conditional requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpCacheService {

    private static final DateTimeFormatter HTTP_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");

    /**
     * Generate ETag for content
     */
    public String generateETag(Object content) {
        if (content == null) {
            return null;
        }
        
        String contentString = content.toString();
        String hash = DigestUtils.md5DigestAsHex(contentString.getBytes(StandardCharsets.UTF_8));
        return "\"" + hash + "\"";
    }

    /**
     * Generate ETag based on last modified time and content hash
     */
    public String generateETag(Object content, Instant lastModified) {
        if (content == null) {
            return null;
        }
        
        String contentString = content.toString();
        String timeString = lastModified != null ? lastModified.toString() : "";
        String combined = contentString + timeString;
        
        String hash = DigestUtils.md5DigestAsHex(combined.getBytes(StandardCharsets.UTF_8));
        return "\"" + hash + "\"";
    }

    /**
     * Check if request has matching ETag (for 304 Not Modified)
     */
    public boolean hasMatchingETag(HttpServletRequest request, String etag) {
        if (etag == null) {
            return false;
        }
        
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch == null) {
            return false;
        }
        
        // Handle multiple ETags or wildcard
        if ("*".equals(ifNoneMatch)) {
            return true;
        }
        
        // Split by comma and check each ETag
        String[] etags = ifNoneMatch.split(",");
        for (String requestETag : etags) {
            if (etag.equals(requestETag.trim())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if resource has been modified since the If-Modified-Since header
     */
    public boolean isModifiedSince(HttpServletRequest request, Instant lastModified) {
        if (lastModified == null) {
            return true;
        }
        
        String ifModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince == null) {
            return true;
        }
        
        try {
            Instant clientTime = Instant.from(
                DateTimeFormatter.RFC_1123_DATE_TIME.parse(ifModifiedSince)
            );
            
            // Truncate to seconds for comparison (HTTP dates don't include milliseconds)
            Instant serverTime = lastModified.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            clientTime = clientTime.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            
            return serverTime.isAfter(clientTime);
            
        } catch (Exception e) {
            log.debug("Invalid If-Modified-Since header: {}", ifModifiedSince);
            return true;
        }
    }

    /**
     * Create ResponseEntity with caching headers for public content
     */
    public <T> ResponseEntity<T> createCachedResponse(T content, CacheConfig config) {
        return createCachedResponse(content, config, null);
    }

    /**
     * Create ResponseEntity with caching headers and last modified time
     */
    public <T> ResponseEntity<T> createCachedResponse(T content, CacheConfig config, Instant lastModified) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        
        // Set Cache-Control header
        if (config.isPublicCache()) {
            CacheControl cacheControl = CacheControl.maxAge(config.getMaxAge(), TimeUnit.SECONDS)
                .cachePublic();
            
            if (config.isMustRevalidate()) {
                cacheControl = cacheControl.mustRevalidate();
            }
            
            if (config.isNoTransform()) {
                cacheControl = cacheControl.noTransform();
            }
            
            builder.cacheControl(cacheControl);
        } else {
            builder.cacheControl(CacheControl.noCache());
        }
        
        // Set ETag
        if (config.isUseETag()) {
            String etag = generateETag(content, lastModified);
            if (etag != null) {
                builder.eTag(etag);
            }
        }
        
        // Set Last-Modified
        if (lastModified != null) {
            builder.lastModified(lastModified);
        }
        
        // Set Expires header if specified
        if (config.getExpiresAfter() != null) {
            Instant expires = Instant.now().plus(config.getExpiresAfter());
            builder.header(HttpHeaders.EXPIRES, 
                expires.atOffset(ZoneOffset.UTC).format(HTTP_DATE_FORMAT));
        }
        
        // Set Vary header for content negotiation
        if (config.getVaryHeaders() != null && !config.getVaryHeaders().isEmpty()) {
            builder.varyBy(config.getVaryHeaders().toArray(new String[0]));
        }
        
        return builder.body(content);
    }

    /**
     * Create 304 Not Modified response
     */
    public ResponseEntity<Void> createNotModifiedResponse(String etag, Instant lastModified) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(304);
        
        if (etag != null) {
            builder.eTag(etag);
        }
        
        if (lastModified != null) {
            builder.lastModified(lastModified);
        }
        
        return builder.build();
    }

    /**
     * Check if request should return 304 Not Modified
     */
    public boolean shouldReturnNotModified(HttpServletRequest request, String etag, Instant lastModified) {
        // Check ETag first (more specific)
        if (etag != null && hasMatchingETag(request, etag)) {
            return true;
        }
        
        // Check If-Modified-Since
        if (lastModified != null && !isModifiedSince(request, lastModified)) {
            return true;
        }
        
        return false;
    }

    /**
     * Create cache configuration for different content types
     */
    public static class CacheConfig {
        private boolean publicCache = true;
        private long maxAge = 3600; // 1 hour default
        private boolean useETag = true;
        private boolean mustRevalidate = false;
        private boolean noTransform = false;
        private Duration expiresAfter;
        private java.util.List<String> varyHeaders;

        public static CacheConfig publicCache(long maxAgeSeconds) {
            CacheConfig config = new CacheConfig();
            config.publicCache = true;
            config.maxAge = maxAgeSeconds;
            return config;
        }

        public static CacheConfig privateCache(long maxAgeSeconds) {
            CacheConfig config = new CacheConfig();
            config.publicCache = false;
            config.maxAge = maxAgeSeconds;
            return config;
        }

        public static CacheConfig noCache() {
            CacheConfig config = new CacheConfig();
            config.publicCache = false;
            config.maxAge = 0;
            config.useETag = false;
            return config;
        }

        public static CacheConfig longTerm() {
            return publicCache(86400); // 24 hours
        }

        public static CacheConfig shortTerm() {
            return publicCache(300); // 5 minutes
        }

        public static CacheConfig images() {
            CacheConfig config = publicCache(604800); // 1 week
            config.noTransform = true;
            return config;
        }

        public static CacheConfig api() {
            CacheConfig config = publicCache(300); // 5 minutes
            config.mustRevalidate = true;
            return config;
        }

        // Getters and setters
        public boolean isPublicCache() { return publicCache; }
        public CacheConfig publicCache(boolean publicCache) { this.publicCache = publicCache; return this; }
        
        public long getMaxAge() { return maxAge; }
        public CacheConfig maxAge(long maxAge) { this.maxAge = maxAge; return this; }
        
        public boolean isUseETag() { return useETag; }
        public CacheConfig useETag(boolean useETag) { this.useETag = useETag; return this; }
        
        public boolean isMustRevalidate() { return mustRevalidate; }
        public CacheConfig mustRevalidate(boolean mustRevalidate) { this.mustRevalidate = mustRevalidate; return this; }
        
        public boolean isNoTransform() { return noTransform; }
        public CacheConfig noTransform(boolean noTransform) { this.noTransform = noTransform; return this; }
        
        public Duration getExpiresAfter() { return expiresAfter; }
        public CacheConfig expiresAfter(Duration expiresAfter) { this.expiresAfter = expiresAfter; return this; }
        
        public java.util.List<String> getVaryHeaders() { return varyHeaders; }
        public CacheConfig varyHeaders(java.util.List<String> varyHeaders) { this.varyHeaders = varyHeaders; return this; }
    }
}
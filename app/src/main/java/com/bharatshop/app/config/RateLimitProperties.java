package com.bharatshop.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for rate limiting
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private ApiLimit api = new ApiLimit();
    private AuthLimit auth = new AuthLimit();
    private AdminLimit admin = new AdminLimit();
    
    // Default rate limit settings
    public int getDefaultLimit() {
        return api.getRequests();
    }
    
    public int getDefaultDuration() {
        return api.getWindow();
    }

    @Data
    public static class ApiLimit {
        private int requests = 100; // requests per window
        private int window = 1; // window in minutes
    }

    @Data
    public static class AuthLimit {
        private int requests = 10; // requests per window
        private int window = 1; // window in minutes
    }

    @Data
    public static class AdminLimit {
        private int requests = 200; // requests per window
        private int window = 1; // window in minutes
    }
}
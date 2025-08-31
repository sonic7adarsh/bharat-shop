package com.bharatshop.app.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for custom application metrics
 */
@Configuration
public class MetricsConfig {

    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicInteger activeTenants = new AtomicInteger(0);

    /**
     * Counter for API requests by endpoint
     */
    @Bean
    public Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("bharatshop.api.requests")
                .description("Total API requests")
                .register(meterRegistry);
    }

    /**
     * Counter for business operations
     */
    @Bean
    public Counter businessOperationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("bharatshop.business.operations")
                .description("Total business operations")
                .register(meterRegistry);
    }

    /**
     * Timer for database operations
     */
    @Bean
    public Timer databaseOperationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("bharatshop.database.operations")
                .description("Database operation duration")
                .register(meterRegistry);
    }

    /**
     * Timer for external API calls
     */
    @Bean
    public Timer externalApiTimer(MeterRegistry meterRegistry) {
        return Timer.builder("bharatshop.external.api")
                .description("External API call duration")
                .register(meterRegistry);
    }

    /**
     * Counter for authentication events
     */
    @Bean
    public Counter authenticationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("bharatshop.auth.events")
                .description("Authentication events")
                .register(meterRegistry);
    }

    /**
     * Counter for rate limit violations
     */
    @Bean
    public Counter rateLimitCounter(MeterRegistry meterRegistry) {
        return Counter.builder("bharatshop.rate.limit.violations")
                .description("Rate limit violations")
                .register(meterRegistry);
    }

    /**
     * Gauge for active users
     */
    @Bean
    public Gauge activeUsersGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("bharatshop.users.active", activeUsers, AtomicInteger::get)
                .description("Number of active users")
                .register(meterRegistry);
    }

    /**
     * Gauge for active tenants
     */
    @Bean
    public Gauge activeTenantsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("bharatshop.tenants.active", activeTenants, AtomicInteger::get)
                .description("Number of active tenants")
                .register(meterRegistry);
    }

    /**
     * Custom info contributor for application information
     */
    @Bean
    public InfoContributor customInfoContributor() {
        return new InfoContributor() {
            @Override
            public void contribute(Info.Builder builder) {
                Map<String, Object> appInfo = Map.of(
                    "name", "BharatShop",
                    "description", "Multi-tenant e-commerce platform",
                    "version", "1.0.0",
                    "startup-time", LocalDateTime.now().toString()
                );
                
                Map<String, Object> features = Map.of(
                    "multi-tenancy", true,
                    "rate-limiting", true,
                    "monitoring", true,
                    "swagger-docs", true,
                    "containerized", true
                );
                
                builder.withDetail("app", appInfo);
                builder.withDetail("features", features);
            }
        };
    }

    // Utility methods to update gauges
    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    public void incrementActiveTenants() {
        activeTenants.incrementAndGet();
    }

    public void decrementActiveTenants() {
        activeTenants.decrementAndGet();
    }
}
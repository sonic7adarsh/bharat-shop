package com.bharatshop.app.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Custom Redis Health Configuration
 * 
 * This configuration provides a custom Redis health indicator that properly
 * checks Redis connectivity using the same connection factory as our RedisTemplate.
 */
@Configuration
public class RedisHealthConfig {

    /**
     * Custom Redis health indicator that uses our RedisTemplate connection
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return new HealthIndicator() {
            @Override
            public Health health() {
                try {
                    // Test the connection using the same factory as RedisTemplate
                    redisConnectionFactory.getConnection().ping();
                    return Health.up()
                            .withDetail("redis", "Available")
                            .withDetail("host", "localhost")
                            .withDetail("port", 6379)
                            .build();
                } catch (Exception e) {
                    return Health.down()
                            .withDetail("redis", "Not available")
                            .withDetail("error", e.getMessage())
                            .build();
                }
            }
        };
    }
}
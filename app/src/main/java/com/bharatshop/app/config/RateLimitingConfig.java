package com.bharatshop.app.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Configuration for rate limiting using Bucket4j with Redis
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitingConfig {

    private final RateLimitProperties rateLimitProperties;

    @Bean
    public RedisTemplate<String, Object> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    // Simplified rate limiting without distributed proxy manager
    // TODO: Implement proper distributed rate limiting with Redis
    
    @Bean
    public Bucket defaultBucket() {
        Bandwidth limit = Bandwidth.classic(rateLimitProperties.getDefaultLimit(), 
                                          Refill.intervally(rateLimitProperties.getDefaultLimit(), 
                                                          Duration.ofMinutes(rateLimitProperties.getDefaultDuration())));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Bean
    public RateLimitService rateLimitService(Bucket defaultBucket) {
        return new RateLimitService(defaultBucket, rateLimitProperties);
    }

    /**
     * Service for managing rate limits
     */
    public static class RateLimitService {
        private final Bucket defaultBucket;
        private final RateLimitProperties properties;

        public RateLimitService(Bucket defaultBucket, RateLimitProperties properties) {
            this.defaultBucket = defaultBucket;
            this.properties = properties;
        }

        public Bucket createBucket(String key, RateLimitType type) {
            // For now, return the default bucket
            // TODO: Implement per-key buckets with Redis storage
            return defaultBucket;
        }

        public boolean tryConsume(String key, RateLimitType type) {
            return defaultBucket.tryConsume(1);
        }

        public boolean tryConsume(String key, RateLimitType type, long tokens) {
            return defaultBucket.tryConsume(tokens);
        }
    }

    /**
     * Rate limit types
     */
    public enum RateLimitType {
        API_GENERAL,
        API_AUTH,
        API_ADMIN
    }
}
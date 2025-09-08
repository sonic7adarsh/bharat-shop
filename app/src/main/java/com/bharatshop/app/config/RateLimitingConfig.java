package com.bharatshop.app.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * Redis-based rate limiting - only when Redis is available and working
     */
    @Bean
    public ProxyManager<String> redisProxyManager(RedisConnectionFactory connectionFactory) {
        try {
            LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
            String redisUri = "redis://" + lettuceFactory.getStandaloneConfiguration().getHostName() + ":" + lettuceFactory.getStandaloneConfiguration().getPort();
            RedisClient redisClient = RedisClient.create(redisUri);
            StatefulRedisConnection<String, byte[]> connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
            
            // Test the connection
            connection.sync().ping();
            log.info("Successfully connected to Redis at {}", redisUri);
            
            return LettuceBasedProxyManager.builderFor(connection).build();
        } catch (Exception e) {
            log.warn("Failed to connect to Redis: {}. Falling back to in-memory rate limiting.", e.getMessage());
            return null;
        }
    }

    @Bean
    public RateLimitService rateLimitService(RedisConnectionFactory connectionFactory) {
        try {
            ProxyManager<String> proxyManager = redisProxyManager(connectionFactory);
            if (proxyManager != null) {
                log.info("Initializing Redis-based rate limiting service");
                return new RedisRateLimitService(proxyManager, rateLimitProperties);
            }
        } catch (Exception e) {
            log.warn("Redis rate limiting initialization failed: {}", e.getMessage());
        }
        
        log.info("Initializing in-memory rate limiting service");
        return new InMemoryRateLimitService(rateLimitProperties);
    }

    /**
     * Abstract service for managing rate limits
     */
    public static abstract class RateLimitService {
        protected final RateLimitProperties properties;

        public RateLimitService(RateLimitProperties properties) {
            this.properties = properties;
        }

        public abstract boolean tryConsume(String key, RateLimitType type);
        public abstract boolean tryConsume(String key, RateLimitType type, long tokens);
        
        protected BucketConfiguration getBucketConfiguration(RateLimitType type) {
            int limit = getLimit(type);
            Duration duration = getDuration(type);
            
            Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, duration));
            return BucketConfiguration.builder()
                    .addLimit(bandwidth)
                    .build();
        }
        
        private int getLimit(RateLimitType type) {
            return switch (type) {
                case API_AUTH -> properties.getAuthLimit();
                case API_ADMIN -> properties.getAdminLimit();
                default -> properties.getDefaultLimit();
            };
        }
        
        private Duration getDuration(RateLimitType type) {
            return switch (type) {
                case API_AUTH -> Duration.ofMinutes(properties.getAuthDuration());
                case API_ADMIN -> Duration.ofMinutes(properties.getAdminDuration());
                default -> Duration.ofMinutes(properties.getDefaultDuration());
            };
        }
    }

    /**
     * Redis-based rate limiting service
     */
    public static class RedisRateLimitService extends RateLimitService {
        private final ProxyManager<String> proxyManager;

        public RedisRateLimitService(ProxyManager<String> proxyManager, RateLimitProperties properties) {
            super(properties);
            this.proxyManager = proxyManager;
        }

        @Override
        public boolean tryConsume(String key, RateLimitType type) {
            return tryConsume(key, type, 1);
        }

        @Override
        public boolean tryConsume(String key, RateLimitType type, long tokens) {
            String bucketKey = type.name() + ":" + key;
            Bucket bucket = proxyManager.builder().build(bucketKey, getBucketConfiguration(type));
            return bucket.tryConsume(tokens);
        }
    }

    /**
     * In-memory rate limiting service
     */
    public static class InMemoryRateLimitService extends RateLimitService {
        private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

        public InMemoryRateLimitService(RateLimitProperties properties) {
            super(properties);
        }

        @Override
        public boolean tryConsume(String key, RateLimitType type) {
            return tryConsume(key, type, 1);
        }

        @Override
        public boolean tryConsume(String key, RateLimitType type, long tokens) {
            String bucketKey = type.name() + ":" + key;
            Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> {
                BucketConfiguration config = getBucketConfiguration(type);
                Bandwidth[] bandwidths = config.getBandwidths();
                return Bucket.builder()
                    .addLimit(bandwidths[0])
                    .build();
            });
            return bucket.tryConsume(tokens);
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
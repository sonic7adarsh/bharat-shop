package com.bharatshop.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;

/**
 * Redis Cache Configuration
 * 
 * This configuration provides caching capabilities with Redis when available,
 * and falls back to in-memory caching when Redis is not available.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis-based cache manager - only active when Redis is available
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager
                .RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(30))
                        .disableCachingNullValues())
                .build();
    }

    /**
     * Fallback in-memory cache manager - active when Redis is not available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "false", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(
                "products", 
                "categories", 
                "users", 
                "plans", 
                "media",
                "rate-limits"
        );
    }

    /**
     * Redis template configuration - only when Redis is available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
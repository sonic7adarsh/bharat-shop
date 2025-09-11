package com.bharatshop.shared.config;

import com.bharatshop.shared.tenant.TenantContext;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching with tenant-aware keys.
 * Provides different cache configurations for various data types.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisConnectionFactory connectionFactory;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager() {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new TenantAwareKeySerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()))
            .disableCachingNullValues();

        // Specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Product cache - 2 hours TTL
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("product-by-slug", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Category cache - 4 hours TTL (changes less frequently)
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigurations.put("category-trees", defaultConfig.entryTtl(Duration.ofHours(4)));
        
        // Template cache - 6 hours TTL (rarely changes)
        cacheConfigurations.put("templates", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("template-lists", defaultConfig.entryTtl(Duration.ofHours(6)));
        
        // Page cache - 1 hour TTL
        cacheConfigurations.put("pages", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("page-by-slug", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Image cache - 24 hours TTL (static content)
        cacheConfigurations.put("images", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("image-variants", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * Custom key serializer that adds tenant context to cache keys
     */
    private static class TenantAwareKeySerializer extends StringRedisSerializer {
        @Override
        public byte[] serialize(String key) {
            if (key == null) {
                return new byte[0];
            }
            
            Long tenantId = TenantContext.getCurrentTenant();
            String tenantAwareKey = tenantId != null ? 
                "tenant:" + tenantId + ":" + key : 
                "global:" + key;
            
            return super.serialize(tenantAwareKey);
        }
    }
}
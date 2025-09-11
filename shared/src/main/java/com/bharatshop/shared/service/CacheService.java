package com.bharatshop.shared.service;

import com.bharatshop.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Redis cache operations with tenant-aware keys.
 * Provides methods for caching, retrieving, and invalidating cached data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    // Cache names
    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_BY_SLUG_CACHE = "product-by-slug";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String CATEGORY_TREES_CACHE = "category-trees";
    public static final String TEMPLATES_CACHE = "templates";
    public static final String TEMPLATE_LISTS_CACHE = "template-lists";
    public static final String PAGES_CACHE = "pages";
    public static final String PAGE_BY_SLUG_CACHE = "page-by-slug";
    public static final String IMAGES_CACHE = "images";
    public static final String IMAGE_VARIANTS_CACHE = "image-variants";

    /**
     * Get cached value by key
     */
    public <T> T get(String cacheName, String key, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    if (type.isInstance(value)) {
                        log.debug("Cache hit for key: {} in cache: {}", key, cacheName);
                        return type.cast(value);
                    }
                }
            }
            log.debug("Cache miss for key: {} in cache: {}", key, cacheName);
            return null;
        } catch (Exception e) {
            log.warn("Error retrieving from cache: {} key: {}", cacheName, key, e);
            return null;
        }
    }

    /**
     * Put value in cache
     */
    public void put(String cacheName, String key, Object value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
                log.debug("Cached value for key: {} in cache: {}", key, cacheName);
            }
        } catch (Exception e) {
            log.warn("Error caching value for key: {} in cache: {}", key, cacheName, e);
        }
    }

    /**
     * Put value in cache with custom TTL
     */
    public void put(String cacheName, String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            // For custom TTL, use RedisTemplate directly
            String tenantAwareKey = getTenantAwareKey(cacheName + ":" + key);
            redisTemplate.opsForValue().set(tenantAwareKey, value, timeout, timeUnit);
            log.debug("Cached value with TTL for key: {} in cache: {}", key, cacheName);
        } catch (Exception e) {
            log.warn("Error caching value with TTL for key: {} in cache: {}", key, cacheName, e);
        }
    }

    /**
     * Evict specific key from cache
     */
    public void evict(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Evicted key: {} from cache: {}", key, cacheName);
            }
        } catch (Exception e) {
            log.warn("Error evicting key: {} from cache: {}", key, cacheName, e);
        }
    }

    /**
     * Clear entire cache
     */
    public void clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("Error clearing cache: {}", cacheName, e);
        }
    }

    /**
     * Invalidate all product-related caches for current tenant
     */
    public void invalidateProductCaches() {
        clear(PRODUCTS_CACHE);
        clear(PRODUCT_BY_SLUG_CACHE);
        log.info("Invalidated product caches for tenant: {}", TenantContext.getCurrentTenant());
    }

    /**
     * Invalidate all category-related caches for current tenant
     */
    public void invalidateCategoryCaches() {
        clear(CATEGORIES_CACHE);
        clear(CATEGORY_TREES_CACHE);
        log.info("Invalidated category caches for tenant: {}", TenantContext.getCurrentTenant());
    }

    /**
     * Invalidate all template-related caches for current tenant
     */
    public void invalidateTemplateCaches() {
        clear(TEMPLATES_CACHE);
        clear(TEMPLATE_LISTS_CACHE);
        log.info("Invalidated template caches for tenant: {}", TenantContext.getCurrentTenant());
    }

    /**
     * Invalidate all page-related caches for current tenant
     */
    public void invalidatePageCaches() {
        clear(PAGES_CACHE);
        clear(PAGE_BY_SLUG_CACHE);
        log.info("Invalidated page caches for tenant: {}", TenantContext.getCurrentTenant());
    }

    /**
     * Invalidate all image-related caches for current tenant
     */
    public void invalidateImageCaches() {
        clear(IMAGES_CACHE);
        clear(IMAGE_VARIANTS_CACHE);
        log.info("Invalidated image caches for tenant: {}", TenantContext.getCurrentTenant());
    }

    /**
     * Invalidate all caches for current tenant
     */
    public void invalidateAllCaches() {
        invalidateProductCaches();
        invalidateCategoryCaches();
        invalidateTemplateCaches();
        invalidatePageCaches();
        invalidateImageCaches();
        log.info("Invalidated all caches for tenant: {}", TenantContext.getCurrentTenant());
    }

    /**
     * Invalidate caches by pattern (using Redis SCAN)
     */
    public void invalidateByPattern(String pattern) {
        try {
            String tenantAwarePattern = getTenantAwareKey(pattern);
            Set<String> keys = redisTemplate.keys(tenantAwarePattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Error invalidating keys by pattern: {}", pattern, e);
        }
    }

    /**
     * Get tenant-aware cache key
     */
    private String getTenantAwareKey(String key) {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? 
            "tenant:" + tenantId + ":" + key : 
            "global:" + key;
    }

    /**
     * Check if cache is available
     */
    public boolean isCacheAvailable() {
        try {
            redisTemplate.opsForValue().get("health-check");
            return true;
        } catch (Exception e) {
            log.warn("Cache is not available", e);
            return false;
        }
    }
}
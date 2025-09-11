package com.bharatshop.shared.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheService
 * Tests caching operations, tenant-aware keys, and cache invalidation
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache cache;

    @InjectMocks
    private CacheService cacheService;

    private static final String TEST_TENANT_ID = "tenant-123";
    private static final String TEST_KEY = "test-key";
    private static final String TEST_VALUE = "test-value";

    @BeforeEach
    void setUp() {
        // Mock TenantContext if needed
        when(cacheManager.getCache(anyString())).thenReturn(cache);
    }

    @Test
    @DisplayName("Should put value in cache with tenant-aware key")
    void shouldPutValueInCacheWithTenantAwareKey() {
        // Given
        String cacheName = CacheService.PRODUCTS_CACHE;
        
        // When
        cacheService.put(cacheName, TEST_KEY, TEST_VALUE);
        
        // Then
        verify(cacheManager).getCache(cacheName);
        verify(cache).put(anyString(), eq(TEST_VALUE));
    }

    @Test
    @DisplayName("Should get value from cache with tenant-aware key")
    void shouldGetValueFromCacheWithTenantAwareKey() {
        // Given
        String cacheName = CacheService.PRODUCTS_CACHE;
        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(cache.get(anyString())).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(TEST_VALUE);
        
        // When
        Object result = cacheService.get(cacheName, TEST_KEY, Object.class);
        
        // Then
        assertThat(result).isEqualTo(TEST_VALUE);
        verify(cacheManager).getCache(cacheName);
        verify(cache).get(anyString());
    }

    @Test
    @DisplayName("Should return null when cache miss")
    void shouldReturnNullWhenCacheMiss() {
        // Given
        String cacheName = CacheService.PRODUCTS_CACHE;
        when(cache.get(anyString())).thenReturn(null);
        
        // When
        Object result = cacheService.get(cacheName, TEST_KEY, Object.class);
        
        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should clear specific cache")
    void shouldClearSpecificCache() {
        // Given
        String cacheName = CacheService.PRODUCTS_CACHE;
        
        // When
        cacheService.clear(cacheName);
        
        // Then
        verify(cacheManager).getCache(cacheName);
        verify(cache).clear();
    }

    @Test
    @DisplayName("Should invalidate product caches")
    void shouldInvalidateProductCaches() {
        // When
        cacheService.invalidateProductCaches();
        
        // Then
        verify(cacheManager, times(2)).getCache(anyString());
        verify(cache, times(2)).clear();
    }

    @Test
    @DisplayName("Should invalidate category caches")
    void shouldInvalidateCategoryCaches() {
        // When
        cacheService.invalidateCategoryCaches();
        
        // Then
        verify(cacheManager, times(2)).getCache(anyString());
        verify(cache, times(2)).clear();
    }

    @Test
    @DisplayName("Should invalidate page caches")
    void shouldInvalidatePageCaches() {
        // When
        cacheService.invalidatePageCaches();
        
        // Then
        verify(cacheManager, times(2)).getCache(anyString());
        verify(cache, times(2)).clear();
    }

    @Test
    @DisplayName("Should invalidate image caches")
    void shouldInvalidateImageCaches() {
        // When
        cacheService.invalidateImageCaches();
        
        // Then
        verify(cacheManager, times(2)).getCache(anyString());
        verify(cache, times(2)).clear();
    }

    @Test
    @DisplayName("Should invalidate all caches")
    void shouldInvalidateAllCaches() {
        // When
        cacheService.invalidateAllCaches();
        
        // Then
        // Should call all individual invalidation methods
        verify(cacheManager, atLeast(8)).getCache(anyString());
        verify(cache, atLeast(8)).clear();
    }

    @Test
    @DisplayName("Should invalidate caches by pattern")
    void shouldInvalidateCachesByPattern() {
        // Given
        String pattern = "product:*";
        Set<String> keys = Set.of("tenant-123:product:1", "tenant-123:product:2");
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        
        // When
        cacheService.invalidateByPattern(pattern);
        
        // Then
        verify(redisTemplate).keys(anyString());
        verify(redisTemplate).delete((Collection<String>) keys);
    }

    @Test
    @DisplayName("Should handle empty keys when invalidating by pattern")
    void shouldHandleEmptyKeysWhenInvalidatingByPattern() {
        // Given
        String pattern = "nonexistent:*";
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        
        // When
        cacheService.invalidateByPattern(pattern);
        
        // Then
        verify(redisTemplate).keys(anyString());
        verify(redisTemplate, never()).delete((Collection<String>) any());
    }

    @Test
    @DisplayName("Should handle null keys when invalidating by pattern")
    void shouldHandleNullKeysWhenInvalidatingByPattern() {
        // Given
        String pattern = "nonexistent:*";
        when(redisTemplate.keys(anyString())).thenReturn(null);
        
        // When
        cacheService.invalidateByPattern(pattern);
        
        // Then
        verify(redisTemplate).keys(anyString());
        verify(redisTemplate, never()).delete((Collection<String>) any());
    }

    @Test
    @DisplayName("Should handle exceptions when invalidating by pattern")
    void shouldHandleExceptionsWhenInvalidatingByPattern() {
        // Given
        String pattern = "error:*";
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));
        
        // When & Then
        assertThatCode(() -> cacheService.invalidateByPattern(pattern))
                .doesNotThrowAnyException();
    }
}
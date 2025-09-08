package com.bharatshop.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to test Redis connectivity
 */
@RestController
@RequestMapping("/api/test")
public class RedisTestController {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (redisTemplate == null) {
                response.put("status", "FAILED");
                response.put("message", "RedisTemplate not available");
                return ResponseEntity.ok(response);
            }
            
            // Test basic Redis operations
            String testKey = "test:connection";
            String testValue = "Redis is working!";
            
            // Set a value
            redisTemplate.opsForValue().set(testKey, testValue);
            
            // Get the value back
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            
            // Clean up
            redisTemplate.delete(testKey);
            
            if (testValue.equals(retrievedValue)) {
                response.put("status", "SUCCESS");
                response.put("message", "Redis connection is working properly");
                response.put("testValue", retrievedValue);
            } else {
                response.put("status", "FAILED");
                response.put("message", "Redis connection test failed - values don't match");
            }
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Redis connection failed: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
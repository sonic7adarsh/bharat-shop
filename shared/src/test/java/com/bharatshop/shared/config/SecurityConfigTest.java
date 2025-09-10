package com.bharatshop.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityConfig
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Test
    @DisplayName("Should verify SecurityConfig class exists")
    void shouldVerifySecurityConfigClassExists() {
        // When & Then
        assertDoesNotThrow(() -> {
            Class<?> securityConfigClass = Class.forName("com.bharatshop.shared.config.SecurityConfig");
            assertNotNull(securityConfigClass);
            assertTrue(securityConfigClass.getAnnotations().length > 0);
        });
    }

    @Test
    @DisplayName("Should validate class name")
    void shouldValidateClassName() {
        // When & Then
        assertDoesNotThrow(() -> {
            Class<?> securityConfigClass = Class.forName("com.bharatshop.shared.config.SecurityConfig");
            assertEquals("SecurityConfig", securityConfigClass.getSimpleName());
        });
    }

    @Test
    @DisplayName("Should validate package name")
    void shouldValidatePackageName() {
        // When & Then
        assertDoesNotThrow(() -> {
            Class<?> securityConfigClass = Class.forName("com.bharatshop.shared.config.SecurityConfig");
            assertEquals("com.bharatshop.shared.config", securityConfigClass.getPackage().getName());
        });
    }
}
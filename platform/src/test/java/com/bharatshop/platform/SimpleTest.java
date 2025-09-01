package com.bharatshop.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple test to verify test infrastructure works
 */
class SimpleTest {

    @Test
    @DisplayName("Should pass simple test")
    void shouldPassSimpleTest() {
        // Given
        String expected = "Hello World";
        
        // When
        String actual = "Hello World";
        
        // Then
        assertThat(actual).isEqualTo(expected);
    }
}
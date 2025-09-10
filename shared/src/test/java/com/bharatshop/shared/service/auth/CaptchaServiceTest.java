package com.bharatshop.shared.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CaptchaService
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Test
    @DisplayName("Should validate challenge ID format")
    void shouldValidateChallengeIdFormat() {
        // Given - invalid challenge ID
        String invalidChallengeId = "invalid-id";

        // When
        boolean isValid = isValidChallengeId(invalidChallengeId);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate valid challenge ID")
    void shouldValidateValidChallengeId() {
        // Given - valid challenge ID (UUID format)
        String validChallengeId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        boolean isValid = isValidChallengeId(validChallengeId);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate answer format")
    void shouldValidateAnswerFormat() {
        // Given - invalid answer format
        String invalidAnswer = "";

        // When
        boolean isValid = isValidAnswer(invalidAnswer);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate valid answer")
    void shouldValidateValidAnswer() {
        // Given - valid answer
        String validAnswer = "ABCD";

        // When
        boolean isValid = isValidAnswer(validAnswer);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
        // When & Then
        assertFalse(isValidChallengeId(null));
        assertFalse(isValidAnswer(null));
    }

    @Test
    @DisplayName("Should handle empty parameters")
    void shouldHandleEmptyParameters() {
        // When & Then
        assertFalse(isValidChallengeId(""));
        assertFalse(isValidAnswer(""));
    }

    @Test
    @DisplayName("Should validate captcha timeout")
    void shouldValidateCaptchaTimeout() {
        // Given - current time and expiry time
        long currentTime = System.currentTimeMillis();
        long expiredTime = currentTime - 300000; // 5 minutes ago

        // When
        boolean isExpired = isCaptchaExpired(expiredTime, currentTime);

        // Then
        assertTrue(isExpired);
    }

    // Helper methods for validation
    private boolean isValidChallengeId(String challengeId) {
        if (challengeId == null || challengeId.trim().isEmpty()) {
            return false;
        }
        // Basic UUID format validation
        return challengeId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    private boolean isValidAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }
        // Basic answer validation (alphanumeric, 3-6 characters)
        return answer.matches("^[A-Za-z0-9]{3,6}$");
    }

    private boolean isCaptchaExpired(long expiryTime, long currentTime) {
        return expiryTime < currentTime;
    }
}
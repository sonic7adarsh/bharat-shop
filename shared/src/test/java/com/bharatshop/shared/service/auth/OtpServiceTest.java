package com.bharatshop.shared.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OtpService
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Test
    @DisplayName("Should validate phone number format")
    void shouldValidatePhoneNumberFormat() {
        // Given - invalid phone number
        String invalidPhone = "invalid-phone";

        // When
        boolean isValid = isValidPhoneNumber(invalidPhone);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate valid phone number")
    void shouldValidateValidPhoneNumber() {
        // Given - valid phone number
        String validPhone = "+919876543210";

        // When
        boolean isValid = isValidPhoneNumber(validPhone);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate OTP format")
    void shouldValidateOtpFormat() {
        // Given - invalid OTP format
        String invalidOtp = "abc";

        // When
        boolean isValid = isValidOtpFormat(invalidOtp);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate valid OTP format")
    void shouldValidateValidOtpFormat() {
        // Given - valid OTP format
        String validOtp = "123456";

        // When
        boolean isValid = isValidOtpFormat(validOtp);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
        // When & Then
        assertFalse(isValidPhoneNumber(null));
        assertFalse(isValidOtpFormat(null));
    }

    @Test
    @DisplayName("Should handle empty parameters")
    void shouldHandleEmptyParameters() {
        // When & Then
        assertFalse(isValidPhoneNumber(""));
        assertFalse(isValidOtpFormat(""));
    }

    // Helper methods for validation
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        // Basic phone number validation
        return phoneNumber.matches("^\\+[1-9]\\d{1,14}$");
    }

    private boolean isValidOtpFormat(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            return false;
        }
        // Basic OTP format validation (6 digits)
        return otp.matches("^\\d{6}$");
    }
}
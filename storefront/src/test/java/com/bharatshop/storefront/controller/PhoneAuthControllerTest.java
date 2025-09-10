package com.bharatshop.storefront.controller;

import com.bharatshop.shared.service.auth.CaptchaService;
import com.bharatshop.shared.service.auth.OtpService;
import com.bharatshop.shared.service.auth.OtpSendRequest;
import com.bharatshop.shared.service.auth.OtpVerifyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for PhoneAuthController
 * Tests phone authentication endpoints with mocked services
 */
@WebMvcTest(PhoneAuthController.class)
class PhoneAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @MockBean
    private CaptchaService captchaService;

    private PhoneAuthController.OtpSendRequestDto sendRequest;
    private PhoneAuthController.OtpVerifyRequestDto verifyRequest;
    private OtpService.OtpSendResult successSendResult;
    private OtpService.OtpSendResult failureSendResult;
    private OtpService.OtpVerifyResult successVerifyResult;
    private OtpService.OtpVerifyResult failureVerifyResult;

    @BeforeEach
    void setUp() {
        // Setup test data
        sendRequest = new PhoneAuthController.OtpSendRequestDto();
        sendRequest.setPhoneNumber("+919876543210");
        sendRequest.setType("LOGIN");
        sendRequest.setDeviceId("device123");
        sendRequest.setSessionId("session123");
        sendRequest.setLocale("en");
        sendRequest.setTimezone("Asia/Kolkata");
        sendRequest.setAppVersion("1.0.0");

        verifyRequest = new PhoneAuthController.OtpVerifyRequestDto();
        verifyRequest.setPhoneNumber("+919876543210");
        verifyRequest.setOtpCode("123456");
        verifyRequest.setType("LOGIN");
        verifyRequest.setDeviceId("device123");
        verifyRequest.setSessionId("session123");
        verifyRequest.setTimestamp(System.currentTimeMillis());

        // Setup mock results
        successSendResult = new OtpService.OtpSendResult(true, "OTP sent successfully", "msg123", null, null);
        failureSendResult = new OtpService.OtpSendResult(false, "Rate limit exceeded", null, "RATE_LIMIT_EXCEEDED", null);
        successVerifyResult = new OtpService.OtpVerifyResult(true, "OTP verified successfully", "+919876543210", null, null);
        failureVerifyResult = new OtpService.OtpVerifyResult(false, "Invalid OTP", null, "INVALID_OTP", null);
    }

    @Test
    @DisplayName("Should send OTP successfully without CAPTCHA")
    void shouldSendOtpSuccessfully() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(false);
        when(otpService.sendOtp(any(OtpSendRequest.class))).thenReturn(successSendResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("OTP sent successfully")))
                .andExpect(jsonPath("$.maskedPhone", is("+91****3210")))
                .andExpect(jsonPath("$.expiresIn", is(300)))
                .andExpect(jsonPath("$.captchaRequired", is(false)));

        verify(otpService).sendOtp(any(OtpSendRequest.class));
        verify(captchaService).recordSuccessfulAttempt(anyString());
    }

    @Test
    @DisplayName("Should require CAPTCHA when abuse detected")
    void shouldRequireCaptchaOnAbuse() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpected(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("CAPTCHA verification required")))
                .andExpect(jsonPath("$.errorCode", is("CAPTCHA_REQUIRED")))
                .andExpect(jsonPath("$.captchaRequired", is(true)));

        verify(otpService, never()).sendOtp(any());
    }

    @Test
    @DisplayName("Should validate CAPTCHA when provided")
    void shouldValidateCaptchaWhenProvided() throws Exception {
        // Given
        sendRequest.setCaptchaToken("captcha123");
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(any(CaptchaService.CaptchaValidationRequest.class)))
                .thenReturn(new CaptchaService.CaptchaValidationResult(true, "CAPTCHA valid", null, null));
        when(otpService.sendOtp(any(OtpSendRequest.class))).thenReturn(successSendResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(captchaService).validateCaptcha(any(CaptchaService.CaptchaValidationRequest.class));
        verify(otpService).sendOtp(any(OtpSendRequest.class));
    }

    @Test
    @DisplayName("Should reject invalid CAPTCHA")
    void shouldRejectInvalidCaptcha() throws Exception {
        // Given
        sendRequest.setCaptchaToken("invalid_captcha");
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(any(CaptchaService.CaptchaValidationRequest.class)))
                .thenReturn(new CaptchaService.CaptchaValidationResult(false, "Invalid CAPTCHA", "CAPTCHA_INVALID", null));

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("CAPTCHA validation failed")))
                .andExpect(jsonPath("$.errorCode", is("CAPTCHA_INVALID")));

        verify(captchaService).recordFailedAttempt(anyString());
        verify(otpService, never()).sendOtp(any());
    }

    @Test
    @DisplayName("Should handle OTP send failure")
    void shouldHandleOtpSendFailure() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(false);
        when(otpService.sendOtp(any(OtpSendRequest.class))).thenReturn(failureSendResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Rate limit exceeded")))
                .andExpect(jsonPath("$.errorCode", is("RATE_LIMIT_EXCEEDED")));

        verify(captchaService).recordFailedAttempt(anyString());
    }

    @Test
    @DisplayName("Should verify OTP successfully")
    void shouldVerifyOtpSuccessfully() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(false);
        when(otpService.verifyOtp(any(OtpVerifyRequest.class))).thenReturn(successVerifyResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("OTP verified successfully")))
                .andExpect(jsonPath("$.phoneNumber", is("+91****3210")))
                .andExpect(jsonPath("$.verified", is(true)));

        verify(otpService).verifyOtp(any(OtpVerifyRequest.class));
        verify(captchaService).resetAttemptTracking(anyString());
    }

    @Test
    @DisplayName("Should handle OTP verification failure")
    void shouldHandleOtpVerificationFailure() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(false);
        when(otpService.verifyOtp(any(OtpVerifyRequest.class))).thenReturn(failureVerifyResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid OTP")))
                .andExpect(jsonPath("$.errorCode", is("INVALID_OTP")));

        verify(captchaService).recordFailedAttempt(anyString());
    }

    @Test
    @DisplayName("Should validate required fields for send OTP")
    void shouldValidateRequiredFieldsForSendOtp() throws Exception {
        // Given - empty request
        PhoneAuthController.OtpSendRequestDto emptyRequest = new PhoneAuthController.OtpSendRequestDto();

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate phone number format")
    void shouldValidatePhoneNumberFormat() throws Exception {
        // Given
        sendRequest.setPhoneNumber("invalid-phone");

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should extract IP address from X-Forwarded-For header")
    void shouldExtractIpFromForwardedHeader() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(false);
        when(otpService.sendOtp(any(OtpSendRequest.class))).thenReturn(successSendResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "203.0.113.1, 192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isOk());

        // Verify that the first IP from X-Forwarded-For is used
        verify(otpService).sendOtp(argThat(request -> 
            "203.0.113.1".equals(request.getIpAddress())));
    }

    @Test
    @DisplayName("Should mask phone number in responses")
    void shouldMaskPhoneNumberInResponses() throws Exception {
        // Given
        when(captchaService.isCaptchaRequired(anyString())).thenReturn(false);
        when(otpService.sendOtp(any(OtpSendRequest.class))).thenReturn(successSendResult);

        // When & Then
        mockMvc.perform(post("/api/storefront/auth/phone/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest))
                .header("X-Forwarded-For", "192.168.1.1")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedPhone", is("+91****3210")));
    }
}
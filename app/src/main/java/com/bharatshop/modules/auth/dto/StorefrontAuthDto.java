package com.bharatshop.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTOs for storefront authentication (customers).
 */
public class StorefrontAuthDto {
    
    @Data
    public static class RegisterRequest {
        @Email(message = "Invalid email format")
        private String email;
        
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phone;
        
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
        
        private String firstName;
        private String lastName;
    }
    
    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email or phone is required")
        private String emailOrPhone;
        
        private String password;
        private String otp;
    }
    
    @Data
    public static class SendOtpRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phone;
    }
    
    @Data
    public static class VerifyOtpRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;
        
        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otp;
    }
    
    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }
    
    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }
    
    @Data
    @Builder
    public static class AuthResponse {
        private String sessionId;
        private UserInfo user;
        private String message;
    }
    
    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String phone;
        private String firstName;
        private String lastName;
        private String status;
        private boolean emailVerified;
        private boolean phoneVerified;
        private LocalDateTime createdAt;
    }
    
    @Data
    public static class MessageResponse {
        private String message;
        private Object data;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public MessageResponse(String message, Object data) {
            this.message = message;
            this.data = data;
        }
    }
}
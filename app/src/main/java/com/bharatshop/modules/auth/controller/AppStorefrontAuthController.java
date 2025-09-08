package com.bharatshop.modules.auth.controller;

import com.bharatshop.modules.auth.dto.StorefrontAuthDto;
import com.bharatshop.modules.auth.service.StorefrontAuthBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Storefront authentication controller for customers.
 * Handles session-based authentication with email or phone+OTP login.
 */
@RestController
@RequestMapping("/api/app/storefront/auth")
@RequiredArgsConstructor
@Slf4j
public class AppStorefrontAuthController {
    
    private final StorefrontAuthBridgeService authService;
    private static final String USER_SESSION_KEY = "storefront_user_id";
    
    /**
     * Register a new customer account
     */
    @PostMapping("/register")
    public ResponseEntity<StorefrontAuthDto.AuthResponse> register(
            @Valid @RequestBody StorefrontAuthDto.RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            StorefrontAuthDto.AuthResponse response = authService.register(request);
            
            // Store user ID in session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(USER_SESSION_KEY, response.getUser().getId());
            session.setMaxInactiveInterval(24 * 60 * 60); // 24 hours
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                StorefrontAuthDto.AuthResponse.builder()
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                StorefrontAuthDto.AuthResponse.builder()
                    .message("Registration failed. Please try again.")
                    .build()
            );
        }
    }
    
    /**
     * Login with email/phone and password or OTP
     */
    @PostMapping("/login")
    public ResponseEntity<StorefrontAuthDto.AuthResponse> login(
            @Valid @RequestBody StorefrontAuthDto.LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            StorefrontAuthDto.AuthResponse response = authService.login(request);
            
            // Store user ID in session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(USER_SESSION_KEY, response.getUser().getId());
            session.setMaxInactiveInterval(24 * 60 * 60); // 24 hours
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                StorefrontAuthDto.AuthResponse.builder()
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                StorefrontAuthDto.AuthResponse.builder()
                    .message("Login failed. Please try again.")
                    .build()
            );
        }
    }
    
    /**
     * Send OTP to phone number
     */
    @PostMapping("/send-otp")
    public ResponseEntity<StorefrontAuthDto.MessageResponse> sendOtp(
            @Valid @RequestBody StorefrontAuthDto.SendOtpRequest request) {
        try {
            StorefrontAuthDto.MessageResponse response = authService.sendOtp(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Send OTP failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new StorefrontAuthDto.MessageResponse(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Send OTP error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new StorefrontAuthDto.MessageResponse("Failed to send OTP. Please try again.")
            );
        }
    }
    
    /**
     * Verify OTP for phone number
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<StorefrontAuthDto.MessageResponse> verifyOtp(
            @Valid @RequestBody StorefrontAuthDto.VerifyOtpRequest request) {
        try {
            StorefrontAuthDto.MessageResponse response = authService.verifyOtp(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Verify OTP failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new StorefrontAuthDto.MessageResponse(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Verify OTP error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new StorefrontAuthDto.MessageResponse("OTP verification failed. Please try again.")
            );
        }
    }
    
    /**
     * Logout and invalidate session
     */
    @PostMapping("/logout")
    public ResponseEntity<StorefrontAuthDto.MessageResponse> logout(
            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                String sessionId = session.getId();
                authService.logout(sessionId);
                session.invalidate();
            }
            
            return ResponseEntity.ok(
                new StorefrontAuthDto.MessageResponse("Logged out successfully")
            );
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new StorefrontAuthDto.MessageResponse("Logout failed. Please try again.")
            );
        }
    }
    
    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<StorefrontAuthDto.UserInfo> getProfile(
            HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            StorefrontAuthDto.UserInfo userInfo = authService.getProfile(userId);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Get profile failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Get profile error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<StorefrontAuthDto.UserInfo> updateProfile(
            @Valid @RequestBody StorefrontAuthDto.UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            StorefrontAuthDto.UserInfo userInfo = authService.updateUserProfile(userId, request);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Update profile failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Update profile error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<StorefrontAuthDto.MessageResponse> changePassword(
            @Valid @RequestBody StorefrontAuthDto.ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new StorefrontAuthDto.MessageResponse("Authentication required")
                );
            }
            
            StorefrontAuthDto.MessageResponse response = authService.changePassword(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Change password failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new StorefrontAuthDto.MessageResponse(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Change password error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new StorefrontAuthDto.MessageResponse("Password change failed. Please try again.")
            );
        }
    }
    
    /**
     * Check authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<StorefrontAuthDto.MessageResponse> getAuthStatus(
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId != null) {
            return ResponseEntity.ok(
                new StorefrontAuthDto.MessageResponse("Authenticated")
            );
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new StorefrontAuthDto.MessageResponse("Not authenticated")
            );
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<StorefrontAuthDto.MessageResponse> health() {
        return ResponseEntity.ok(
            new StorefrontAuthDto.MessageResponse("Storefront auth service is running")
        );
    }
    
    /**
     * Helper method to get current user ID from session
     */
    private String getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (String) session.getAttribute(USER_SESSION_KEY);
        }
        return null;
    }
}
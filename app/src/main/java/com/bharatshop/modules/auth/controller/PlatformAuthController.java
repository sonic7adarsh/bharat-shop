package com.bharatshop.modules.auth.controller;

import com.bharatshop.modules.auth.dto.PlatformAuthDto;
import com.bharatshop.modules.auth.service.PlatformAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Platform authentication controller for vendors, admins, and staff.
 * Handles JWT-based authentication with access and refresh tokens.
 */
@RestController
@RequestMapping("/api/platform/auth")
@RequiredArgsConstructor
@Slf4j
public class PlatformAuthController {
    
    private final PlatformAuthService authService;
    
    /**
     * Register a new vendor account
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody PlatformAuthDto.RegisterRequest request) {
        try {
            PlatformAuthDto.AuthResponse response = authService.registerVendor(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new PlatformAuthDto.MessageResponse(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new PlatformAuthDto.MessageResponse("Registration failed. Please try again.")
            );
        }
    }
    
    /**
     * Login with email and password
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody PlatformAuthDto.LoginRequest request) {
        try {
            PlatformAuthDto.AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new PlatformAuthDto.MessageResponse(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new PlatformAuthDto.MessageResponse("Login failed. Please try again.")
            );
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody PlatformAuthDto.RefreshTokenRequest request) {
        try {
            PlatformAuthDto.AuthResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new PlatformAuthDto.MessageResponse(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new PlatformAuthDto.MessageResponse("Token refresh failed. Please try again.")
            );
        }
    }
    
    /**
     * Logout and invalidate refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<PlatformAuthDto.MessageResponse> logout(
            Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                String userId = authentication.getName();
                authService.logout(userId);
            }
            
            return ResponseEntity.ok(
                new PlatformAuthDto.MessageResponse("Logged out successfully")
            );
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new PlatformAuthDto.MessageResponse("Logout failed. Please try again.")
            );
        }
    }
    
    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<PlatformAuthDto.UserInfo> getCurrentUser(
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String userId = authentication.getName();
            PlatformAuthDto.UserInfo userInfo = authService.getCurrentUser(userId);
            
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Get current user failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Get current user error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<PlatformAuthDto.MessageResponse> health() {
        return ResponseEntity.ok(
            new PlatformAuthDto.MessageResponse("Platform auth service is running")
        );
    }
}
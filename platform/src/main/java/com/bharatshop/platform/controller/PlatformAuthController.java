package com.bharatshop.platform.controller;

import com.bharatshop.platform.dto.auth.*;
import com.bharatshop.platform.service.PlatformAuthService;
import com.bharatshop.platform.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/auth")
@RequiredArgsConstructor
@Tag(name = "Platform Authentication", description = "Authentication APIs for platform users (vendors, admins, staff)")
public class PlatformAuthController {

    private final PlatformAuthService authService;

    @PostMapping("/register/vendor")
    @Operation(summary = "Register new vendor", description = "Register a new vendor account")
    public ResponseEntity<ApiResponse<AuthResponse>> registerVendor(
            @Valid @RequestBody RegisterVendorRequest request) {
        try {
            AuthResponse response = authService.registerVendor(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Vendor registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate platform user and get JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout user and invalidate token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);
            }
            return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Logout failed"));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            Authentication authentication) {
        try {
            String email = authentication.getName();
            AuthResponse.UserInfo userInfo = authService.getCurrentUser(email);
            return ResponseEntity.ok(ApiResponse.success(userInfo, "User information retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found"));
        }
    }
}
package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.auth.*;
import com.bharatshop.storefront.service.StorefrontAuthService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront/auth")
@RequiredArgsConstructor
@Tag(name = "Storefront Authentication", description = "Authentication APIs for storefront customers")
public class StorefrontAuthController {

    private final StorefrontAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new customer", description = "Register a new customer account")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request,
            HttpServletRequest httpRequest) {
        try {
            CustomerProfileResponse response = authService.registerCustomer(request);
            
            // Create session for the new user
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("user_email", response.getEmail());
            session.setAttribute("user_id", response.getId().toString());
            session.setAttribute("authenticated", true);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Customer registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login/email")
    @Operation(summary = "Login with email", description = "Authenticate customer using email and password")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> loginWithEmail(
            @Valid @RequestBody EmailLoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            CustomerProfileResponse response = authService.loginWithEmail(request);
            
            // Create session for the authenticated user
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("user_email", response.getEmail());
            session.setAttribute("user_id", response.getId().toString());
            session.setAttribute("authenticated", true);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials"));
        }
    }

    @PostMapping("/login/phone")
    @Operation(summary = "Initiate phone login", description = "Send OTP to phone number for authentication")
    public ResponseEntity<ApiResponse<String>> initiatePhoneLogin(
            @Valid @RequestBody PhoneLoginRequest request) {
        try {
            String message = authService.initiatePhoneLogin(request);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login/phone/verify")
    @Operation(summary = "Verify phone login", description = "Complete phone authentication by verifying OTP")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> verifyPhoneLogin(
            @Valid @RequestBody OtpVerificationRequest request,
            HttpServletRequest httpRequest) {
        try {
            CustomerProfileResponse response = authService.verifyPhoneLogin(request);
            
            // Create session for the authenticated user
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("user_email", response.getEmail());
            session.setAttribute("user_id", response.getId().toString());
            session.setAttribute("authenticated", true);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Phone login successful"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout customer and invalidate session")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String sessionId = session.getId();
                authService.logout(sessionId);
                session.invalidate();
            }
            return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Logout failed"));
        }
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Get current authenticated customer profile")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCurrentProfile(
            Authentication authentication) {
        try {
            String email = authentication.getName();
            CustomerProfileResponse profile = authService.getCurrentUserProfile(email);
            return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found"));
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update current authenticated customer profile")
    @SecurityRequirement(name = "sessionAuth")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @Valid @RequestBody CustomerProfileResponse profileUpdate,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            CustomerProfileResponse updatedProfile = authService.updateProfile(email, profileUpdate);
            return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Profile update failed"));
        }
    }

    @GetMapping("/session/check")
    @Operation(summary = "Check session status", description = "Check if user session is valid")
    public ResponseEntity<ApiResponse<Boolean>> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean isAuthenticated = session != null && 
                Boolean.TRUE.equals(session.getAttribute("authenticated"));
        
        return ResponseEntity.ok(ApiResponse.success(isAuthenticated, 
                isAuthenticated ? "Session is valid" : "No valid session"));
    }
}
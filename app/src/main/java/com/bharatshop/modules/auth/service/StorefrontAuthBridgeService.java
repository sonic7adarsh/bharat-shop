package com.bharatshop.modules.auth.service;

import com.bharatshop.modules.auth.dto.StorefrontAuthDto;
import com.bharatshop.storefront.dto.auth.*;
import com.bharatshop.storefront.entity.StorefrontUser;
import com.bharatshop.storefront.repository.StorefrontUserRepository;
import com.bharatshop.storefront.service.StorefrontAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bridge service that adapts StorefrontAuthService to work with app module DTOs
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorefrontAuthBridgeService {
    
    private final StorefrontAuthService storefrontAuthService;
    private final StorefrontUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public StorefrontAuthDto.AuthResponse register(StorefrontAuthDto.RegisterRequest request) {
        // Convert to storefront DTO and call existing method
        RegisterCustomerRequest storefrontRequest = RegisterCustomerRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .build();
        
        CustomerProfileResponse profile = storefrontAuthService.registerCustomer(storefrontRequest);
        
        // Convert back to app module DTO
        StorefrontAuthDto.UserInfo userInfo = StorefrontAuthDto.UserInfo.builder()
                .id(profile.getId().toString())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .status("ACTIVE")
                .emailVerified(profile.getEmailVerified())
                .phoneVerified(profile.getPhoneVerified())
                .createdAt(profile.getCreatedAt())
                .build();
        
        return StorefrontAuthDto.AuthResponse.builder()
                .user(userInfo)
                .message("Registration successful")
                .build();
    }
    
    public StorefrontAuthDto.AuthResponse login(StorefrontAuthDto.LoginRequest request) {
        CustomerProfileResponse profile;
        
        // Assume emailOrPhone is an email for simplicity
        EmailLoginRequest emailRequest = EmailLoginRequest.builder()
                .email(request.getEmailOrPhone())
                .password(request.getPassword())
                .build();
        profile = storefrontAuthService.loginWithEmail(emailRequest);
        
        // Convert to app module DTO
        StorefrontAuthDto.UserInfo userInfo = StorefrontAuthDto.UserInfo.builder()
                .id(profile.getId().toString())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .status("ACTIVE")
                .emailVerified(profile.getEmailVerified())
                .phoneVerified(profile.getPhoneVerified())
                .createdAt(profile.getCreatedAt())
                .build();
        
        return StorefrontAuthDto.AuthResponse.builder()
                .user(userInfo)
                .message("Login successful")
                .build();
    }
    
    public StorefrontAuthDto.UserInfo updateUserProfile(String userId, StorefrontAuthDto.UpdateProfileRequest request) {
        // Find user by ID
        StorefrontUser user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Update fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        user = userRepository.save(user);
        
        // Convert to app module DTO
        return StorefrontAuthDto.UserInfo.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status("ACTIVE")
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    public StorefrontAuthDto.MessageResponse changePassword(String userId, StorefrontAuthDto.ChangePasswordRequest request) {
        // Find user by ID
        StorefrontUser user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return new StorefrontAuthDto.MessageResponse("Password changed successfully");
    }
    
    public StorefrontAuthDto.MessageResponse sendOtp(StorefrontAuthDto.SendOtpRequest request) {
        // Convert to storefront module DTO
        PhoneLoginRequest phoneRequest = PhoneLoginRequest.builder()
                .phone(request.getPhone())
                .build();
        
        String message = storefrontAuthService.initiatePhoneLogin(phoneRequest);
        return new StorefrontAuthDto.MessageResponse(message);
    }
    
    public StorefrontAuthDto.MessageResponse verifyOtp(StorefrontAuthDto.VerifyOtpRequest request) {
        // Convert to storefront module DTO
        OtpVerificationRequest otpRequest = OtpVerificationRequest.builder()
                .phone(request.getPhone())
                .otp(request.getOtp())
                .build();
        
        CustomerProfileResponse profile = storefrontAuthService.verifyPhoneLogin(otpRequest);
        
        // Return success message instead of full auth response
        return new StorefrontAuthDto.MessageResponse("OTP verified successfully");
    }
    
    public void logout(String userId) {
        // Delegate to storefront service
        storefrontAuthService.logout(userId);
    }
    
    public StorefrontAuthDto.UserInfo getProfile(String userId) {
        CustomerProfileResponse profile = storefrontAuthService.getCurrentUserProfile(userId);
        
        return StorefrontAuthDto.UserInfo.builder()
                .id(profile.getId().toString())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .status("ACTIVE")
                .emailVerified(profile.getEmailVerified())
                .phoneVerified(profile.getPhoneVerified())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
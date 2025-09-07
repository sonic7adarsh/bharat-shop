package com.bharatshop.modules.auth.service;

import com.bharatshop.modules.auth.dto.StorefrontAuthDto;
import com.bharatshop.shared.entity.User;
import com.bharatshop.storefront.repository.StorefrontUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bridge service for storefront authentication using shared entities
 */
@Service
@Slf4j
@Transactional
public class StorefrontAuthBridgeService {
    
    @Autowired
    @Qualifier("storefrontUserRepository")
    private StorefrontUserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public StorefrontAuthDto.AuthResponse register(StorefrontAuthDto.RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByEmailAndDeletedAtIsNull(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with email already exists");
        }
        
        if (request.getPhone() != null && userRepository.findByPhoneAndDeletedAtIsNull(request.getPhone()).isPresent()) {
            throw new RuntimeException("User with phone number already exists");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Convert to DTO
        StorefrontAuthDto.UserInfo userInfo = StorefrontAuthDto.UserInfo.builder()
                .id(savedUser.getId().toString())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .status("ACTIVE")
                .emailVerified(savedUser.getEmailVerified())
                .phoneVerified(savedUser.getPhoneVerified())
                .createdAt(savedUser.getCreatedAt())
                .build();
        
        return StorefrontAuthDto.AuthResponse.builder()
                .user(userInfo)
                .message("Registration successful")
                .build();
    }
    
    public StorefrontAuthDto.AuthResponse login(StorefrontAuthDto.LoginRequest request) {
        // Find user by email or phone
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(request.getEmailOrPhone());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhoneAndDeletedAtIsNull(request.getEmailOrPhone());
        }
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        // Convert to DTO
        StorefrontAuthDto.UserInfo userInfo = StorefrontAuthDto.UserInfo.builder()
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
        
        return StorefrontAuthDto.AuthResponse.builder()
                .user(userInfo)
                .message("Login successful")
                .build();
    }
    
    public StorefrontAuthDto.UserInfo updateUserProfile(String userId, StorefrontAuthDto.UpdateProfileRequest request) {
        // Find user by ID
        User user = userRepository.findById(Long.parseLong(userId))
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
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return new StorefrontAuthDto.MessageResponse("Password changed successfully");
    }
    
    public StorefrontAuthDto.MessageResponse sendOtp(StorefrontAuthDto.SendOtpRequest request) {
        // TODO: Implement OTP service integration
        // For now, return a placeholder response
        log.info("OTP send requested for phone: {}", request.getPhone());
        return new StorefrontAuthDto.MessageResponse("OTP sent successfully");
    }
    
    public StorefrontAuthDto.MessageResponse verifyOtp(StorefrontAuthDto.VerifyOtpRequest request) {
        // TODO: Implement OTP verification
        // For now, return a placeholder response
        log.info("OTP verification requested for phone: {} with OTP: {}", request.getPhone(), request.getOtp());
        return new StorefrontAuthDto.MessageResponse("OTP verified successfully");
    }
    
    public void logout(String userId) {
        // Simple logout - no session management needed for now
        log.info("User logged out: {}", userId);
    }
    
    public StorefrontAuthDto.UserInfo getProfile(String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
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
}
package com.bharatshop.modules.auth.service;

import com.bharatshop.modules.auth.dto.StorefrontAuthDto;
import com.bharatshop.modules.auth.entity.StorefrontUser;
import com.bharatshop.modules.auth.repository.StorefrontUserRepository;
import com.bharatshop.shared.tenant.TenantContext;
import com.bharatshop.shared.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Storefront authentication service for customers.
 * Handles session-based authentication with email or phone+OTP login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorefrontAuthService {
    
    private final StorefrontUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    
    @Transactional
    public StorefrontAuthDto.AuthResponse register(StorefrontAuthDto.RegisterRequest request) {
        // Validate that at least email or phone is provided
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("Either email or phone number is required");
        }
        
        // Check if user already exists
        if (StringUtils.hasText(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        if (StringUtils.hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("User with this phone number already exists");
        }
        
        // Create new storefront user
        StorefrontUser user = StorefrontUser.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(StringUtils.hasText(request.getPassword()) ? 
                    passwordEncoder.encode(request.getPassword()) : null)
                .role(StorefrontUser.CustomerRole.CUSTOMER)
                .status(StorefrontUser.UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .tenantId(TenantContext.getCurrentTenant() != null ? UUID.fromString(TenantContext.getCurrentTenant()) : null)
                .build();
        
        user = userRepository.save(user);
        
        log.info("New storefront user registered: {} / {}", user.getEmail(), user.getPhone());
        
        // Generate session
        return generateAuthResponse(user, "Registration successful");
    }
    
    @Transactional
    public StorefrontAuthDto.AuthResponse login(StorefrontAuthDto.LoginRequest request) {
        StorefrontUser user = userRepository.findByEmailOrPhone(request.getEmailOrPhone())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check user status
        if (user.getStatus() == StorefrontUser.UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }
        
        if (user.getStatus() == StorefrontUser.UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Account is inactive");
        }
        
        // Handle password-based login
        if (StringUtils.hasText(request.getPassword())) {
            if (user.getPasswordHash() == null) {
                throw new IllegalArgumentException("Password not set for this account. Please use OTP login.");
            }
            
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            
            log.info("Storefront user logged in with password: {}", user.getEmail());
            return generateAuthResponse(user, "Login successful");
        }
        
        // Handle OTP-based login
        if (StringUtils.hasText(request.getOtp())) {
            if (!StringUtils.hasText(user.getPhone())) {
                throw new IllegalArgumentException("Phone number not available for OTP verification");
            }
            
            if (!otpService.verifyOtp(user.getPhone(), request.getOtp())) {
                throw new IllegalArgumentException("Invalid or expired OTP");
            }
            
            // Mark phone as verified
            user.setPhoneVerified(true);
            userRepository.save(user);
            
            log.info("Storefront user logged in with OTP: {}", user.getPhone());
            return generateAuthResponse(user, "Login successful");
        }
        
        throw new IllegalArgumentException("Either password or OTP is required");
    }
    
    public StorefrontAuthDto.MessageResponse sendOtp(StorefrontAuthDto.SendOtpRequest request) {
        StorefrontUser user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("User not found with this phone number"));
        
        String otp = otpService.generateOtp(request.getPhone());
        
        // Store OTP info in user record for tracking
        user.setLastOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        
        log.info("OTP sent to storefront user: {}", request.getPhone());
        
        return new StorefrontAuthDto.MessageResponse("OTP sent successfully", 
            new StorefrontAuthDto.MessageResponse("OTP: " + otp)); // Remove in production
    }
    
    public StorefrontAuthDto.MessageResponse verifyOtp(StorefrontAuthDto.VerifyOtpRequest request) {
        StorefrontUser user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("User not found with this phone number"));
        
        if (!otpService.verifyOtp(request.getPhone(), request.getOtp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        
        // Mark phone as verified
        user.setPhoneVerified(true);
        user.setLastOtp(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
        
        log.info("Phone verified for storefront user: {}", request.getPhone());
        
        return new StorefrontAuthDto.MessageResponse("Phone number verified successfully");
    }
    
    public void logout(String sessionId) {
        // In a real implementation, you would invalidate the session
        // For now, we'll just log the logout
        log.info("Storefront user logged out with session: {}", sessionId);
    }
    
    public StorefrontAuthDto.UserInfo getProfile(String userId) {
        UUID userUuid = UUID.fromString(userId);
        StorefrontUser user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return mapToUserInfo(user);
    }
    
    @Transactional
    public StorefrontAuthDto.UserInfo updateProfile(String userId, StorefrontAuthDto.UpdateProfileRequest request) {
        UUID userUuid = UUID.fromString(userId);
        StorefrontUser user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Update fields if provided
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Reset verification status
        }
        
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Phone number already exists");
            }
            user.setPhone(request.getPhone());
            user.setPhoneVerified(false); // Reset verification status
        }
        
        user = userRepository.save(user);
        
        log.info("Profile updated for storefront user: {}", user.getId());
        
        return mapToUserInfo(user);
    }
    
    @Transactional
    public StorefrontAuthDto.MessageResponse changePassword(String userId, StorefrontAuthDto.ChangePasswordRequest request) {
        UUID userUuid = UUID.fromString(userId);
        StorefrontUser user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify current password if user has one
        if (user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
        }
        
        // Set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed for storefront user: {}", user.getId());
        
        return new StorefrontAuthDto.MessageResponse("Password changed successfully");
    }
    
    private StorefrontAuthDto.AuthResponse generateAuthResponse(StorefrontUser user, String message) {
        // In a real implementation, you would generate a proper session ID
        String sessionId = "session_" + UUID.randomUUID().toString();
        
        return StorefrontAuthDto.AuthResponse.builder()
                .sessionId(sessionId)
                .user(mapToUserInfo(user))
                .message(message)
                .build();
    }
    
    private StorefrontAuthDto.UserInfo mapToUserInfo(StorefrontUser user) {
        return StorefrontAuthDto.UserInfo.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
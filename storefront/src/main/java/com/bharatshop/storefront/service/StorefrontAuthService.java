package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.auth.*;
import com.bharatshop.shared.entity.User;
import com.bharatshop.storefront.repository.StorefrontUserRepository;
import com.bharatshop.shared.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorefrontAuthService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(StorefrontAuthService.class);
    private final StorefrontUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    
    // Import the DTOs from app module
    // Note: These methods bridge the app module controller with storefront service

    public CustomerProfileResponse registerCustomer(RegisterCustomerRequest request) {
        log.info("Registering new customer with email: {}", request.getEmail());
        
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Check if user already exists
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        if (request.getPhone() != null && userRepository.existsByPhoneAndDeletedAtIsNull(request.getPhone())) {
            throw new IllegalArgumentException("User with this phone number already exists");
        }
        
        // Create new customer user
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .userType(User.UserType.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .phoneVerified(false)
                .emailVerified(false)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
        
        user = userRepository.save(user);
        log.info("Customer registered successfully with ID: {}", user.getId());
        
        return mapToProfileResponse(user);
    }

    public CustomerProfileResponse loginWithEmail(EmailLoginRequest request) {
        log.info("Email login attempt for: {}", request.getEmail());
        
        // Get user details
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Authenticate user manually
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("Account is disabled");
        }
        
        log.info("User logged in successfully: {}", user.getEmail());
        return mapToProfileResponse(user);
    }

    public String initiatePhoneLogin(PhoneLoginRequest request) {
        log.info("Phone login attempt for: {}", request.getPhone());
        
        // Check if user exists with this phone number
        User user = userRepository.findByPhoneAndDeletedAtIsNull(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this phone number"));
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is disabled");
        }
        
        // Generate and send OTP
        String otp = otpService.generateOtp(request.getPhone());
        log.info("OTP sent to phone: {}", request.getPhone());
        
        return "OTP sent to your phone number. Please verify to complete login.";
    }

    public CustomerProfileResponse verifyPhoneLogin(OtpVerificationRequest request) {
        log.info("Verifying OTP for phone: {}", request.getPhone());
        
        // Verify OTP
        if (!otpService.verifyOtp(request.getPhone(), request.getOtp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        
        // Get user
        User user = userRepository.findByPhoneAndDeletedAtIsNull(request.getPhone())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Mark phone as verified if not already
        if (!user.getPhoneVerified()) {
            user.setPhoneVerified(true);
            user = userRepository.save(user);
        }
        
        log.info("Phone login successful for: {}", request.getPhone());
        return mapToProfileResponse(user);
    }

    public void logout(String sessionId) {
        // In a session-based authentication system, you would:
        // 1. Invalidate the session
        // 2. Clear session data from storage
        // 3. Remove session cookies
        
        log.info("User logged out with session: {}", sessionId);
        
        // For now, we'll just log the logout
        // In production, implement session invalidation
    }

    public CustomerProfileResponse getCurrentUserProfile(String email) {
        log.info("Getting current user profile for: {}", email);
        
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return mapToProfileResponse(user);
    }

    public CustomerProfileResponse updateProfile(String email, CustomerProfileResponse profileUpdate) {
        log.info("Updating profile for user: {}", email);
        
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Update profile fields
        if (profileUpdate.getFirstName() != null) {
            user.setFirstName(profileUpdate.getFirstName());
        }
        if (profileUpdate.getLastName() != null) {
            user.setLastName(profileUpdate.getLastName());
        }
        if (profileUpdate.getAddress() != null) {
            user.setAddress(profileUpdate.getAddress());
        }
        if (profileUpdate.getCity() != null) {
            user.setCity(profileUpdate.getCity());
        }
        if (profileUpdate.getState() != null) {
            user.setState(profileUpdate.getState());
        }
        if (profileUpdate.getZipCode() != null) {
            user.setZipCode(profileUpdate.getZipCode());
        }
        if (profileUpdate.getCountry() != null) {
            user.setCountry(profileUpdate.getCountry());
        }
        
        user = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", email);
        
        return mapToProfileResponse(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by email first, then by phone
        User user = userRepository.findByEmailAndDeletedAtIsNull(username)
                .or(() -> userRepository.findByPhoneAndDeletedAtIsNull(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // Always use email as username
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList()))
                .accountExpired(false)
                .accountLocked(user.getStatus() == com.bharatshop.shared.entity.User.UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(user.getStatus() != com.bharatshop.shared.entity.User.UserStatus.ACTIVE)
                .build();
    }

    private CustomerProfileResponse mapToProfileResponse(com.bharatshop.shared.entity.User user) {
        return CustomerProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .enabled(user.getStatus() == com.bharatshop.shared.entity.User.UserStatus.ACTIVE)
                .phoneVerified(user.getPhoneVerified())
                .emailVerified(user.getEmailVerified())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getZipCode())
                .country(user.getCountry())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
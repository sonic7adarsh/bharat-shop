package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.auth.*;
import com.bharatshop.storefront.entity.StorefrontUser;
import com.bharatshop.storefront.repository.StorefrontUserRepository;
import com.bharatshop.shared.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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
    private final AuthenticationManager authenticationManager;
    
    // Import the DTOs from app module
    // Note: These methods bridge the app module controller with storefront service

    public CustomerProfileResponse registerCustomer(RegisterCustomerRequest request) {
        log.info("Registering new customer with email: {}", request.getEmail());
        
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("User with this phone number already exists");
        }
        
        // Create new customer user
        StorefrontUser user = StorefrontUser.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(StorefrontUser.StorefrontRole.CUSTOMER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
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
        
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        // Get user details
        StorefrontUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        log.info("User logged in successfully: {}", user.getEmail());
        return mapToProfileResponse(user);
    }

    public String initiatePhoneLogin(PhoneLoginRequest request) {
        log.info("Phone login attempt for: {}", request.getPhone());
        
        // Check if user exists with this phone number
        StorefrontUser user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this phone number"));
        
        if (!user.getEnabled()) {
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
        StorefrontUser user = userRepository.findByPhone(request.getPhone())
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
        
        StorefrontUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return mapToProfileResponse(user);
    }

    public CustomerProfileResponse updateProfile(String email, CustomerProfileResponse profileUpdate) {
        log.info("Updating profile for user: {}", email);
        
        StorefrontUser user = userRepository.findByEmail(email)
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
        StorefrontUser user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + username));
        
        return User.builder()
                .username(user.getEmail()) // Always use email as username
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(!user.getAccountNonExpired())
                .accountLocked(!user.getAccountNonLocked())
                .credentialsExpired(!user.getCredentialsNonExpired())
                .disabled(!user.getEnabled())
                .build();
    }

    private CustomerProfileResponse mapToProfileResponse(StorefrontUser user) {
        return CustomerProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.getEnabled())
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
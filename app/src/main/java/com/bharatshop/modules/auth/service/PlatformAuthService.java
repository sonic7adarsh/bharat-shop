package com.bharatshop.modules.auth.service;

import com.bharatshop.modules.auth.dto.PlatformAuthDto;
import com.bharatshop.modules.auth.entity.PlatformUser;
import com.bharatshop.modules.auth.repository.PlatformUserRepository;
import com.bharatshop.shared.tenant.TenantContext;
import com.bharatshop.shared.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Platform authentication service for vendors, admins, and staff.
 * Handles JWT-based authentication with access and refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAuthService {
    
    private final PlatformUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Transactional
    public PlatformAuthDto.AuthResponse registerVendor(PlatformAuthDto.RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        // Validate role (only VENDOR and STAFF can be registered via this endpoint)
        PlatformUser.PlatformRole role;
        try {
            role = PlatformUser.PlatformRole.valueOf(request.getRole().toUpperCase());
            if (role == PlatformUser.PlatformRole.ADMIN) {
                throw new IllegalArgumentException("Admin users cannot be registered via this endpoint");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }
        
        // Create new platform user
        PlatformUser user = PlatformUser.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .status(PlatformUser.UserStatus.PENDING_VERIFICATION)
                .tenantId(TenantContext.getCurrentTenant() != null ? UUID.fromString(TenantContext.getCurrentTenant()) : null)
                .build();
        
        user = userRepository.save(user);
        
        log.info("New platform user registered: {} with role: {}", user.getEmail(), user.getRole());
        
        // Generate tokens
        return generateAuthResponse(user);
    }
    
    @Transactional
    public PlatformAuthDto.AuthResponse login(PlatformAuthDto.LoginRequest request) {
        // Find user by email
        PlatformUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        
        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        // Check user status
        if (user.getStatus() == PlatformUser.UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("Account is suspended");
        }
        
        if (user.getStatus() == PlatformUser.UserStatus.INACTIVE) {
            throw new IllegalArgumentException("Account is inactive");
        }
        
        log.info("Platform user logged in: {} with role: {}", user.getEmail(), user.getRole());
        
        // Generate tokens
        return generateAuthResponse(user);
    }
    
    @Transactional
    public PlatformAuthDto.AuthResponse refreshToken(PlatformAuthDto.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Find user by refresh token first
        PlatformUser user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        
        // Create UserDetails for validation
        UserDetails userDetails = createUserDetails(user);
        
        // Validate refresh token
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        // Check user status
        if (user.getStatus() != PlatformUser.UserStatus.ACTIVE && 
            user.getStatus() != PlatformUser.UserStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException("Account is not active");
        }
        
        log.info("Refreshing token for platform user: {}", user.getEmail());
        
        // Generate new tokens
        return generateAuthResponse(user);
    }
    
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            userRepository.findByRefreshToken(refreshToken)
                    .ifPresent(user -> {
                        user.setRefreshToken(null);
                        userRepository.save(user);
                        log.info("Platform user logged out: {}", user.getEmail());
                    });
        }
    }
    
    public PlatformAuthDto.UserInfo getCurrentUser(String userId) {
        UUID userUuid = UUID.fromString(userId);
        PlatformUser user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return mapToUserInfo(user);
    }
    
    private PlatformAuthDto.AuthResponse generateAuthResponse(PlatformUser user) {
        UserDetails userDetails = createUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        // Save refresh token
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        
        return PlatformAuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .user(mapToUserInfo(user))
                .build();
    }
    
    private UserDetails createUserDetails(PlatformUser user) {
        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        
        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
    
    private PlatformAuthDto.UserInfo mapToUserInfo(PlatformUser user) {
        return PlatformAuthDto.UserInfo.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
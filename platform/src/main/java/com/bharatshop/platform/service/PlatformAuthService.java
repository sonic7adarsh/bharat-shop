package com.bharatshop.platform.service;

import com.bharatshop.platform.dto.auth.*;
import com.bharatshop.shared.entity.User;
import com.bharatshop.platform.repository.PlatformUserRepository;
import com.bharatshop.shared.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.HashSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformAuthService implements UserDetailsService {
    
    private static final Logger log = LoggerFactory.getLogger(PlatformAuthService.class);

    private final PlatformUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse registerVendor(RegisterVendorRequest request) {
        log.info("Registering new vendor with email: {}", request.getEmail());
        
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        // Create new vendor user
        Set<User.UserRole> roles = new HashSet<>();
        roles.add(User.UserRole.VENDOR);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        user = userRepository.save(user);
        log.info("Vendor registered successfully with ID: {}", user.getId());
        
        // Generate tokens
        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .roles(user.getRoles())
                        .enabled(user.getEnabled())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        // Get user details
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Authenticate user manually
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        if (!user.getEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }
        
        // Generate tokens
        UserDetails userDetails = loadUserByUsername(request.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        log.info("User logged in successfully: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .roles(user.getRoles())
                        .enabled(user.getEnabled())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");
        
        String refreshToken = request.getRefreshToken();
        String userEmail = jwtService.extractUsername(refreshToken);
        
        if (userEmail != null) {
            UserDetails userDetails = loadUserByUsername(userEmail);
            
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String newAccessToken = jwtService.generateAccessToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);
                
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                
                log.info("Token refreshed successfully for user: {}", userEmail);
                
                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.getAccessTokenExpiration())
                        .user(AuthResponse.UserInfo.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .roles(user.getRoles())
                                .enabled(user.getEnabled())
                                .createdAt(user.getCreatedAt())
                                .build())
                        .build();
            }
        }
        
        throw new IllegalArgumentException("Invalid refresh token");
    }

    public void logout(String token) {
        // In a production environment, you would typically:
        // 1. Add the token to a blacklist/revocation list
        // 2. Store blacklisted tokens in Redis with expiration
        // 3. Check blacklist during token validation
        
        String userEmail = jwtService.extractUsername(token);
        log.info("User logged out: {}", userEmail);
        
        // For now, we'll just log the logout
        // In production, implement token blacklisting
    }

    public AuthResponse.UserInfo getCurrentUser(String email) {
        log.info("Getting current user info for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList()))
                .accountExpired(!user.getAccountNonExpired())
                .accountLocked(!user.getAccountNonLocked())
                .credentialsExpired(!user.getCredentialsNonExpired())
                .disabled(!user.getEnabled())
                .build();
    }
}
package com.bharatshop.platform.service;

import com.bharatshop.platform.dto.auth.*;
import com.bharatshop.platform.entity.PlatformUser;
import com.bharatshop.platform.repository.PlatformUserRepository;
import com.bharatshop.shared.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformAuthService implements UserDetailsService {
    
    private static final Logger log = LoggerFactory.getLogger(PlatformAuthService.class);

    private final PlatformUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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
        PlatformUser user = PlatformUser.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(PlatformUser.PlatformRole.VENDOR))
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
        
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        // Get user details
        PlatformUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Generate tokens
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
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
                
                PlatformUser user = userRepository.findByEmail(userEmail)
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
        
        PlatformUser user = userRepository.findByEmail(email)
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
        PlatformUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
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
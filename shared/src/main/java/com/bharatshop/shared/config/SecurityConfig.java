package com.bharatshop.shared.config;

import com.bharatshop.shared.filter.JwtAuthenticationFilter;
import com.bharatshop.shared.filter.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationManager authenticationManager(
            @Qualifier("storefrontAuthService") UserDetailsService storefrontUserDetailsService,
            @Qualifier("platformAuthService") UserDetailsService platformUserDetailsService) {
        
        // Create DaoAuthenticationProvider for storefront users
        DaoAuthenticationProvider storefrontProvider = new DaoAuthenticationProvider();
        storefrontProvider.setUserDetailsService(storefrontUserDetailsService);
        storefrontProvider.setPasswordEncoder(passwordEncoder);
        
        // Create DaoAuthenticationProvider for platform users
        DaoAuthenticationProvider platformProvider = new DaoAuthenticationProvider();
        platformProvider.setUserDetailsService(platformUserDetailsService);
        platformProvider.setPasswordEncoder(passwordEncoder);
        
        // Return ProviderManager with both providers
        return new ProviderManager(storefrontProvider, platformProvider);
    }

    /**
     * Security configuration for Platform APIs (/api/platform/**)
     * Uses JWT-based authentication
     */
    @Bean
    @Order(1)
    public SecurityFilterChain platformSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/platform/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/platform/auth/register").permitAll()
                        .requestMatchers("/api/platform/auth/login").permitAll()
                        .requestMatchers("/api/platform/auth/refresh").permitAll()
                        .requestMatchers("/api/platform/auth/phone/**").permitAll()
                        .requestMatchers("/api/platform/health").permitAll()
                        .requestMatchers("/api/platform/swagger-ui/**").permitAll()
                        .requestMatchers("/api/platform/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Security configuration for Storefront APIs (/api/storefront/**)
     * Uses session-based authentication
     */
    @Bean
    @Order(2)
    public SecurityFilterChain storefrontSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/storefront/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/storefront/auth/register").permitAll()
                        .requestMatchers("/api/storefront/auth/login/**").permitAll()
                        .requestMatchers("/api/storefront/auth/phone/**").permitAll()
                        .requestMatchers("/api/storefront/auth/session/check").permitAll()
                        .requestMatchers("/api/storefront/products/**").permitAll()
                        .requestMatchers("/api/storefront/categories/**").permitAll()
                        .requestMatchers("/api/storefront/health").permitAll()
                        .requestMatchers("/api/storefront/swagger-ui/**").permitAll()
                        .requestMatchers("/api/storefront/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Default security configuration for other paths
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .build();
    }
}
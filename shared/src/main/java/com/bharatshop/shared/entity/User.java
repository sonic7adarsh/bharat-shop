package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Unified User entity that consolidates StorefrontUser, PlatformUser, and User entities.
 * Supports both customer and platform user types with role-based differentiation.
 * Uses session-based authentication for customers and JWT-based authentication for platform users.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    
    // Basic user information
    @Column(unique = true, nullable = false)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    @Column(nullable = false)
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @Column(nullable = false)
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String passwordHash;
    
    // User type and roles
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;
    
    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<UserRole> roles = new HashSet<>();
    
    // User status and verification
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(nullable = false)
    private Boolean accountNonExpired = true;
    
    @Column(nullable = false)
    private Boolean accountNonLocked = true;
    
    @Column(nullable = false)
    private Boolean credentialsNonExpired = true;
    
    // Verification fields (mainly for storefront users)
    @Column
    private Boolean emailVerified = false;
    
    @Column
    private Boolean phoneVerified = false;
    
    // OTP fields for storefront authentication
    @Column
    private String lastOtp;
    
    @Column
    private LocalDateTime otpExpiresAt;
    
    // JWT refresh token for platform users
    @Column
    private String refreshToken;
    
    // Address fields (mainly for storefront users)
    @Column
    private String address;
    
    @Column
    private String city;
    
    @Column
    private String state;
    
    @Column
    private String zipCode;
    
    @Column
    private String country;
    
    // Enums
    public enum UserType {
        CUSTOMER,    // Storefront users
        PLATFORM     // Platform users (admin, vendor, staff)
    }
    
    public enum UserRole {
        // Customer roles
        CUSTOMER,
        
        // Platform roles
        ADMIN,
        VENDOR,
        STAFF
    }
    
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION
    }
    
    // Helper methods
    public boolean isCustomer() {
        return userType == UserType.CUSTOMER;
    }
    
    public boolean isPlatformUser() {
        return userType == UserType.PLATFORM;
    }
    
    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }
    
    public void addRole(UserRole role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }
    
    public void removeRole(UserRole role) {
        if (roles != null) {
            roles.remove(role);
        }
    }
    
    public boolean isActive() {
        return status == UserStatus.ACTIVE && enabled;
    }
    
    public boolean isVerified() {
        if (isCustomer()) {
            return emailVerified || phoneVerified;
        }
        return true; // Platform users are verified by default
    }
}
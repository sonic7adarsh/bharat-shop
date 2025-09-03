package com.bharatshop.modules.auth.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Storefront user entity for customers.
 * Uses session-based authentication with email or phone+OTP login.
 */
@Entity(name = "AppStorefrontUser")
@Table(name = "storefront_users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontUser extends BaseEntity {
    
    @Column(unique = true)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    @Column
    private String firstName;
    
    @Column
    private String lastName;
    
    @Column
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerRole role = CustomerRole.CUSTOMER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column
    private boolean emailVerified = false;
    
    @Column
    private boolean phoneVerified = false;
    
    @Column
    private String lastOtp;
    
    @Column
    private java.time.LocalDateTime otpExpiresAt;
    
    public enum CustomerRole {
        CUSTOMER
    }
    
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION
    }
}
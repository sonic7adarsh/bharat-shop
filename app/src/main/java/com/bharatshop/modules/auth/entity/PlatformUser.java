package com.bharatshop.modules.auth.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Platform user entity for vendors, admins, and staff.
 * Uses JWT-based authentication with email/password login.
 */
@Entity
@Table(name = "platform_users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUser extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column
    private String refreshToken;
    
    public enum PlatformRole {
        ADMIN, VENDOR, STAFF
    }
    
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION
    }
    
    // Manual getters for compatibility
    public String getEmail() {
        return email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public PlatformRole getRole() {
        return role;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
}
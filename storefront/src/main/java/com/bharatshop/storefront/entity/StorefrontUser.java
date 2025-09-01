package com.bharatshop.storefront.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "storefront_users")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StorefrontUser extends BaseEntity {

    @Column(unique = true, nullable = false)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(unique = true)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number should be valid")
    private String phone;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorefrontRole role = StorefrontRole.CUSTOMER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    private Boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private Boolean phoneVerified = false;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    public enum StorefrontRole {
        CUSTOMER
    }
    
    // Manual getter methods for compilation compatibility
    public String getEmail() {
        return email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getPassword() {
        return password;
    }
    
    public StorefrontRole getRole() {
        return role;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public Boolean getAccountNonExpired() {
        return accountNonExpired;
    }
    
    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }
    
    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    public Boolean getPhoneVerified() {
        return phoneVerified;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getCity() {
        return city;
    }
    
    public String getState() {
        return state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public String getCountry() {
        return country;
    }
}
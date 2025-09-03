package com.bharatshop.storefront.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
@Entity(name = "StorefrontUser")
@Table(name = "storefront_users")
public class StorefrontUser extends BaseEntity {
    
    // Constructors
    public StorefrontUser() {
        super();
    }
    
    public StorefrontUser(String email, String phone, String password, StorefrontRole role) {
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.enabled = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.phoneVerified = false;
        this.emailVerified = false;
    }

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
    
    // Manual setter methods
    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setRole(StorefrontRole role) {
        this.role = role;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    // Builder method for compatibility
    public static StorefrontUserBuilder builder() {
        return new StorefrontUserBuilder();
    }
    
    public static class StorefrontUserBuilder {
        private StorefrontUser user = new StorefrontUser();
        
        public StorefrontUserBuilder email(String email) {
            user.email = email;
            return this;
        }
        
        public StorefrontUserBuilder phone(String phone) {
            user.phone = phone;
            return this;
        }
        
        public StorefrontUserBuilder password(String password) {
            user.password = password;
            return this;
        }
        
        public StorefrontUserBuilder role(StorefrontRole role) {
            user.role = role;
            return this;
        }
        
        public StorefrontUserBuilder enabled(Boolean enabled) {
            user.enabled = enabled;
            return this;
        }
        
        public StorefrontUserBuilder accountNonExpired(Boolean accountNonExpired) {
            user.accountNonExpired = accountNonExpired;
            return this;
        }
        
        public StorefrontUserBuilder accountNonLocked(Boolean accountNonLocked) {
            user.accountNonLocked = accountNonLocked;
            return this;
        }
        
        public StorefrontUserBuilder credentialsNonExpired(Boolean credentialsNonExpired) {
            user.credentialsNonExpired = credentialsNonExpired;
            return this;
        }
        
        public StorefrontUserBuilder phoneVerified(Boolean phoneVerified) {
            user.phoneVerified = phoneVerified;
            return this;
        }
        
        public StorefrontUserBuilder emailVerified(Boolean emailVerified) {
            user.emailVerified = emailVerified;
            return this;
        }
        
        public StorefrontUserBuilder firstName(String firstName) {
            user.firstName = firstName;
            return this;
        }
        
        public StorefrontUserBuilder lastName(String lastName) {
            user.lastName = lastName;
            return this;
        }
        
        public StorefrontUser build() {
            return user;
        }
    }
}
package com.bharatshop.storefront.dto.auth;

import com.bharatshop.shared.entity.User;
import com.bharatshop.shared.entity.User.UserRole;
import com.bharatshop.shared.entity.User.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// import java.util.UUID; // Replaced with Long

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer profile response")
public class CustomerProfileResponse {
    
    // Manual getter methods
    public Long getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public Boolean getEnabled() {
        return enabled;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Email address", example = "customer@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "User role", example = "CUSTOMER")
    private UserRole role;

    @Schema(description = "Account enabled status", example = "true")
    private Boolean enabled;

    @Schema(description = "Phone verification status", example = "true")
    private Boolean phoneVerified;

    @Schema(description = "Email verification status", example = "true")
    private Boolean emailVerified;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Address", example = "123 Main St")
    private String address;

    @Schema(description = "City", example = "New York")
    private String city;

    @Schema(description = "State", example = "NY")
    private String state;

    @Schema(description = "ZIP code", example = "10001")
    private String zipCode;

    @Schema(description = "Country", example = "USA")
    private String country;

    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Manual builder method for compilation compatibility
    public static CustomerProfileResponseBuilder builder() {
        return new CustomerProfileResponseBuilder();
    }
    
    public static class CustomerProfileResponseBuilder {
        private Long id;
        private String email;
        private String phone;
        private UserRole role;
        private Boolean enabled;
        private Boolean phoneVerified;
        private Boolean emailVerified;
        private String firstName;
        private String lastName;
        private String address;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public CustomerProfileResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public CustomerProfileResponseBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public CustomerProfileResponseBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public CustomerProfileResponseBuilder role(UserRole role) {
            this.role = role;
            return this;
        }
        
        public CustomerProfileResponseBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public CustomerProfileResponseBuilder phoneVerified(Boolean phoneVerified) {
            this.phoneVerified = phoneVerified;
            return this;
        }
        
        public CustomerProfileResponseBuilder emailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }
        
        public CustomerProfileResponseBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        public CustomerProfileResponseBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public CustomerProfileResponseBuilder address(String address) {
            this.address = address;
            return this;
        }
        
        public CustomerProfileResponseBuilder city(String city) {
            this.city = city;
            return this;
        }
        
        public CustomerProfileResponseBuilder state(String state) {
            this.state = state;
            return this;
        }
        
        public CustomerProfileResponseBuilder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }
        
        public CustomerProfileResponseBuilder country(String country) {
            this.country = country;
            return this;
        }
        
        public CustomerProfileResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public CustomerProfileResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public CustomerProfileResponse build() {
            CustomerProfileResponse response = new CustomerProfileResponse();
            response.id = this.id;
            response.email = this.email;
            response.phone = this.phone;
            response.role = this.role;
            response.enabled = this.enabled;
            response.phoneVerified = this.phoneVerified;
            response.emailVerified = this.emailVerified;
            response.firstName = this.firstName;
            response.lastName = this.lastName;
            response.address = this.address;
            response.city = this.city;
            response.state = this.state;
            response.zipCode = this.zipCode;
            response.country = this.country;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
        }
    }
}
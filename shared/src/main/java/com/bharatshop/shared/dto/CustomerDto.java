package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDto extends BaseDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;
    
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;
    
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Please provide a valid phone number")
    private String phone;
    
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    private Gender gender;
    
    @NotNull(message = "Status is required")
    private CustomerStatus status = CustomerStatus.ACTIVE;
    
    private Boolean emailVerified = false;
    private Boolean acceptsMarketing = false;
    
    @DecimalMin(value = "0.0", message = "Total spent cannot be negative")
    private BigDecimal totalSpent = BigDecimal.ZERO;
    
    @Min(value = 0, message = "Orders count cannot be negative")
    private Integer ordersCount = 0;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastOrderDate;
    
    @Valid
    private List<CustomerAddressDto> addresses;
    
    public enum Gender {
        MALE, FEMALE, OTHER
    }
    
    public enum CustomerStatus {
        ACTIVE, INACTIVE, BLOCKED
    }
    
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerAddressDto {
        private Long id;
        
        @NotNull(message = "Address type is required")
        private AddressType type = AddressType.BOTH;
        
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        private String firstName;
        
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        private String lastName;
        
        @Size(max = 255, message = "Company name cannot exceed 255 characters")
        private String company;
        
        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        private String addressLine1;
        
        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        private String addressLine2;
        
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        private String city;
        
        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        private String state;
        
        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Please provide a valid 6-digit postal code")
        private String postalCode;
        
        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country cannot exceed 100 characters")
        private String country = "India";
        
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Please provide a valid phone number")
        private String phone;
        
        private Boolean isDefault = false;
        
        public enum AddressType {
            BILLING, SHIPPING, BOTH
        }
    }
}
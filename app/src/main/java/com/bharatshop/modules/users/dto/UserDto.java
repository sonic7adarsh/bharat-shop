package com.bharatshop.modules.users.dto;

import com.bharatshop.shared.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
// import java.util.UUID; // Replaced with Long

/**
 * User DTOs using records for immutability.
 * Follows clean architecture principles with validation annotations.
 */
public final class UserDto {
    
    /**
     * User response DTO for API responses.
     */
    public record UserResponse(
            Long id,
            String email,
            String firstName,
            String lastName,
            String phone,
            User.UserRole role,
            User.UserStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
    
    /**
     * User creation request DTO.
     */
    public record CreateUserRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,
            
            @NotBlank(message = "First name is required")
            String firstName,
            
            @NotBlank(message = "Last name is required")
            String lastName,
            
            @NotBlank(message = "Password is required")
            String password,
            
            @NotBlank(message = "Phone is required")
            String phone,
            
            @NotNull(message = "Role is required")
            User.UserRole role
    ) {}
    
    /**
     * User update request DTO.
     */
    public record UpdateUserRequest(
            String firstName,
            String lastName,
            String phone,
            User.UserStatus status
    ) {}
    
    /**
     * User login request DTO.
     */
    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,
            
            @NotBlank(message = "Password is required")
            String password
    ) {}
    
    /**
     * User login response DTO.
     */
    public record LoginResponse(
            String token,
            UserResponse user
    ) {}
}
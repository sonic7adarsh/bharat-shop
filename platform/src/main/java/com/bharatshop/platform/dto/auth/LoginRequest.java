package com.bharatshop.platform.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request")
public class LoginRequest {

    @Schema(description = "Email address", example = "user@example.com")
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "Password", example = "password123")
    @NotBlank(message = "Password is required")
    private String password;
    
    // Manual getters for compilation compatibility
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
}
package com.bharatshop.platform.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vendor registration request")
public class RegisterVendorRequest {

    @Schema(description = "Email address", example = "vendor@example.com")
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "Password", example = "SecurePassword123!")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Schema(description = "Confirm password", example = "SecurePassword123!")
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    
    // Manual getters for compilation compatibility
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
}
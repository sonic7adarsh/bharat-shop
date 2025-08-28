package com.bharatshop.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for creating or updating a tenant")
public class TenantCreateDto {
    
    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    @Schema(description = "Name of the tenant", example = "Acme Corporation")
    private String name;
    
    @NotBlank(message = "Tenant code is required")
    @Size(min = 2, max = 50, message = "Tenant code must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Tenant code can only contain letters, numbers, hyphens, and underscores")
    @Schema(description = "Unique code for the tenant", example = "acme-corp")
    private String code;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Description of the tenant", example = "Leading provider of innovative solutions")
    private String description;
    
    // Manual getters for compilation compatibility
    public String getName() {
        return name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
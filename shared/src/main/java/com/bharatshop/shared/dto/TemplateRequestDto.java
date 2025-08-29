package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for Template operations.
 * Used for creating and updating templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequestDto {
    
    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Template configuration is required")
    private String config;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Size(max = 500, message = "Preview image URL must not exceed 500 characters")
    private String previewImage;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    private Integer sortOrder;
    
    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
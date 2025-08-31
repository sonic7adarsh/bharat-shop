package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Category DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDto extends BaseDto {
    
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 255, message = "Category name must be between 2 and 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    private Long parentId;
    
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;
    
    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;
    
    private Boolean isActive = true;
    
    // For nested category structure
    private CategoryDto parent;
    private List<CategoryDto> children;
    
    // For product count in category
    private Long productCount;
}
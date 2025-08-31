package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * OptionValue DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionValueDto extends BaseDto {
    
    private UUID id;
    
    @NotNull(message = "Option ID is required")
    private UUID optionId;
    
    @NotBlank(message = "Option value is required")
    @Size(min = 1, max = 255, message = "Option value must be between 1 and 255 characters")
    private String value;
    
    @Size(max = 255, message = "Display value cannot exceed 255 characters")
    private String displayValue;
    
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color code must be a valid hex color")
    private String colorCode;
    
    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;
    
    private Boolean isActive = true;
    
    // For nested structure with parent option
    private OptionDto option;
    
    // For variant count using this option value
    private Long variantCount;
}
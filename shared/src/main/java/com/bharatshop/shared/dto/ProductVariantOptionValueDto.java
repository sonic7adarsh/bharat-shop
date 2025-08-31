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
 * ProductVariantOptionValue DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantOptionValueDto extends BaseDto {
    
    private UUID id;
    
    @NotNull(message = "Variant ID is required")
    private UUID variantId;
    
    @NotNull(message = "Option ID is required")
    private UUID optionId;
    
    @NotNull(message = "Option value ID is required")
    private UUID optionValueId;
    
    // For nested structure with variant details
    private ProductVariantDto variant;
    
    // For nested structure with option details
    private OptionDto option;
    
    // For nested structure with option value details
    private OptionValueDto optionValue;
    
    // Convenience fields for display
    private String optionName; // e.g., "Color"
    private String optionValueName; // e.g., "Red"
    private String optionValueDisplayValue; // e.g., "Bright Red"
    private String colorCode; // e.g., "#FF0000" for color options
}
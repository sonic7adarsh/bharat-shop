package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * ProductVariantOptionValue DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantOptionValueDto {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @NotNull(message = "Variant ID is required")
    private Long variantId;
    
    @NotNull(message = "Option ID is required")
    private Long optionId;
    
    @NotNull(message = "Option value ID is required")
    private Long optionValueId;
    
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
    
    // Manual setters to fix Lombok issue
    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }
    
    public void setOptionValueName(String optionValueName) {
        this.optionValueName = optionValueName;
    }
    
    public void setOptionValueDisplayValue(String optionValueDisplayValue) {
        this.optionValueDisplayValue = optionValueDisplayValue;
    }
    
    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }
    
    public Long getOptionId() {
        return optionId;
    }
    
    public Long getOptionValueId() {
        return optionValueId;
    }
    
    public Long getVariantId() {
        return variantId;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }
    
    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
    
    public void setOptionValueId(Long optionValueId) {
        this.optionValueId = optionValueId;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
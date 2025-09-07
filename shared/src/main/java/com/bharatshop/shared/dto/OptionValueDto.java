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
 * OptionValue DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionValueDto {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @NotNull(message = "Option ID is required")
    private Long optionId;
    
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
    
    public String getValue() {
        return value;
    }
    
    public Long getOptionId() {
        return optionId;
    }
    
    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
}
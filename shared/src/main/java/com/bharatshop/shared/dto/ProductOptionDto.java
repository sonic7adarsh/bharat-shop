package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

/**
 * ProductOption DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOptionDto extends BaseDto {
    
    private UUID id;
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotNull(message = "Option ID is required")
    private UUID optionId;
    
    private Boolean isRequired = false;
    
    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;
    
    // For nested structure with option details
    private OptionDto option;
    
    // For nested structure with option values
    private List<OptionValueDto> optionValues;
    
    // For product details (when needed)
    private ProductDto product;
}
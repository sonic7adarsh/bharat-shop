package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ProductOption DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOptionDto {
    
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Option ID is required")
    private Long optionId;
    
    private Boolean isRequired = false;
    
    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;
    
    // For nested structure with option details
    private OptionDto option;
    
    // For nested structure with option values
    private List<OptionValueDto> optionValues;
    
    // For product details (when needed)
    private ProductDto product;
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public Long getOptionId() {
        return optionId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
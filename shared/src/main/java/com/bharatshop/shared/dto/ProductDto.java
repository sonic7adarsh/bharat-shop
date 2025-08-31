package com.bharatshop.shared.dto;

import com.bharatshop.shared.validation.ValidPrice;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Product DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto extends BaseDto {
    
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;
    
    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "SKU must contain only uppercase letters, numbers, hyphens, and underscores")
    @Size(min = 3, max = 100, message = "SKU must be between 3 and 100 characters")
    private String sku;
    
    @NotNull(message = "Price is required")
    @ValidPrice
    private BigDecimal price;
    
    @ValidPrice
    private BigDecimal comparePrice;
    
    @ValidPrice
    private BigDecimal costPrice;
    
    private Boolean trackInventory = true;
    
    @Min(value = 0, message = "Inventory quantity cannot be negative")
    private Integer inventoryQuantity = 0;
    
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold = 5;
    
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private BigDecimal weight;
    
    @Size(max = 100, message = "Dimensions cannot exceed 100 characters")
    private String dimensions;
    
    @NotNull(message = "Status is required")
    private ProductStatus status = ProductStatus.DRAFT;
    
    private Boolean featured = false;
    
    @Size(max = 255, message = "SEO title cannot exceed 255 characters")
    private String seoTitle;
    
    @Size(max = 500, message = "SEO description cannot exceed 500 characters")
    private String seoDescription;
    
    private List<CategoryDto> categories;
    private List<ProductImageDto> images;
    
    public enum ProductStatus {
        ACTIVE, INACTIVE, DRAFT
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductImageDto {
        private Long id;
        
        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        private String imageUrl;
        
        @Size(max = 255, message = "Alt text cannot exceed 255 characters")
        private String altText;
        
        private Integer sortOrder = 0;
        private Boolean isPrimary = false;
    }
}
package com.bharatshop.shared.dto;

import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.validation.ValidPrice;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ProductVariant DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantDto {
    
    private UUID id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @NotNull(message = "Product ID is required")
    private UUID productId;
    
    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "SKU must contain only uppercase letters, numbers, hyphens, and underscores")
    @Size(min = 3, max = 100, message = "SKU must be between 3 and 100 characters")
    private String sku;
    
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;
    
    @NotNull(message = "Price is required")
    @ValidPrice
    private BigDecimal price;
    
    @ValidPrice
    private BigDecimal salePrice;
    
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock = 0;
    
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private BigDecimal weight;
    
    private String dimensions;
    
    // JSON attributes as Map for easier handling in DTOs
    private Map<String, Object> attributes;
    
    private UUID imageId;
    
    private Boolean isDefault = false;
    
    @NotNull(message = "Status is required")
    private ProductVariant.VariantStatus status = ProductVariant.VariantStatus.ACTIVE;
    
    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;
    
    // For nested structure with product details
    private ProductDto product;
    
    // For nested structure with option values
    private List<ProductVariantOptionValueDto> optionValues;
    
    // For image details
    private String imageUrl;
    
    // Computed fields for convenience
    private BigDecimal effectivePrice; // sale price if available, otherwise regular price
    private Boolean isOnSale; // true if sale price is set and less than regular price
    private BigDecimal discountAmount; // difference between price and sale price
    private Double discountPercentage; // discount percentage
    private Boolean isInStock; // true if stock > 0
    private Boolean isLowStock; // true if stock is below threshold
    
    // For variant combination display
    private String variantTitle; // e.g., "Red / Large"
    private List<String> optionValueNames; // e.g., ["Red", "Large"]
}
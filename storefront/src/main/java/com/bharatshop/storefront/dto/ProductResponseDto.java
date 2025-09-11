package com.bharatshop.storefront.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
// import java.util.UUID; // Replaced with Long
import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.dto.ProductOptionDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product response data")
public class ProductResponseDto {
    
    @Schema(description = "Product ID", example = "1")
    private Long id;
    
    @Schema(description = "Product name", example = "iPhone 15 Pro")
    private String name;
    
    @Schema(description = "Product description", example = "Latest iPhone with advanced features")
    private String description;
    
    @Schema(description = "Product price", example = "999.99")
    private BigDecimal price;
    
    @Schema(description = "Discounted price if applicable", example = "899.99")
    private BigDecimal discountPrice;
    
    @Schema(description = "Product category", example = "Electronics")
    private String category;
    
    @Schema(description = "Product brand", example = "Apple")
    private String brand;
    
    @Schema(description = "Stock keeping unit", example = "IPH15PRO128")
    private String sku;
    
    @Schema(description = "Available stock quantity", example = "50")
    private Integer stockQuantity;
    
    @Schema(description = "Product image URLs")
    private List<String> imageUrls;
    
    @Schema(description = "Product image thumbnails by size")
    private List<Map<String, String>> thumbnailUrls;
    
    @Schema(description = "Responsive image srcset for each image")
    private List<String> srcsets;
    
    @Schema(description = "Whether product is featured", example = "true")
    private Boolean featured;
    
    @Schema(description = "Whether product is active", example = "true")
    private Boolean active;
    
    @Schema(description = "Product rating", example = "4.5")
    private BigDecimal rating;
    
    @Schema(description = "Number of reviews", example = "125")
    private Integer reviewCount;
    
    @Schema(description = "Meta title for SEO")
    private String metaTitle;
    
    @Schema(description = "Meta description for SEO")
    private String metaDescription;
    
    @Schema(description = "URL slug", example = "iphone-15-pro")
    private String slug;
    
    @Schema(description = "Product weight in kg", example = "0.221")
    private BigDecimal weight;
    
    @Schema(description = "Product dimensions", example = "159.9 x 76.7 x 8.25 mm")
    private String dimensions;
    
    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Default/selected product variant")
    private ProductVariantDto defaultVariant;
    
    @Schema(description = "All available product variants")
    private List<ProductVariantDto> variants;
    
    @Schema(description = "Product options (size, color, etc.)")
    private List<ProductOptionDto> options;
    
    @Schema(description = "Whether product has variants")
    private Boolean hasVariants;
    
    @Schema(description = "Whether product is in stock")
    public Boolean getInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
    
    @Schema(description = "Whether product has discount")
    public Boolean getHasDiscount() {
        return discountPrice != null && discountPrice.compareTo(price) < 0;
    }
    
    @Schema(description = "Discount percentage if applicable")
    public BigDecimal getDiscountPercentage() {
        if (discountPrice == null || price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = price.subtract(discountPrice);
        return discount.divide(price, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    // Manual builder method for compilation compatibility
    public static ProductResponseDtoBuilder builder() {
        return new ProductResponseDtoBuilder();
    }
    
    public static class ProductResponseDtoBuilder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private String category;
        private String brand;
        private String sku;
        private Integer stockQuantity;
        private List<String> imageUrls;
        private List<Map<String, String>> thumbnailUrls;
        private List<String> srcsets;
        private Boolean featured;
        private Boolean active;
        private BigDecimal rating;
        private Integer reviewCount;
        private String metaTitle;
        private String metaDescription;
        private String slug;
        private BigDecimal weight;
        private String dimensions;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private ProductVariantDto defaultVariant;
        private List<ProductVariantDto> variants;
        private List<ProductOptionDto> options;
        private Boolean hasVariants;
        
        public ProductResponseDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public ProductResponseDtoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public ProductResponseDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public ProductResponseDtoBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }
        
        public ProductResponseDtoBuilder discountPrice(BigDecimal discountPrice) {
            this.discountPrice = discountPrice;
            return this;
        }
        
        public ProductResponseDtoBuilder category(String category) {
            this.category = category;
            return this;
        }
        
        public ProductResponseDtoBuilder brand(String brand) {
            this.brand = brand;
            return this;
        }
        
        public ProductResponseDtoBuilder sku(String sku) {
            this.sku = sku;
            return this;
        }
        
        public ProductResponseDtoBuilder stockQuantity(Integer stockQuantity) {
            this.stockQuantity = stockQuantity;
            return this;
        }
        
        public ProductResponseDtoBuilder imageUrls(List<String> imageUrls) {
            this.imageUrls = imageUrls;
            return this;
        }
        
        public ProductResponseDtoBuilder thumbnailUrls(List<Map<String, String>> thumbnailUrls) {
            this.thumbnailUrls = thumbnailUrls;
            return this;
        }
        
        public ProductResponseDtoBuilder srcsets(List<String> srcsets) {
            this.srcsets = srcsets;
            return this;
        }
        
        public ProductResponseDtoBuilder featured(Boolean featured) {
            this.featured = featured;
            return this;
        }
        
        public ProductResponseDtoBuilder active(Boolean active) {
            this.active = active;
            return this;
        }
        
        public ProductResponseDtoBuilder rating(BigDecimal rating) {
            this.rating = rating;
            return this;
        }
        
        public ProductResponseDtoBuilder reviewCount(Integer reviewCount) {
            this.reviewCount = reviewCount;
            return this;
        }
        
        public ProductResponseDtoBuilder metaTitle(String metaTitle) {
            this.metaTitle = metaTitle;
            return this;
        }
        
        public ProductResponseDtoBuilder metaDescription(String metaDescription) {
            this.metaDescription = metaDescription;
            return this;
        }
        
        public ProductResponseDtoBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }
        
        public ProductResponseDtoBuilder weight(BigDecimal weight) {
            this.weight = weight;
            return this;
        }
        
        public ProductResponseDtoBuilder dimensions(String dimensions) {
            this.dimensions = dimensions;
            return this;
        }
        
        public ProductResponseDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public ProductResponseDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public ProductResponseDtoBuilder defaultVariant(ProductVariantDto defaultVariant) {
            this.defaultVariant = defaultVariant;
            return this;
        }
        
        public ProductResponseDtoBuilder variants(List<ProductVariantDto> variants) {
            this.variants = variants;
            return this;
        }
        
        public ProductResponseDtoBuilder options(List<ProductOptionDto> options) {
            this.options = options;
            return this;
        }
        
        public ProductResponseDtoBuilder hasVariants(Boolean hasVariants) {
            this.hasVariants = hasVariants;
            return this;
        }
        
        public ProductResponseDto build() {
            ProductResponseDto dto = new ProductResponseDto();
            dto.id = this.id;
            dto.name = this.name;
            dto.description = this.description;
            dto.price = this.price;
            dto.discountPrice = this.discountPrice;
            dto.category = this.category;
            dto.brand = this.brand;
            dto.sku = this.sku;
            dto.stockQuantity = this.stockQuantity;
            dto.imageUrls = this.imageUrls;
            dto.thumbnailUrls = this.thumbnailUrls;
            dto.srcsets = this.srcsets;
            dto.featured = this.featured;
            dto.active = this.active;
            dto.rating = this.rating;
            dto.reviewCount = this.reviewCount;
            dto.metaTitle = this.metaTitle;
            dto.metaDescription = this.metaDescription;
            dto.slug = this.slug;
            dto.weight = this.weight;
            dto.dimensions = this.dimensions;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.defaultVariant = this.defaultVariant;
            dto.variants = this.variants;
            dto.options = this.options;
            dto.hasVariants = this.hasVariants;
            return dto;
        }
    }
}
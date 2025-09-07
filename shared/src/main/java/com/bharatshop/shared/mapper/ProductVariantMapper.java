package com.bharatshop.shared.mapper;

import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.entity.ProductVariant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manual mapper for ProductVariant entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Component
public class ProductVariantMapper {
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * Maps ProductVariant entity to ProductVariantDto.
     */
    public ProductVariantDto toDto(ProductVariant productVariant) {
        if (productVariant == null) {
            return null;
        }
        
        ProductVariantDto dto = new ProductVariantDto();
        dto.setId(productVariant.getId());
        dto.setProductId(productVariant.getProductId());
        dto.setSku(productVariant.getSku());
        dto.setBarcode(productVariant.getBarcode());
        dto.setPrice(productVariant.getPrice());
        dto.setSalePrice(productVariant.getSalePrice());
        dto.setStock(productVariant.getStock());
        dto.setWeight(productVariant.getWeight());
        dto.setDimensions(productVariant.getDimensions());
        dto.setAttributes(jsonToMap(productVariant.getAttributes()));
        dto.setStatus(productVariant.getStatus());
        dto.setCreatedAt(productVariant.getCreatedAt());
        dto.setUpdatedAt(productVariant.getUpdatedAt());
        
        // Ignored fields: product, optionValues, imageUrl, effectivePrice, isOnSale, 
        // discountAmount, discountPercentage, isInStock, isLowStock, variantTitle, optionValueNames
        
        return dto;
    }
    
    /**
     * Maps ProductVariantDto to ProductVariant entity.
     */
    public ProductVariant toEntity(ProductVariantDto productVariantDto) {
        if (productVariantDto == null) {
            return null;
        }
        
        ProductVariant entity = new ProductVariant();
        entity.setProductId(productVariantDto.getProductId());
        entity.setSku(productVariantDto.getSku());
        entity.setBarcode(productVariantDto.getBarcode());
        entity.setPrice(productVariantDto.getPrice());
        entity.setSalePrice(productVariantDto.getSalePrice());
        entity.setStock(productVariantDto.getStock());
        entity.setWeight(productVariantDto.getWeight());
        entity.setDimensions(productVariantDto.getDimensions());
        entity.setAttributes(mapToJson(productVariantDto.getAttributes()));
        entity.setStatus(productVariantDto.getStatus());
        
        // Ignored fields: id, tenantId, createdAt, updatedAt, deletedAt, product, optionValues
        
        return entity;
    }
    
    /**
     * Updates existing ProductVariant entity with data from ProductVariantDto.
     */
    public void updateEntity(ProductVariantDto productVariantDto, ProductVariant productVariant) {
        if (productVariantDto == null || productVariant == null) {
            return;
        }
        
        if (productVariantDto.getSku() != null) {
            productVariant.setSku(productVariantDto.getSku());
        }
        if (productVariantDto.getBarcode() != null) {
            productVariant.setBarcode(productVariantDto.getBarcode());
        }
        if (productVariantDto.getPrice() != null) {
            productVariant.setPrice(productVariantDto.getPrice());
        }
        if (productVariantDto.getSalePrice() != null) {
            productVariant.setSalePrice(productVariantDto.getSalePrice());
        }
        if (productVariantDto.getStock() != null) {
            productVariant.setStock(productVariantDto.getStock());
        }
        if (productVariantDto.getWeight() != null) {
            productVariant.setWeight(productVariantDto.getWeight());
        }
        if (productVariantDto.getDimensions() != null) {
            productVariant.setDimensions(productVariantDto.getDimensions());
        }
        if (productVariantDto.getAttributes() != null) {
            productVariant.setAttributes(mapToJson(productVariantDto.getAttributes()));
        }
        if (productVariantDto.getStatus() != null) {
            productVariant.setStatus(productVariantDto.getStatus());
        }
        
        // Ignored fields: id, productId, tenantId, createdAt, updatedAt, deletedAt, product, optionValues
    }
    
    /**
     * Maps list of ProductVariants to list of ProductVariantDtos.
     */
    public List<ProductVariantDto> toDtoList(List<ProductVariant> productVariants) {
        if (productVariants == null) {
            return null;
        }
        return productVariants.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps list of ProductVariantDtos to list of ProductVariant entities.
     */
    public List<ProductVariant> toEntityList(List<ProductVariantDto> productVariantDtos) {
        if (productVariantDtos == null) {
            return null;
        }
        return productVariantDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps ProductVariantDto with computed fields for display.
     */
    public ProductVariantDto toDtoWithComputedFields(ProductVariant productVariant) {
        ProductVariantDto dto = toDto(productVariant);
        
        // Set computed fields
        dto.setEffectivePrice(calculateEffectivePrice(dto.getPrice(), dto.getSalePrice()));
        dto.setIsOnSale(isOnSale(dto.getPrice(), dto.getSalePrice()));
        dto.setDiscountAmount(calculateDiscountAmount(dto.getPrice(), dto.getSalePrice()));
        dto.setDiscountPercentage(calculateDiscountPercentage(dto.getPrice(), dto.getSalePrice()));
        dto.setIsInStock(dto.getStock() != null && dto.getStock() > 0);
        dto.setIsLowStock(dto.getStock() != null && dto.getStock() > 0 && dto.getStock() <= 5); // Default threshold
        
        return dto;
    }
    
    /**
     * Converts JSON string to Map.
     */
    protected Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * Converts Map to JSON string.
     */
    protected String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    // Helper methods for computed fields
    private BigDecimal calculateEffectivePrice(BigDecimal price, BigDecimal salePrice) {
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice.compareTo(price) < 0) {
            return salePrice;
        }
        return price;
    }
    
    private Boolean isOnSale(BigDecimal price, BigDecimal salePrice) {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice.compareTo(price) < 0;
    }
    
    private BigDecimal calculateDiscountAmount(BigDecimal price, BigDecimal salePrice) {
        if (isOnSale(price, salePrice)) {
            return price.subtract(salePrice);
        }
        return BigDecimal.ZERO;
    }
    
    private Double calculateDiscountPercentage(BigDecimal price, BigDecimal salePrice) {
        if (isOnSale(price, salePrice) && price.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = price.subtract(salePrice);
            return discount.divide(price, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        return 0.0;
    }
}
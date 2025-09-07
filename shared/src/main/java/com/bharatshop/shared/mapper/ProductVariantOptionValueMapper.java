package com.bharatshop.shared.mapper;

import com.bharatshop.shared.dto.ProductVariantOptionValueDto;
import com.bharatshop.shared.entity.ProductVariantOptionValue;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for ProductVariantOptionValue entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Component
public class ProductVariantOptionValueMapper {
    
    /**
     * Maps ProductVariantOptionValue entity to ProductVariantOptionValueDto.
     */
    public ProductVariantOptionValueDto toDto(ProductVariantOptionValue productVariantOptionValue) {
        if (productVariantOptionValue == null) {
            return null;
        }
        
        ProductVariantOptionValueDto dto = new ProductVariantOptionValueDto();
        dto.setId(productVariantOptionValue.getId());
        dto.setVariantId(productVariantOptionValue.getVariantId());
        dto.setOptionId(productVariantOptionValue.getOptionId());
        dto.setOptionValueId(productVariantOptionValue.getOptionValueId());
        dto.setCreatedAt(productVariantOptionValue.getCreatedAt());
        dto.setUpdatedAt(productVariantOptionValue.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Maps ProductVariantOptionValueDto to ProductVariantOptionValue entity.
     */
    public ProductVariantOptionValue toEntity(ProductVariantOptionValueDto productVariantOptionValueDto) {
        if (productVariantOptionValueDto == null) {
            return null;
        }
        
        ProductVariantOptionValue entity = new ProductVariantOptionValue();
        entity.setVariantId(productVariantOptionValueDto.getVariantId());
        entity.setOptionId(productVariantOptionValueDto.getOptionId());
        entity.setOptionValueId(productVariantOptionValueDto.getOptionValueId());
        
        return entity;
    }
    
    /**
     * Updates existing ProductVariantOptionValue entity with ProductVariantOptionValueDto data.
     */
    public void updateEntity(ProductVariantOptionValueDto productVariantOptionValueDto, ProductVariantOptionValue productVariantOptionValue) {
        if (productVariantOptionValueDto == null || productVariantOptionValue == null) {
            return;
        }
        
        // Note: Key fields (variantId, optionId, optionValueId) are not updated as they should not be changed after creation
    }
    
    /**
     * Maps list of ProductVariantOptionValue entities to list of ProductVariantOptionValueDtos.
     */
    public List<ProductVariantOptionValueDto> toDtoList(List<ProductVariantOptionValue> productVariantOptionValues) {
        if (productVariantOptionValues == null) {
            return null;
        }
        return productVariantOptionValues.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps list of ProductVariantOptionValueDtos to list of ProductVariantOptionValue entities.
     */
    public List<ProductVariantOptionValue> toEntityList(List<ProductVariantOptionValueDto> productVariantOptionValueDtos) {
        if (productVariantOptionValueDtos == null) {
            return null;
        }
        return productVariantOptionValueDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps ProductVariantOptionValueDto with computed fields for display.
     */
    public ProductVariantOptionValueDto toDtoWithComputedFields(ProductVariantOptionValue entity) {
        ProductVariantOptionValueDto dto = toDto(entity);
        
        // Set computed fields from related entities
        if (entity.getOption() != null) {
            dto.setOptionName(entity.getOption().getName());
        }
        
        if (entity.getOptionValue() != null) {
            dto.setOptionValueName(entity.getOptionValue().getValue());
            dto.setOptionValueDisplayValue(entity.getOptionValue().getDisplayValue());
            dto.setColorCode(entity.getOptionValue().getColorCode());
        }
        
        return dto;
    }
}
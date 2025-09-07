package com.bharatshop.shared.mapper;

import com.bharatshop.shared.dto.ProductOptionDto;
import com.bharatshop.shared.entity.ProductOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for ProductOption entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Component
public class ProductOptionMapper {
    
    /**
     * Maps ProductOption entity to ProductOptionDto.
     */
    public ProductOptionDto toDto(ProductOption productOption) {
        if (productOption == null) {
            return null;
        }
        
        ProductOptionDto dto = new ProductOptionDto();
        dto.setId(productOption.getId());
        dto.setProductId(productOption.getProductId());
        dto.setOptionId(productOption.getOptionId());
        dto.setIsRequired(productOption.getIsRequired());
        dto.setSortOrder(productOption.getSortOrder());
        // option, optionValues, and product are ignored as per original mapping
        
        return dto;
    }
    
    /**
     * Maps ProductOptionDto to ProductOption entity.
     */
    public ProductOption toEntity(ProductOptionDto productOptionDto) {
        if (productOptionDto == null) {
            return null;
        }
        
        ProductOption entity = new ProductOption();
        entity.setProductId(productOptionDto.getProductId());
        entity.setOptionId(productOptionDto.getOptionId());
        entity.setIsRequired(productOptionDto.getIsRequired());
        entity.setSortOrder(productOptionDto.getSortOrder());
        // id, tenantId, audit fields, product, and option are ignored as per original mapping
        
        return entity;
    }
    
    /**
     * Updates existing ProductOption entity with ProductOptionDto data.
     */
    public void updateEntity(ProductOptionDto productOptionDto, ProductOption productOption) {
        if (productOptionDto == null || productOption == null) {
            return;
        }
        
        if (productOptionDto.getIsRequired() != null) {
            productOption.setIsRequired(productOptionDto.getIsRequired());
        }
        if (productOptionDto.getSortOrder() != null) {
            productOption.setSortOrder(productOptionDto.getSortOrder());
        }
        // Note: isActive field is not available in ProductOption entity
        // id, productId, optionId, tenantId, audit fields, product, and option are ignored as per original mapping
    }
    
    /**
     * Maps list of ProductOption entities to list of ProductOptionDtos.
     */
    public List<ProductOptionDto> toDtoList(List<ProductOption> productOptions) {
        if (productOptions == null) {
            return null;
        }
        
        return productOptions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps list of ProductOptionDtos to list of ProductOption entities.
     */
    public List<ProductOption> toEntityList(List<ProductOptionDto> productOptionDtos) {
        if (productOptionDtos == null) {
            return null;
        }
        
        return productOptionDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
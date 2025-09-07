package com.bharatshop.shared.mapper;

import com.bharatshop.shared.dto.OptionValueDto;
import com.bharatshop.shared.entity.OptionValue;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for OptionValue entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Component
public class OptionValueMapper {
    
    /**
     * Maps OptionValue entity to OptionValueDto.
     */
    public OptionValueDto toDto(OptionValue optionValue) {
        if (optionValue == null) {
            return null;
        }
        
        OptionValueDto dto = new OptionValueDto();
        dto.setId(optionValue.getId());
        dto.setOptionId(optionValue.getOptionId());
        dto.setValue(optionValue.getValue());
        dto.setDisplayValue(optionValue.getDisplayValue());
        dto.setColorCode(optionValue.getColorCode());

        dto.setSortOrder(optionValue.getSortOrder());
        dto.setIsActive(optionValue.getIsActive());
        // option and variantCount are ignored as per original mapping
        
        return dto;
    }
    
    /**
     * Maps OptionValueDto to OptionValue entity.
     */
    public OptionValue toEntity(OptionValueDto optionValueDto) {
        if (optionValueDto == null) {
            return null;
        }
        
        OptionValue entity = new OptionValue();
        entity.setOptionId(optionValueDto.getOptionId());
        entity.setValue(optionValueDto.getValue());
        entity.setDisplayValue(optionValueDto.getDisplayValue());
        entity.setColorCode(optionValueDto.getColorCode());

        entity.setSortOrder(optionValueDto.getSortOrder());
        entity.setIsActive(optionValueDto.getIsActive());
        // id, tenantId, audit fields, and option are ignored as per original mapping
        
        return entity;
    }
    
    /**
     * Updates existing OptionValue entity with OptionValueDto data.
     */
    public void updateEntity(OptionValueDto optionValueDto, OptionValue optionValue) {
        if (optionValueDto == null || optionValue == null) {
            return;
        }
        
        if (optionValueDto.getValue() != null) {
            optionValue.setValue(optionValueDto.getValue());
        }
        if (optionValueDto.getDisplayValue() != null) {
            optionValue.setDisplayValue(optionValueDto.getDisplayValue());
        }
        if (optionValueDto.getColorCode() != null) {
            optionValue.setColorCode(optionValueDto.getColorCode());
        }

        if (optionValueDto.getSortOrder() != null) {
            optionValue.setSortOrder(optionValueDto.getSortOrder());
        }
        if (optionValueDto.getIsActive() != null) {
            optionValue.setIsActive(optionValueDto.getIsActive());
        }
        // id, optionId, tenantId, audit fields, and option are ignored as per original mapping
    }
    
    /**
     * Maps list of OptionValue entities to list of OptionValueDtos.
     */
    public List<OptionValueDto> toDtoList(List<OptionValue> optionValues) {
        if (optionValues == null) {
            return null;
        }
        
        return optionValues.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps list of OptionValueDtos to list of OptionValue entities.
     */
    public List<OptionValue> toEntityList(List<OptionValueDto> optionValueDtos) {
        if (optionValueDtos == null) {
            return null;
        }
        
        return optionValueDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
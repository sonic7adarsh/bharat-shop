package com.bharatshop.shared.mapper;

import com.bharatshop.shared.dto.OptionDto;
import com.bharatshop.shared.entity.Option;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for Option entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Component
public class OptionMapper {
    
    /**
     * Maps Option entity to OptionDto.
     */
    public OptionDto toDto(Option option) {
        if (option == null) {
            return null;
        }
        
        OptionDto dto = new OptionDto();
        dto.setId(option.getId());
        dto.setName(option.getName());
        dto.setDisplayName(option.getDisplayName());
        dto.setType(option.getType());
        dto.setIsRequired(option.getIsRequired());
        dto.setSortOrder(option.getSortOrder());
        dto.setIsActive(option.getIsActive());
        // optionValues and productCount are ignored as per original mapping
        
        return dto;
    }
    
    /**
     * Maps OptionDto to Option entity.
     */
    public Option toEntity(OptionDto optionDto) {
        if (optionDto == null) {
            return null;
        }
        
        Option entity = new Option();
        entity.setName(optionDto.getName());
        entity.setDisplayName(optionDto.getDisplayName());
        entity.setType(optionDto.getType());
        entity.setIsRequired(optionDto.getIsRequired());
        entity.setSortOrder(optionDto.getSortOrder());
        entity.setIsActive(optionDto.getIsActive());
        // id, tenantId, audit fields, and optionValues are ignored as per original mapping
        
        return entity;
    }
    
    /**
     * Updates existing Option entity with OptionDto data.
     */
    public void updateEntity(OptionDto optionDto, Option option) {
        if (optionDto == null || option == null) {
            return;
        }
        
        if (optionDto.getName() != null) {
            option.setName(optionDto.getName());
        }
        if (optionDto.getDisplayName() != null) {
            option.setDisplayName(optionDto.getDisplayName());
        }
        if (optionDto.getType() != null) {
            option.setType(optionDto.getType());
        }
        if (optionDto.getIsRequired() != null) {
            option.setIsRequired(optionDto.getIsRequired());
        }
        if (optionDto.getSortOrder() != null) {
            option.setSortOrder(optionDto.getSortOrder());
        }
        if (optionDto.getIsActive() != null) {
            option.setIsActive(optionDto.getIsActive());
        }
        // id, tenantId, audit fields, and optionValues are ignored as per original mapping
    }
    
    /**
     * Maps list of Option entities to list of OptionDtos.
     */
    public List<OptionDto> toDtoList(List<Option> options) {
        if (options == null) {
            return null;
        }
        
        return options.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps list of OptionDtos to list of Option entities.
     */
    public List<Option> toEntityList(List<OptionDto> optionDtos) {
        if (optionDtos == null) {
            return null;
        }
        
        return optionDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
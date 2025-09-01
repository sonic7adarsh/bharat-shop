package com.bharatshop.shared.dto;

import com.bharatshop.shared.entity.Option;
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
 * Option DTO with validation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionDto {
    
    private UUID id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @NotBlank(message = "Option name is required")
    @Size(min = 2, max = 100, message = "Option name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
    private String displayName;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @NotNull(message = "Option type is required")
    private Option.OptionType type;
    
    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;
    
    private Boolean isRequired = false;
    
    private Boolean isActive = true;
    
    // For nested structure with option values
    private List<OptionValueDto> optionValues;
    
    // For product count using this option
    private Long productCount;
}
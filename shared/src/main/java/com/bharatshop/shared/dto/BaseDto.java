package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Base DTO class with common fields for all DTOs
 */
@Data
@SuperBuilder
public abstract class BaseDto {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    protected BaseDto() {
        // Default constructor for Jackson
    }
}
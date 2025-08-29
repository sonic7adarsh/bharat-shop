package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Template data.
 * Contains template information for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponseDto {
    
    private UUID id;
    private String name;
    private String config;
    private String description;
    private String previewImage;
    private String category;
    private Integer sortOrder;
    private Boolean isActive;
    private UUID tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
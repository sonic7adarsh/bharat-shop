package com.bharatshop.platform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for tenant response")
public class TenantResponseDto {
    
    @Schema(description = "Unique identifier of the tenant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Name of the tenant", example = "Acme Corporation")
    private String name;
    
    @Schema(description = "Unique code for the tenant", example = "acme-corp")
    private String code;
    
    @Schema(description = "Description of the tenant", example = "Leading provider of innovative solutions")
    private String description;
    
    @Schema(description = "Whether the tenant is active", example = "true")
    private boolean active;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "When the tenant was created", example = "2024-01-15 10:30:00")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "When the tenant was last updated", example = "2024-01-15 10:30:00")
    private LocalDateTime updatedAt;
    
    // Manual builder method for compilation compatibility
    public static TenantResponseDtoBuilder builder() {
        return new TenantResponseDtoBuilder();
    }
    
    public static class TenantResponseDtoBuilder {
        private UUID id;
        private String name;
        private String code;
        private String description;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public TenantResponseDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public TenantResponseDtoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public TenantResponseDtoBuilder code(String code) {
            this.code = code;
            return this;
        }
        
        public TenantResponseDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public TenantResponseDtoBuilder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public TenantResponseDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public TenantResponseDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public TenantResponseDto build() {
            TenantResponseDto dto = new TenantResponseDto();
            dto.id = this.id;
            dto.name = this.name;
            dto.code = this.code;
            dto.description = this.description;
            dto.active = this.active;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            return dto;
        }
    }
}
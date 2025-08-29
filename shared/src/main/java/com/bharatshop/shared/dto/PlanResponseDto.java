package com.bharatshop.shared.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponseDto {

    private UUID id;
    private String name;
    private BigDecimal price;
    private JsonNode features;
    private Integer durationDays;
    private String description;
    private Integer displayOrder;
    private Boolean isPopular;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields for convenience
    private Integer maxProducts;
    private Long storageLimit;
    private Boolean hasAdvancedFeatures;
    private Boolean hasAnalytics;
    private Boolean hasCustomDomain;
    private String formattedPrice;
    private String durationText;
    
    // Helper method to format duration
    public String getDurationText() {
        if (durationDays == null) return null;
        
        if (durationDays == 30) {
            return "1 Month";
        } else if (durationDays == 90) {
            return "3 Months";
        } else if (durationDays == 180) {
            return "6 Months";
        } else if (durationDays == 365) {
            return "1 Year";
        } else {
            return durationDays + " Days";
        }
    }
    
    // Helper method to format price
    public String getFormattedPrice() {
        if (price == null) return null;
        return "₹" + price.toString();
    }
    
    // Manual builder method for compilation compatibility
    public static PlanResponseDtoBuilder builder() {
        return new PlanResponseDtoBuilder();
    }
    
    public static class PlanResponseDtoBuilder {
        private UUID id;
        private String name;
        private BigDecimal price;
        private JsonNode features;
        private Integer durationDays;
        private String description;
        private Integer displayOrder;
        private Boolean isPopular;
        private Boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public PlanResponseDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public PlanResponseDtoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public PlanResponseDtoBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }
        
        public PlanResponseDtoBuilder features(JsonNode features) {
            this.features = features;
            return this;
        }
        
        public PlanResponseDtoBuilder durationDays(Integer durationDays) {
            this.durationDays = durationDays;
            return this;
        }
        
        public PlanResponseDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public PlanResponseDtoBuilder displayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }
        
        public PlanResponseDtoBuilder isPopular(Boolean isPopular) {
            this.isPopular = isPopular;
            return this;
        }
        
        public PlanResponseDtoBuilder maxProducts(Integer maxProducts) {
            this.maxProducts = maxProducts;
            return this;
        }
        
        public PlanResponseDtoBuilder maxOrders(Integer maxOrders) {
            this.maxOrders = maxOrders;
            return this;
        }
        
        public PlanResponseDtoBuilder maxStorage(String maxStorage) {
            this.maxStorage = maxStorage;
            return this;
        }
        
        public PlanResponseDtoBuilder hasAnalytics(Boolean hasAnalytics) {
            this.hasAnalytics = hasAnalytics;
            return this;
        }
        
        public PlanResponseDtoBuilder hasCustomDomain(Boolean hasCustomDomain) {
            this.hasCustomDomain = hasCustomDomain;
            return this;
        }
        
        public PlanResponseDtoBuilder hasPrioritySupport(Boolean hasPrioritySupport) {
            this.hasPrioritySupport = hasPrioritySupport;
            return this;
        }
        
        public PlanResponseDtoBuilder storageLimit(Long storageLimit) {
            this.storageLimit = storageLimit;
            return this;
        }
        
        public PlanResponseDtoBuilder active(Boolean active) {
            this.active = active;
            return this;
        }
        
        public PlanResponseDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public PlanResponseDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public PlanResponseDto build() {
            PlanResponseDto dto = new PlanResponseDto();
            dto.id = this.id;
            dto.name = this.name;
            dto.price = this.price;
            dto.features = this.features;
            dto.durationDays = this.durationDays;
            dto.description = this.description;
            dto.displayOrder = this.displayOrder;
            dto.isPopular = this.isPopular;
            dto.active = this.active;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.maxProducts = this.maxProducts;
            dto.maxOrders = this.maxOrders;
            dto.maxStorage = this.maxStorage;
            dto.hasAnalytics = this.hasAnalytics;
            dto.hasCustomDomain = this.hasCustomDomain;
            dto.hasPrioritySupport = this.hasPrioritySupport;
            dto.storageLimit = this.storageLimit;
            return dto;
        }
    }
}
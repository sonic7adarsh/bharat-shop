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
}
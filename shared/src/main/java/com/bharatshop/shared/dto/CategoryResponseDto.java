package com.bharatshop.shared.dto;

import com.bharatshop.shared.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Category operations.
 * Used for returning category data in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentId;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // SEO fields
    private String seoTitle;
    private String seoDescription;
    private String canonicalUrl;
    private String structuredData;
    private Boolean featuredInSitemap;
    
    // For nested category structure
    private CategoryResponseDto parent;
    private List<CategoryResponseDto> children;
    
    // For product count in category
    private Long productCount;
}
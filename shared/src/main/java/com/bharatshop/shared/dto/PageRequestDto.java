package com.bharatshop.shared.dto;

import com.bharatshop.shared.enums.PageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for Page operations.
 * Used for creating and updating pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {
    
    @NotBlank(message = "Page title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @NotBlank(message = "Page slug is required")
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;
    
    private String content;
    
    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;
    
    @Size(max = 255, message = "Meta title must not exceed 255 characters")
    private String metaTitle;
    
    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    private String metaDescription;
    
    @Size(max = 1000, message = "Meta keywords must not exceed 1000 characters")
    private String metaKeywords;
    
    private String layout;
    
    private String seo;
    
    @NotNull(message = "Active status is required")
    private Boolean active;
    
    @NotNull(message = "Published status is required")
    private Boolean published;
    
    private Integer sortOrder;
    
    @NotNull(message = "Page type is required")
    private PageType pageType;
    
    private String template;
    
    private String templateId;
    
    private String customCss;
    
    private String customJs;
    
    private String featuredImage;
    
    private String author;
    
    private String status;
}
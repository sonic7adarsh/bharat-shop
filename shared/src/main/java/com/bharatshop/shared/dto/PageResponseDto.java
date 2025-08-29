package com.bharatshop.shared.dto;

import com.bharatshop.shared.enums.PageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Page operations.
 * Used for returning page data in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto {
    
    private UUID id;
    private UUID tenantId;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String layout;
    private String seo;
    private Boolean active;
    private Boolean published;
    private Integer sortOrder;
    private PageType pageType;
    private String templateId;
    private String template;
    private String customCss;
    private String customJs;
    private String featuredImage;
    private String author;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
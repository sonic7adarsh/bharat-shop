package com.bharatshop.storefront.dto;

import com.bharatshop.storefront.model.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for CMS Page data in storefront APIs.
 * Contains public-facing page information for customers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto {
    
    private UUID id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private Page.PageType pageType;
    private String template;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
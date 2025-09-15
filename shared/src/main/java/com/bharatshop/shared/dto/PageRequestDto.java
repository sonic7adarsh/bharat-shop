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
    
    // SEO fields
    @Size(max = 1000, message = "Canonical URL cannot exceed 1000 characters")
    private String canonicalUrl;
    
    private String structuredData;
    
    private Boolean featuredInSitemap = false;
    
    // Manual getters for fields that Lombok might not be generating
    public String getTitle() {
        return title;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getExcerpt() {
        return excerpt;
    }
    
    public String getMetaTitle() {
        return metaTitle;
    }
    
    public String getMetaDescription() {
        return metaDescription;
    }
    
    public String getMetaKeywords() {
        return metaKeywords;
    }
    
    public String getLayout() {
        return layout;
    }
    
    public String getSeo() {
        return seo;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public Boolean getPublished() {
        return published;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public PageType getPageType() {
        return pageType;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public String getCustomCss() {
        return customCss;
    }
    
    public String getCustomJs() {
        return customJs;
    }
    
    public String getFeaturedImage() {
        return featuredImage;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getCanonicalUrl() {
        return canonicalUrl;
    }
    
    public String getStructuredData() {
        return structuredData;
    }
    
    public Boolean getFeaturedInSitemap() {
        return featuredInSitemap;
    }
}
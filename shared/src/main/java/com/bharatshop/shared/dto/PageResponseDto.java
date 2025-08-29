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
    
    // Manual setters for fields that Lombok might not be generating
    public void setId(UUID id) {
        this.id = id;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
    
    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }
    
    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }
    
    public void setMetaKeywords(String metaKeywords) {
        this.metaKeywords = metaKeywords;
    }
    
    public void setLayout(String layout) {
        this.layout = layout;
    }
    
    public void setSeo(String seo) {
        this.seo = seo;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public void setPublished(Boolean published) {
        this.published = published;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public void setCustomCss(String customCss) {
        this.customCss = customCss;
    }
    
    public void setCustomJs(String customJs) {
        this.customJs = customJs;
    }
    
    public void setFeaturedImage(String featuredImage) {
        this.featuredImage = featuredImage;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
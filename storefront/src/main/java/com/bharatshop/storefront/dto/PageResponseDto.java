package com.bharatshop.storefront.dto;

import com.bharatshop.shared.enums.PageType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// import java.util.UUID; // Replaced with Long

/**
 * Response DTO for CMS Page data in storefront APIs.
 * Contains public-facing page information for customers.
 */
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto {
    
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private PageType pageType;
    private String template;
    private String layout;
    private String seo;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Manual getter methods
    public Long getId() {
        return id;
    }
    
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
    
    public PageType getPageType() {
        return pageType;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public String getLayout() {
        return layout;
    }
    
    public String getSeo() {
        return seo;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Manual builder method
    public static PageResponseDtoBuilder builder() {
        return new PageResponseDtoBuilder();
    }
    
    public static class PageResponseDtoBuilder {
        private Long id;
        private String title;
        private String slug;
        private String content;
        private String excerpt;
        private String metaTitle;
        private String metaDescription;
        private String metaKeywords;
        private PageType pageType;
        private String template;
        private String layout;
        private String seo;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public PageResponseDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public PageResponseDtoBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public PageResponseDtoBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }
        
        public PageResponseDtoBuilder content(String content) {
            this.content = content;
            return this;
        }
        
        public PageResponseDtoBuilder excerpt(String excerpt) {
            this.excerpt = excerpt;
            return this;
        }
        
        public PageResponseDtoBuilder metaTitle(String metaTitle) {
            this.metaTitle = metaTitle;
            return this;
        }
        
        public PageResponseDtoBuilder metaDescription(String metaDescription) {
            this.metaDescription = metaDescription;
            return this;
        }
        
        public PageResponseDtoBuilder metaKeywords(String metaKeywords) {
            this.metaKeywords = metaKeywords;
            return this;
        }
        
        public PageResponseDtoBuilder pageType(PageType pageType) {
            this.pageType = pageType;
            return this;
        }
        
        public PageResponseDtoBuilder template(String template) {
            this.template = template;
            return this;
        }
        
        public PageResponseDtoBuilder layout(String layout) {
            this.layout = layout;
            return this;
        }
        
        public PageResponseDtoBuilder seo(String seo) {
            this.seo = seo;
            return this;
        }
        
        public PageResponseDtoBuilder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }
        
        public PageResponseDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public PageResponseDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public PageResponseDto build() {
            PageResponseDto dto = new PageResponseDto();
            dto.id = this.id;
            dto.title = this.title;
            dto.slug = this.slug;
            dto.content = this.content;
            dto.excerpt = this.excerpt;
            dto.metaTitle = this.metaTitle;
            dto.metaDescription = this.metaDescription;
            dto.metaKeywords = this.metaKeywords;
            dto.pageType = this.pageType;
            dto.template = this.template;
            dto.layout = this.layout;
            dto.seo = this.seo;
            dto.sortOrder = this.sortOrder;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            return dto;
        }
    }
}
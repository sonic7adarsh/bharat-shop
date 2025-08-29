package com.bharatshop.shared.entity;

import com.bharatshop.shared.enums.PageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Page entity for content management system.
 * Represents pages that can be customized and rendered in the storefront.
 */
@Entity
@Table(name = "pages", indexes = {
    @Index(name = "idx_page_slug", columnList = "slug"),
    @Index(name = "idx_page_tenant_slug", columnList = "tenant_id, slug"),
    @Index(name = "idx_page_active", columnList = "active"),
    @Index(name = "idx_page_published", columnList = "published"),
    @Index(name = "idx_page_type", columnList = "page_type")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Page extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 100)
    private String slug;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 500)
    private String excerpt;
    
    @Column(length = 200)
    private String metaTitle;
    
    @Column(length = 500)
    private String metaDescription;
    
    @Column(length = 1000)
    private String metaKeywords;
    
    @Column(name = "layout", columnDefinition = "JSON")
    private String layout;
    
    @Column(name = "seo", columnDefinition = "JSON")
    private String seo;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false)
    private Boolean published = false;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false)
    private PageType pageType = PageType.STATIC;
    
    @Column(length = 100)
    private String template;
    
    @Column(name = "template_id")
    private String templateId;
    
    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;
    
    @Column(name = "custom_js", columnDefinition = "TEXT")
    private String customJs;
    
    @Column(name = "featured_image", length = 500)
    private String featuredImage;
    
    @Column(name = "author", length = 100)
    private String author;
    
    @Column(name = "status", length = 50)
    private String status = "draft";
    
    // Manual getters for BaseEntity fields that Lombok is not generating
    public java.time.LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }
    
    public java.util.UUID getId() {
        return this.id;
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    
    public java.time.LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    
    public java.util.UUID getTenantId() {
        return this.tenantId;
    }
    
    // Getters for all fields
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
    
    // Setters for fields that need to be updated
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
    
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
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
    
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setDeletedAt(java.time.LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public void setId(java.util.UUID id) {
        this.id = id;
    }
    
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setTenantId(java.util.UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    // Builder method for compatibility
    public static PageBuilder builder() {
        return new PageBuilder();
    }
    
    public static class PageBuilder {
        private Page page = new Page();
        
        public PageBuilder title(String title) {
            page.title = title;
            return this;
        }
        
        public PageBuilder slug(String slug) {
            page.slug = slug;
            return this;
        }
        
        public PageBuilder content(String content) {
            page.content = content;
            return this;
        }
        
        public PageBuilder excerpt(String excerpt) {
            page.excerpt = excerpt;
            return this;
        }
        
        public PageBuilder metaTitle(String metaTitle) {
            page.metaTitle = metaTitle;
            return this;
        }
        
        public PageBuilder metaDescription(String metaDescription) {
            page.metaDescription = metaDescription;
            return this;
        }
        
        public PageBuilder metaKeywords(String metaKeywords) {
            page.metaKeywords = metaKeywords;
            return this;
        }
        
        public PageBuilder layout(String layout) {
            page.layout = layout;
            return this;
        }
        
        public PageBuilder seo(String seo) {
            page.seo = seo;
            return this;
        }
        
        public PageBuilder active(Boolean active) {
            page.active = active;
            return this;
        }
        
        public PageBuilder published(Boolean published) {
            page.published = published;
            return this;
        }
        
        public PageBuilder sortOrder(Integer sortOrder) {
            page.sortOrder = sortOrder;
            return this;
        }
        
        public PageBuilder pageType(PageType pageType) {
            page.pageType = pageType;
            return this;
        }
        
        public PageBuilder template(String template) {
            page.template = template;
            return this;
        }
        
        public PageBuilder templateId(String templateId) {
            page.templateId = templateId;
            return this;
        }
        
        public PageBuilder customCss(String customCss) {
            page.customCss = customCss;
            return this;
        }
        
        public PageBuilder customJs(String customJs) {
            page.customJs = customJs;
            return this;
        }
        
        public PageBuilder featuredImage(String featuredImage) {
            page.featuredImage = featuredImage;
            return this;
        }
        
        public PageBuilder author(String author) {
            page.author = author;
            return this;
        }
        
        public PageBuilder status(String status) {
            page.status = status;
            return this;
        }
        
        public Page build() {
            return page;
        }
    }
}
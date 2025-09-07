package com.bharatshop.shared.entity;

import com.bharatshop.shared.enums.PageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Page entity for content management system.
 * Represents pages that can be customized and rendered in the storefront.
 */
@Entity(name = "SharedPage")
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
    
    public Long getId() {
        return this.id;
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    
    public java.time.LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    
    public Long getTenantId() {
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
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId != null ? tenantId : null;
    }
    

}
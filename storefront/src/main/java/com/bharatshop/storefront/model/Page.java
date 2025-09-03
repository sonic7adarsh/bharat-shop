package com.bharatshop.storefront.model;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * CMS Page entity for storefront content management.
 * Represents static pages like About Us, Privacy Policy, Terms of Service, etc.
 */
@Entity(name = "StorefrontPage")
@Table(name = "storefront_pages", indexes = {
    @Index(name = "idx_page_slug", columnList = "slug"),
    @Index(name = "idx_page_tenant_slug", columnList = "tenantId, slug"),
    @Index(name = "idx_page_active", columnList = "active")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Page extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, unique = true, length = 100)
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
    
    @Column(name = "storefront_sort_order")
    private Integer sortOrder = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "storefront_page_type", nullable = false)
    private PageType pageType = PageType.STATIC;
    
    @Column(length = 100)
    private String template;
    
    // Manual getter methods
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
    
    // Manual setter methods
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
    
    public enum PageType {
        STATIC,     // Static content pages
        DYNAMIC,    // Dynamic content pages
        LANDING,    // Landing pages
        LEGAL       // Legal pages (Privacy, Terms, etc.)
    }
}
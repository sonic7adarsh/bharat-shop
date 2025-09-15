package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_category_slug", columnList = "slug"),
        @Index(name = "idx_category_parent_id", columnList = "parent_id"),
        @Index(name = "idx_category_sort_order", columnList = "sort_order")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "previous_slug", length = 255)
    private String previousSlug; // For tracking slug changes

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription; // For SEO meta description

    @Column(name = "meta_title", length = 60)
    private String metaTitle;

    @Column(name = "meta_description", length = 160)
    private String metaDescription;

    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // Manual setter methods to fix Lombok issue
    public void setPreviousSlug(String previousSlug) {
        this.previousSlug = previousSlug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }

    // SEO metadata relationship
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "seo_metadata_id")
    private SeoMetadata seoMetadata;

    // Structured data for SEO
    @Column(name = "structured_data", columnDefinition = "TEXT")
    private String structuredData; // JSON-LD format for BreadcrumbList schema

    // SEO flags
    @Column(name = "featured_in_sitemap")
    private Boolean featuredInSitemap = true;

    @Column(name = "sitemap_priority", precision = 3, scale = 2)
    private Double sitemapPriority = 0.6;

    @Column(name = "sitemap_change_frequency", length = 20)
    private String sitemapChangeFrequency = "monthly";

    // Transient fields for hierarchical operations
    @Transient
    private List<Category> children;

    @Transient
    private Category parent;
    
    // Manual getters since Lombok is not working properly
    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getMetaTitle() { return metaTitle; }
    public String getName() { return name; }
    public String getMetaDescription() { return metaDescription; }
    public String getShortDescription() { return shortDescription; }
    public String getMetaKeywords() { return metaKeywords; }
    public String getSlug() { return slug; }
    public Long getParentId() { return parentId; }
    public Boolean getFeaturedInSitemap() { return featuredInSitemap; }
    public String getSitemapChangeFrequency() { return sitemapChangeFrequency; }
    public Double getSitemapPriority() { return sitemapPriority; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}
package com.bharatshop.shared.entity;

import com.bharatshop.shared.enums.PageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
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
}
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
@Entity
@Table(name = "pages", indexes = {
    @Index(name = "idx_page_slug", columnList = "slug"),
    @Index(name = "idx_page_tenant_slug", columnList = "tenantId, slug"),
    @Index(name = "idx_page_active", columnList = "active")
})
@Getter
@Setter
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
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false)
    private Boolean published = false;
    
    @Column
    private Integer sortOrder = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageType pageType = PageType.STATIC;
    
    @Column(length = 100)
    private String template;
    
    public enum PageType {
        STATIC,     // Static content pages
        DYNAMIC,    // Dynamic content pages
        LANDING,    // Landing pages
        LEGAL       // Legal pages (Privacy, Terms, etc.)
    }
}
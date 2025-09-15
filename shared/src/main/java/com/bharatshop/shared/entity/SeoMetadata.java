package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "seo_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SeoMetadata extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PRODUCT, CATEGORY, PAGE

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "title", length = 60)
    private String title;

    @Column(name = "description", length = 160)
    private String description;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;

    // OpenGraph fields
    @Column(name = "og_title", length = 60)
    private String ogTitle;

    @Column(name = "og_description", length = 160)
    private String ogDescription;

    @Column(name = "og_image", length = 500)
    private String ogImage;

    @Column(name = "og_type", length = 50)
    private String ogType;

    @Column(name = "og_url", length = 500)
    private String ogUrl;

    // Twitter Card fields
    @Column(name = "twitter_card", length = 50)
    private String twitterCard;

    @Column(name = "twitter_title", length = 60)
    private String twitterTitle;

    @Column(name = "twitter_description", length = 160)
    private String twitterDescription;

    @Column(name = "twitter_image", length = 500)
    private String twitterImage;

    @Column(name = "twitter_site", length = 50)
    private String twitterSite;

    // Structured data
    @Column(name = "structured_data", columnDefinition = "TEXT")
    private String structuredData; // JSON-LD format

    // SEO settings
    @Column(name = "no_index")
    private Boolean noIndex = false;

    @Column(name = "no_follow")
    private Boolean noFollow = false;

    @Column(name = "priority", precision = 3, scale = 2)
    private Double priority = 0.5; // For sitemap

    @Column(name = "change_frequency", length = 20)
    private String changeFrequency = "weekly"; // For sitemap

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    // Manual getters and setters since Lombok is not working properly
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public void setOgTitle(String ogTitle) { this.ogTitle = ogTitle; }
    public void setOgDescription(String ogDescription) { this.ogDescription = ogDescription; }
    public void setOgType(String ogType) { this.ogType = ogType; }
    public void setOgUrl(String ogUrl) { this.ogUrl = ogUrl; }
    public void setOgImage(String ogImage) { this.ogImage = ogImage; }
    public void setTwitterCard(String twitterCard) { this.twitterCard = twitterCard; }
    public void setTwitterTitle(String twitterTitle) { this.twitterTitle = twitterTitle; }
    public void setTwitterDescription(String twitterDescription) { this.twitterDescription = twitterDescription; }
    public void setTwitterImage(String twitterImage) { this.twitterImage = twitterImage; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public String getKeywords() { return keywords; }
    public String getOgTitle() { return ogTitle; }
    public String getOgDescription() { return ogDescription; }
    public String getOgImage() { return ogImage; }
    public String getOgType() { return ogType; }
    public String getOgUrl() { return ogUrl; }
    public String getTwitterCard() { return twitterCard; }
    public String getTwitterTitle() { return twitterTitle; }
    public String getTwitterDescription() { return twitterDescription; }
    public String getTwitterImage() { return twitterImage; }
    public String getTwitterSite() { return twitterSite; }
    

    
    @PrePersist
    @PreUpdate
    public void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }

    // Unique constraint on entity_type + entity_id + tenant_id
    @Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type", "entity_id", "tenant_id"})
    })
    public static class SeoMetadataConstraints {}
}
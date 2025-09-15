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
@Table(name = "slug_redirects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SlugRedirect extends BaseEntity {

    @Column(name = "old_slug", nullable = false, length = 255)
    private String oldSlug;

    @Column(name = "new_slug", nullable = false, length = 255)
    private String newSlug;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PRODUCT, CATEGORY, PAGE

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "redirect_type", nullable = false)
    private Integer redirectType = 301; // HTTP status code (301, 302)

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "redirect_count")
    private Long redirectCount = 0L;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "reason", length = 500)
    private String reason; // Reason for redirect (slug change, SEO optimization, etc.)

    public void incrementRedirectCount() {
        this.redirectCount = (this.redirectCount == null ? 0 : this.redirectCount) + 1;
        this.lastAccessed = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void setNewSlug(String newSlug) {
        this.newSlug = newSlug;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public String getNewSlug() {
        return newSlug;
    }
    
    public Long getRedirectCount() {
        return redirectCount;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    


    // Indexes for performance
    @Table(indexes = {
        @Index(name = "idx_slug_redirects_old_slug_tenant", columnList = "old_slug, tenant_id"),
        @Index(name = "idx_slug_redirects_entity", columnList = "entity_type, entity_id, tenant_id"),
        @Index(name = "idx_slug_redirects_active", columnList = "is_active, tenant_id")
    })
    public static class SlugRedirectIndexes {}
}
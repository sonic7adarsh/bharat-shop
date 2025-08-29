package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Template entity representing predefined page templates.
 * Contains template configuration and layout definitions.
 */
@Entity
@Table(name = "templates", indexes = {
    @Index(name = "idx_template_name", columnList = "name"),
    @Index(name = "idx_template_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Template extends BaseEntity {
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "config", columnDefinition = "JSON")
    private String config;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "preview_image", length = 500)
    private String previewImage;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    // Manual getter methods for compilation compatibility
    public String getName() {
        return name;
    }
    
    public String getConfig() {
        return config;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getPreviewImage() {
        return previewImage;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public String getCategory() {
        return category;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    // Manual setter methods for compilation compatibility
    public void setName(String name) {
        this.name = name;
    }
    
    public void setConfig(String config) {
        this.config = config;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setPreviewImage(String previewImage) {
        this.previewImage = previewImage;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
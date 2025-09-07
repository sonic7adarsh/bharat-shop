package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Option entity representing product options like Size, Color, etc.
 * Each option can have multiple values (e.g., Size: S, M, L, XL)
 */
@Entity
@Table(name = "options", indexes = {
        @Index(name = "idx_option_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_option_name", columnList = "name"),
        @Index(name = "idx_option_tenant_name", columnList = "tenant_id, name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_option_tenant_name", columnNames = {"tenant_id", "name"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Option extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OptionType type;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // One-to-many relationship with OptionValue
    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OptionValue> optionValues;

    public enum OptionType {
        TEXT,      // Simple text values
        COLOR,     // Color values with hex codes
        SIZE,      // Size values (S, M, L, etc.)
        MATERIAL,  // Material types
        CUSTOM     // Custom option type
    }
    
    // Manual getters and setters to fix Lombok issue
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public void setType(OptionType type) {
        this.type = type;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public OptionType getType() {
        return type;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public Long getId() {
        return id;
    }
}
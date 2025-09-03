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
    
    // Manual getter to fix Lombok issue
    public String getName() {
        return name;
    }
}
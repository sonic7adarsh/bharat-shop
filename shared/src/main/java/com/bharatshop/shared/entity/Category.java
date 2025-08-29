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

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Transient fields for hierarchical operations
    @Transient
    private List<Category> children;

    @Transient
    private Category parent;
}
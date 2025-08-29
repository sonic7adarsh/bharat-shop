package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_product_slug", columnList = "slug"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_name", columnList = "name")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images;

    @ElementCollection
    @CollectionTable(name = "product_categories", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "category_id")
    private List<UUID> categories;

    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        OUT_OF_STOCK,
        DISCONTINUED
    }
}
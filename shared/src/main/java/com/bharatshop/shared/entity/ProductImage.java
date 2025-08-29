package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "product_images")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductImage extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    // Relationship with Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
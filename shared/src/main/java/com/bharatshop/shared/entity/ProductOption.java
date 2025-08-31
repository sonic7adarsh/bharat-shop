package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * ProductOption entity representing the many-to-many relationship between products and options.
 * This defines which options are available for a specific product.
 */
@Entity
@Table(name = "product_options", indexes = {
        @Index(name = "idx_product_option_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_product_option_product_id", columnList = "product_id"),
        @Index(name = "idx_product_option_option_id", columnList = "option_id"),
        @Index(name = "idx_product_option_tenant_product", columnList = "tenant_id, product_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_option_tenant_product_option", 
                         columnNames = {"tenant_id", "product_id", "option_id"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductOption extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Many-to-one relationship with Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    // Many-to-one relationship with Option
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    private Option option;
}
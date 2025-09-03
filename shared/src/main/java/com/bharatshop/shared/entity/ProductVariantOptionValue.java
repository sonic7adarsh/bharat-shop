package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * ProductVariantOptionValue entity representing the relationship between product variants and option values.
 * This defines which specific option values (e.g., Size: Large, Color: Red) belong to each product variant.
 */
@Entity
@Table(name = "product_variant_option_values", indexes = {
        @Index(name = "idx_pvov_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_pvov_variant_id", columnList = "variant_id"),
        @Index(name = "idx_pvov_option_id", columnList = "option_id"),
        @Index(name = "idx_pvov_option_value_id", columnList = "option_value_id"),
        @Index(name = "idx_pvov_tenant_variant", columnList = "tenant_id, variant_id"),
        @Index(name = "idx_pvov_variant_option", columnList = "variant_id, option_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_pvov_tenant_variant_option", 
                         columnNames = {"tenant_id", "variant_id", "option_id"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductVariantOptionValue extends BaseEntity {

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "option_value_id", nullable = false)
    private UUID optionValueId;

    // Many-to-one relationship with ProductVariant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;

    // Many-to-one relationship with Option
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    private Option option;

    // Many-to-one relationship with OptionValue
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", insertable = false, updatable = false)
    private OptionValue optionValue;
    
    // Manual getters to fix Lombok issue
    public Option getOption() {
        return option;
    }
    
    public OptionValue getOptionValue() {
        return optionValue;
    }
}
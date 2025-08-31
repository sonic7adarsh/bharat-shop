package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * OptionValue entity representing individual values for product options
 * (e.g., "Large" for Size option, "Red" for Color option)
 */
@Entity
@Table(name = "option_values", indexes = {
        @Index(name = "idx_option_value_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_option_value_option_id", columnList = "option_id"),
        @Index(name = "idx_option_value_value", columnList = "value"),
        @Index(name = "idx_option_value_tenant_option", columnList = "tenant_id, option_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_option_value_tenant_option_value", 
                         columnNames = {"tenant_id", "option_id", "value"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OptionValue extends BaseEntity {

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "value", nullable = false, length = 100)
    private String value;

    @Column(name = "display_value", nullable = false, length = 100)
    private String displayValue;

    @Column(name = "color_code", length = 7) // For hex color codes like #FF0000
    private String colorCode;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Many-to-one relationship with Option
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    private Option option;
}
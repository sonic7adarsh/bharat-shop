package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Plan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "json")
    @JsonProperty("features")
    private JsonNode features;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_popular")
    private Boolean isPopular = false;

    // Helper methods for feature checking
    public Integer getMaxProducts() {
        if (features != null && features.has("maxProducts")) {
            return features.get("maxProducts").asInt();
        }
        return 0;
    }

    public Long getStorageLimit() {
        if (features != null && features.has("storageLimit")) {
            return features.get("storageLimit").asLong();
        }
        return 0L;
    }

    public Boolean hasFeature(String featureName) {
        if (features != null && features.has(featureName)) {
            return features.get(featureName).asBoolean();
        }
        return false;
    }
}
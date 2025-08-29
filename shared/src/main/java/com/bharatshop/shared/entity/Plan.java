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

    // Manual getters for fields that Lombok might not be generating
    public UUID getId() {
        return this.id;
    }
    
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    
    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }
    
    public UUID getTenantId() {
        return this.tenantId;
    }
    
    // Manual setter methods for compilation compatibility
    public void setId(UUID id) {
        this.id = id;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public void setIsPopular(Boolean isPopular) {
        this.isPopular = isPopular;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public JsonNode getFeatures() {
        return features;
    }

    public void setFeatures(JsonNode features) {
        this.features = features;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Boolean getActive() {
        return active;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public Boolean getIsPopular() {
        return isPopular;
    }
}
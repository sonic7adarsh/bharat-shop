package com.bharatshop.platform.model;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_code", columnList = "code", unique = true),
    @Index(name = "idx_tenant_name", columnList = "name", unique = true),
    @Index(name = "idx_tenant_active", columnList = "active")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
public class Tenant extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    
    // System tenant UUID for global tenant management
    private static final Long SYSTEM_TENANT_ID = 0L;
    
    /**
     * Override the tenant filter for Tenant entity since it's the master tenant data
     * Tenants are managed globally and not filtered by tenant_id
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        // For tenant entity, set tenantId to system UUID
        if (this.getTenantId() == null) {
            this.setTenantId(SYSTEM_TENANT_ID);
        }
    }
    
    // Manual getters for compilation compatibility
    public String getName() {
        return name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    // Manual setters for compilation compatibility
    public void setName(String name) {
        this.name = name;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    // Manual builder method for compilation compatibility
    public static TenantBuilder builder() {
        return new TenantBuilder();
    }
    
    public static class TenantBuilder {
        private String name;
        private String code;
        private String description;
        private boolean active = true;
        
        public TenantBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public TenantBuilder code(String code) {
            this.code = code;
            return this;
        }
        
        public TenantBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public TenantBuilder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Tenant build() {
            Tenant tenant = new Tenant();
            tenant.setName(this.name);
            tenant.setCode(this.code);
            tenant.setDescription(this.description);
            tenant.setActive(this.active);
            return tenant;
        }
    }
}
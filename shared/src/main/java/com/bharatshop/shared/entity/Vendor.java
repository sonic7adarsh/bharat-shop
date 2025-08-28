package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Vendor entity representing a store/vendor in the platform.
 * Contains store configuration, branding, and business information.
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_domain", columnList = "domain", unique = true),
    @Index(name = "idx_vendor_name", columnList = "name"),
    @Index(name = "idx_vendor_store_name", columnList = "store_name")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor extends BaseEntity {
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 500)
    private String logo;
    
    @Column(name = "theme_config", columnDefinition = "JSON")
    private String themeConfig;
    
    @Column(unique = true, length = 255)
    private String domain;
    
    @Column(length = 10)
    private String currency = "USD";
    
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";
    
    @Column(name = "subscription_id", length = 255)
    private String subscriptionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorStatus status = VendorStatus.PENDING;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    public enum VendorStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        INACTIVE
    }
}
package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_addresses", indexes = {
    @Index(name = "idx_address_customer_tenant", columnList = "customerId, tenantId"),
    @Index(name = "idx_address_tenant", columnList = "tenantId"),
    @Index(name = "idx_address_customer", columnList = "customerId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;
    
    @Column(name = "line1", nullable = false, length = 255)
    private String line1;
    
    @Column(name = "line2", length = 255)
    private String line2;
    
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    
    @Column(name = "state", nullable = false, length = 100)
    private String state;
    
    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;
    
    @Column(name = "country", nullable = false, length = 100)
    private String country;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        address.append(line1);
        if (line2 != null && !line2.trim().isEmpty()) {
            address.append(", ").append(line2);
        }
        address.append(", ").append(city);
        address.append(", ").append(state);
        address.append(" - ").append(pincode);
        address.append(", ").append(country);
        return address.toString();
    }
    
    public String getShortAddress() {
        return city + ", " + state + " - " + pincode;
    }
}
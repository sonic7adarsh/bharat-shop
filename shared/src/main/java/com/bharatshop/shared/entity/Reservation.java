package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_variant", columnList = "productVariantId"),
    @Index(name = "idx_reservation_order", columnList = "orderId"),
    @Index(name = "idx_reservation_expires", columnList = "expiresAt"),
    @Index(name = "idx_reservation_tenant_variant", columnList = "tenantId, productVariantId"),
    @Index(name = "idx_reservation_status_expires", columnList = "status, expiresAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false)
    private UUID productVariantId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "orderId")
    private Long orderId;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }
    
    public void markAsCommitted() {
        this.status = ReservationStatus.COMMITTED;
    }
    
    public void markAsReleased() {
        this.status = ReservationStatus.RELEASED;
    }
    
    public enum ReservationStatus {
        ACTIVE,     // Reservation is active and holding stock
        COMMITTED,  // Reservation has been converted to order item
        RELEASED    // Reservation has been released (expired or cancelled)
    }
}
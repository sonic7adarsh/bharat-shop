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
    
    // Manual getter and setter methods to bypass Lombok issues
    public ReservationStatus getStatus() {
        return status;
    }
    
    public Long getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public UUID getProductVariantId() {
        return productVariantId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    // Manual builder method to bypass Lombok issues
    public static ReservationBuilder builder() {
        return new ReservationBuilder();
    }
    
    // Manual ReservationBuilder class
    public static class ReservationBuilder {
        private UUID tenantId;
        private UUID productVariantId;
        private Integer quantity;
        private Long orderId;
        private LocalDateTime expiresAt;
        private ReservationStatus status = ReservationStatus.ACTIVE;
        
        public ReservationBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public ReservationBuilder productVariantId(UUID productVariantId) {
            this.productVariantId = productVariantId;
            return this;
        }
        
        public ReservationBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public ReservationBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public ReservationBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public ReservationBuilder status(ReservationStatus status) {
            this.status = status;
            return this;
        }
        
        public Reservation build() {
            Reservation reservation = new Reservation();
            reservation.tenantId = this.tenantId;
            reservation.productVariantId = this.productVariantId;
            reservation.quantity = this.quantity;
            reservation.orderId = this.orderId;
            reservation.expiresAt = this.expiresAt;
            reservation.status = this.status;
            return reservation;
        }
    }
}
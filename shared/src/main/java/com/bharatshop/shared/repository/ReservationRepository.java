package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    /**
     * Find all active reservations for a specific product variant with pessimistic lock
     * This ensures atomic operations when checking/updating stock
     */

    
    /**
     * Calculate total reserved quantity for a product variant
     */
    @Query(value = "SELECT COALESCE(SUM(quantity), 0) FROM reservation " +
           "WHERE tenant_id = ?1 " +
           "AND product_variant_id = ?2 " +
           "AND status = 'ACTIVE' " +
           "AND expires_at > ?3", nativeQuery = true)
    Integer getTotalReservedQuantity(
        Long tenantId,
        Long productVariantId,
        LocalDateTime now
    );
    
    /**
     * Find reservations by order ID
     */
    List<Reservation> findByOrderIdAndTenantId(Long orderId, Long tenantId);
    
    /**
     * Find expired reservations for cleanup
     */
    @Query(value = "SELECT * FROM reservation WHERE status = 'ACTIVE' " +
           "AND expires_at <= ?1", nativeQuery = true)
    List<Reservation> findExpiredReservations(LocalDateTime now);
    
    /**
     * Find stale reservations (active but older than threshold)
     */
    @Query(value = "SELECT * FROM reservation WHERE tenant_id = ?1 " +
           "AND status = 'ACTIVE' " +
           "AND created_at <= ?2", nativeQuery = true)
    List<Reservation> findStaleReservations(
        Long tenantId,
        LocalDateTime threshold
    );
    
    /**
     * Bulk update expired reservations to RELEASED status
     */
    @Modifying
    @Query(value = "UPDATE reservation SET status = 'RELEASED', updated_at = ?1 " +
           "WHERE status = 'ACTIVE' AND expires_at <= ?1", nativeQuery = true)
    int releaseExpiredReservations(LocalDateTime now);
    
    /**
     * Find reservations by tenant and status
     */
    List<Reservation> findByTenantIdAndStatusOrderByCreatedAtDesc(
        Long tenantId, 
        Reservation.ReservationStatus status
    );
    
    /**
     * Find reservations by tenant and status with pagination
     */
    Page<Reservation> findByTenantIdAndStatus(
        Long tenantId, 
        Reservation.ReservationStatus status,
        Pageable pageable
    );
    
    /**
     * Count reservations by tenant and status
     */
    long countByTenantIdAndStatus(
        Long tenantId, 
        Reservation.ReservationStatus status
    );
    
    /**
     * Find reservation by ID and tenant (without lock)
     */
    Optional<Reservation> findByIdAndTenantId(
        Long id,
        Long tenantId
    );
    

    
    /**
     * Find stale reservations across all tenants
     */
    @Query(value = "SELECT * FROM reservation WHERE status = 'ACTIVE' " +
           "AND created_at <= ?1", nativeQuery = true)
    List<Reservation> findStaleReservationsAllTenants(
        LocalDateTime threshold
    );
    
    /**
     * Count active reservations for a tenant
     */
    @Query(value = "SELECT COUNT(*) FROM reservation WHERE tenant_id = ?1 " +
           "AND status = 'ACTIVE' AND expires_at > ?2", nativeQuery = true)
    long countActiveReservations(
        Long tenantId,
        LocalDateTime now
    );
}
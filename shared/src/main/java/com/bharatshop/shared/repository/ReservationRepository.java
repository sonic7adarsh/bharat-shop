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
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    /**
     * Find all active reservations for a specific product variant with pessimistic lock
     * This ensures atomic operations when checking/updating stock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.tenantId = :tenantId " +
           "AND r.productVariantId = :productVariantId " +
           "AND r.status = 'ACTIVE' " +
           "AND r.expiresAt > :now")
    List<Reservation> findActiveReservationsForVariantWithLock(
        @Param("tenantId") UUID tenantId,
        @Param("productVariantId") UUID productVariantId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Calculate total reserved quantity for a product variant
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Reservation r " +
           "WHERE r.tenantId = :tenantId " +
           "AND r.productVariantId = :productVariantId " +
           "AND r.status = 'ACTIVE' " +
           "AND r.expiresAt > :now")
    Integer getTotalReservedQuantity(
        @Param("tenantId") UUID tenantId,
        @Param("productVariantId") UUID productVariantId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Find reservations by order ID
     */
    List<Reservation> findByOrderIdAndTenantId(Long orderId, UUID tenantId);
    
    /**
     * Find expired reservations for cleanup
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' " +
           "AND r.expiresAt <= :now")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);
    
    /**
     * Find stale reservations (active but older than threshold)
     */
    @Query("SELECT r FROM Reservation r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'ACTIVE' " +
           "AND r.createdAt <= :threshold")
    List<Reservation> findStaleReservations(
        @Param("tenantId") UUID tenantId,
        @Param("threshold") LocalDateTime threshold
    );
    
    /**
     * Bulk update expired reservations to RELEASED status
     */
    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'RELEASED', r.updatedAt = :now " +
           "WHERE r.status = 'ACTIVE' AND r.expiresAt <= :now")
    int releaseExpiredReservations(@Param("now") LocalDateTime now);
    
    /**
     * Find reservations by tenant and status
     */
    List<Reservation> findByTenantIdAndStatusOrderByCreatedAtDesc(
        UUID tenantId, 
        Reservation.ReservationStatus status
    );
    
    /**
     * Find reservations by tenant and status with pagination
     */
    Page<Reservation> findByTenantIdAndStatus(
        UUID tenantId, 
        Reservation.ReservationStatus status,
        Pageable pageable
    );
    
    /**
     * Count reservations by tenant and status
     */
    long countByTenantIdAndStatus(
        UUID tenantId, 
        Reservation.ReservationStatus status
    );
    
    /**
     * Find reservation by ID and tenant (without lock)
     */
    Optional<Reservation> findByIdAndTenantId(
        Long id,
        UUID tenantId
    );
    
    /**
     * Find reservation by ID with lock for atomic updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<Reservation> findByIdAndTenantIdWithLock(
        @Param("id") Long id,
        @Param("tenantId") UUID tenantId
    );
    
    /**
     * Find stale reservations across all tenants
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' " +
           "AND r.createdAt <= :threshold")
    List<Reservation> findStaleReservationsAllTenants(
        @Param("threshold") LocalDateTime threshold
    );
    
    /**
     * Count active reservations for a tenant
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.tenantId = :tenantId " +
           "AND r.status = 'ACTIVE' AND r.expiresAt > :now")
    long countActiveReservations(
        @Param("tenantId") UUID tenantId,
        @Param("now") LocalDateTime now
    );
}
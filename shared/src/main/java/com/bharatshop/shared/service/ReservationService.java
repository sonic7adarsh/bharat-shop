package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.entity.Reservation;
import com.bharatshop.shared.repository.ProductVariantRepository;
import com.bharatshop.shared.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    
    // Manual log field to bypass Lombok issues
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ProductVariantRepository productVariantRepository;
    
    // Default reservation timeout in minutes
    private static final int DEFAULT_RESERVATION_TIMEOUT_MINUTES = 15;
    
    /**
     * Reserve stock for a product variant atomically
     * 
     * @param tenantId The tenant ID
     * @param productVariantId The product variant ID
     * @param quantity The quantity to reserve
     * @param timeoutMinutes Reservation timeout in minutes (optional)
     * @return The created reservation
     * @throws IllegalArgumentException if insufficient stock available
     */
    @Transactional
    public Reservation reserveStock(Long tenantId, Long productVariantId, Integer quantity, Integer timeoutMinutes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        int timeout = timeoutMinutes != null ? timeoutMinutes : DEFAULT_RESERVATION_TIMEOUT_MINUTES;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeout);
        LocalDateTime now = LocalDateTime.now();
        
        // Get product variant
        Optional<ProductVariant> variantOpt = productVariantRepository.findByIdAndDeletedAtIsNull(productVariantId);
        if (variantOpt.isEmpty()) {
            throw new IllegalArgumentException("Product variant not found: " + productVariantId);
        }
        
        ProductVariant variant = variantOpt.get();
        if (!variant.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Product variant does not belong to tenant");
        }
        
        // Check if variant is available for reservation
        if (variant.getStatus() != ProductVariant.VariantStatus.ACTIVE) {
            throw new IllegalArgumentException("Product variant is not available for reservation");
        }
        
        // Get current reserved quantity with lock
        Integer currentReserved = reservationRepository.getTotalReservedQuantity(tenantId, productVariantId, now);
        if (currentReserved == null) {
            currentReserved = 0;
        }
        
        // Check if sufficient stock is available (stock - reserved >= requested quantity)
        int availableStock = variant.getStock() - currentReserved;
        if (availableStock < quantity) {
            throw new IllegalArgumentException(
                String.format("Insufficient stock. Available: %d, Requested: %d", availableStock, quantity)
            );
        }
        
        // Create and save reservation
        Reservation reservation = Reservation.builder()
                .tenantId(tenantId)
                .productVariantId(productVariantId)
                .quantity(quantity)
                .expiresAt(expiresAt)
                .status(Reservation.ReservationStatus.ACTIVE)
                .build();
        
        reservation = reservationRepository.save(reservation);
        
        log.info("Reserved {} units of variant {} for tenant {} (expires at {})", 
                quantity, productVariantId, tenantId, expiresAt);
        
        return reservation;
    }
    
    /**
     * Reserve stock with default timeout
     */
    @Transactional
    public Reservation reserveStock(Long tenantId, Long productVariantId, Integer quantity) {
        return reserveStock(tenantId, productVariantId, quantity, null);
    }
    
    /**
     * Commit all reservations for an order
     */
    @Transactional
    public void commitReservations(Long tenantId, Long orderId) {
        List<Reservation> reservations = reservationRepository.findByOrderIdAndTenantId(orderId, tenantId);
        List<Long> reservationIds = reservations.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.ACTIVE)
                .map(Reservation::getId)
                .collect(java.util.stream.Collectors.toList());
        
        if (!reservationIds.isEmpty()) {
            commitReservations(tenantId, reservationIds, orderId);
        }
    }
    
    /**
     * Commit reservations to order and decrement actual stock
     * 
     * @param tenantId The tenant ID
     * @param reservationIds List of reservation IDs to commit
     * @param orderId The order ID to associate with reservations
     */
    @Transactional
    public void commitReservations(Long tenantId, List<Long> reservationIds, Long orderId) {
        for (Long reservationId : reservationIds) {
            Optional<Reservation> reservationOpt = reservationRepository.findByIdAndTenantId(reservationId, tenantId);
            
            if (reservationOpt.isEmpty()) {
                log.warn("Reservation not found: {} for tenant {}", reservationId, tenantId);
                continue;
            }
            
            Reservation reservation = reservationOpt.get();
            
            if (!reservation.isActive()) {
                log.warn("Attempting to commit inactive reservation: {}", reservationId);
                continue;
            }
            
            // Get product variant
            Optional<ProductVariant> variantOpt = productVariantRepository.findByIdAndDeletedAtIsNull(reservation.getProductVariantId());
            if (variantOpt.isEmpty()) {
                log.error("Product variant not found for reservation: {}", reservationId);
                continue;
            }
            
            ProductVariant variant = variantOpt.get();
            
            // Decrement actual stock
            int newStock = variant.getStock() - reservation.getQuantity();
            if (newStock < 0) {
                log.error("Stock would go negative for variant {}: current={}, reserved={}", 
                         variant.getId(), variant.getStock(), reservation.getQuantity());
                throw new IllegalStateException("Insufficient stock to commit reservation");
            }
            
            variant.setStock(newStock);
            productVariantRepository.save(variant);
            
            // Mark reservation as committed
            reservation.setOrderId(orderId);
            reservation.markAsCommitted();
            reservationRepository.save(reservation);
            
            log.info("Committed reservation {} for variant {} (new stock: {})", 
                    reservationId, variant.getId(), newStock);
        }
    }
    
    /**
     * Release a specific reservation
     */
    @Transactional
    public void releaseReservation(Long tenantId, Long reservationId) {
        Optional<Reservation> reservationOpt = reservationRepository.findByIdAndTenantId(reservationId, tenantId);
        
        if (reservationOpt.isEmpty()) {
            log.warn("Reservation not found: {} for tenant {}", reservationId, tenantId);
            return;
        }
        
        Reservation reservation = reservationOpt.get();
        
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            log.warn("Attempting to release non-active reservation: {}", reservationId);
            return;
        }
        
        reservation.markAsReleased();
        reservationRepository.save(reservation);
        
        log.info("Released reservation {} for variant {}", reservationId, reservation.getProductVariantId());
    }
    
    /**
     * Release all reservations for an order (e.g., when order is cancelled)
     */
    @Transactional
    public void releaseOrderReservations(Long tenantId, Long orderId) {
        List<Reservation> reservations = reservationRepository.findByOrderIdAndTenantId(orderId, tenantId);
        
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == Reservation.ReservationStatus.ACTIVE) {
                reservation.markAsReleased();
                reservationRepository.save(reservation);
                
                log.info("Released reservation {} for cancelled order {}", reservation.getId(), orderId);
            }
        }
    }
    
    /**
     * Clean up expired reservations (called by scheduled job)
     */
    @Transactional
    public int cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        int releasedCount = reservationRepository.releaseExpiredReservations(now);
        
        if (releasedCount > 0) {
            log.info("Released {} expired reservations", releasedCount);
        }
        
        return releasedCount;
    }
    
    /**
     * Get available stock for a product variant (total stock - active reservations)
     */
    @Transactional(readOnly = true)
    public int getAvailableStock(Long tenantId, Long productVariantId) {
        Optional<ProductVariant> variantOpt = productVariantRepository.findActiveByIdAndTenantId(productVariantId, tenantId);
        if (variantOpt.isEmpty()) {
            return 0;
        }
        
        ProductVariant variant = variantOpt.get();
        if (!variant.getTenantId().equals(tenantId)) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Integer reserved = reservationRepository.getTotalReservedQuantity(tenantId, productVariantId, now);
        if (reserved == null) {
            reserved = 0;
        }
        
        return Math.max(0, variant.getStock() - reserved);
    }
    
    /**
     * Get all active reservations for a tenant
     */
    @Transactional(readOnly = true)
    public List<Reservation> getActiveReservations(Long tenantId) {
        return reservationRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
            tenantId, Reservation.ReservationStatus.ACTIVE
        );
    }
    
    /**
     * Get active reservations for a tenant with pagination
     */
    @Transactional(readOnly = true)
    public Page<Reservation> getActiveReservations(Long tenantId, Pageable pageable) {
        return reservationRepository.findByTenantIdAndStatus(tenantId, Reservation.ReservationStatus.ACTIVE, pageable);
    }
    
    /**
     * Count active reservations for a tenant
     */
    @Transactional(readOnly = true)
    public long countActiveReservations(Long tenantId) {
        return reservationRepository.countByTenantIdAndStatus(tenantId, Reservation.ReservationStatus.ACTIVE);
    }
    
    /**
     * Get stale reservations (older than threshold) for a specific tenant
     */
    @Transactional(readOnly = true)
    public List<Reservation> getStaleReservations(Long tenantId, int thresholdMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);
        return reservationRepository.findStaleReservations(tenantId, threshold);
    }
    
    /**
     * Get stale reservations (older than threshold) across all tenants
     */
    @Transactional(readOnly = true)
    public List<Reservation> getStaleReservations(int thresholdMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);
        return reservationRepository.findStaleReservationsAllTenants(threshold);
    }
}
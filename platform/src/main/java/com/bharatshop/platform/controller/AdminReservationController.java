package com.bharatshop.platform.controller;

import com.bharatshop.shared.entity.Reservation;
import com.bharatshop.shared.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// import java.util.UUID; // Replaced with Long

/**
 * Admin controller for managing reservations
 * Provides endpoints for inspecting and manually releasing stale reservations
 */
@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@Tag(name = "Admin Reservations", description = "Admin endpoints for managing product reservations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

    private static final Logger log = LoggerFactory.getLogger(AdminReservationController.class);
    private final ReservationService reservationService;

    /**
     * Get active reservations for a tenant with pagination
     */
    @GetMapping
    @Operation(summary = "Get active reservations", description = "Retrieve active reservations for a tenant with pagination")
    public ResponseEntity<Page<Reservation>> getActiveReservations(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Reservation> reservations = reservationService.getActiveReservations(tenantId, pageable);
            
            log.info("Retrieved {} active reservations for tenant {}", reservations.getTotalElements(), tenantId);
            return ResponseEntity.ok(reservations);
            
        } catch (Exception e) {
            log.error("Failed to retrieve active reservations for tenant {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get stale reservations (older than specified minutes)
     */
    @GetMapping("/stale")
    @Operation(summary = "Get stale reservations", description = "Retrieve reservations older than specified minutes")
    public ResponseEntity<List<Reservation>> getStaleReservations(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId,
            @Parameter(description = "Age threshold in minutes") @RequestParam(defaultValue = "60") int olderThanMinutes) {
        
        try {
            List<Reservation> staleReservations = reservationService.getStaleReservations(olderThanMinutes);
            
            // Filter by tenant if needed (depending on your multi-tenancy requirements)
            List<Reservation> tenantStaleReservations = staleReservations.stream()
                    .filter(r -> r.getTenantId().equals(tenantId))
                    .toList();
            
            log.info("Found {} stale reservations for tenant {} (older than {} minutes)", 
                    tenantStaleReservations.size(), tenantId, olderThanMinutes);
            
            return ResponseEntity.ok(tenantStaleReservations);
            
        } catch (Exception e) {
            log.error("Failed to retrieve stale reservations for tenant {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get reservation statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get reservation statistics", description = "Get statistics about reservations for a tenant")
    public ResponseEntity<ReservationStats> getReservationStats(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId) {
        
        try {
            // Get active reservations count
            long activeCount = reservationService.countActiveReservations(tenantId);
            
            // Get stale reservations (older than 1 hour)
            List<Reservation> staleReservations = reservationService.getStaleReservations(60)
                    .stream()
                    .filter(r -> r.getTenantId().equals(tenantId))
                    .toList();
            
            // Get very stale reservations (older than 24 hours)
            List<Reservation> veryStaleReservations = reservationService.getStaleReservations(1440)
                    .stream()
                    .filter(r -> r.getTenantId().equals(tenantId))
                    .toList();
            
            ReservationStats stats = ReservationStats.builder()
                    .activeReservations(activeCount)
                    .staleReservations((long) staleReservations.size())
                    .veryStaleReservations((long) veryStaleReservations.size())
                    .tenantId(tenantId)
                    .build();
            
            log.info("Retrieved reservation stats for tenant {}: {} active, {} stale, {} very stale", 
                    tenantId, activeCount, staleReservations.size(), veryStaleReservations.size());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to retrieve reservation stats for tenant {}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Manually release a specific reservation
     */
    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Release reservation", description = "Manually release a specific reservation")
    public ResponseEntity<String> releaseReservation(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId,
            @Parameter(description = "Reservation ID") @PathVariable Long reservationId) {
        
        try {
            reservationService.releaseReservation(reservationId, tenantId);
            
            log.info("Manually released reservation {} for tenant {}", reservationId, tenantId);
            return ResponseEntity.ok("Reservation released successfully");
            
        } catch (Exception e) {
            log.error("Failed to release reservation {} for tenant {}", reservationId, tenantId, e);
            return ResponseEntity.badRequest().body("Failed to release reservation: " + e.getMessage());
        }
    }

    /**
     * Manually release all stale reservations
     */
    @DeleteMapping("/stale")
    @Operation(summary = "Release stale reservations", description = "Manually release all stale reservations older than specified minutes")
    public ResponseEntity<String> releaseStaleReservations(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId,
            @Parameter(description = "Age threshold in minutes") @RequestParam(defaultValue = "60") int olderThanMinutes) {
        
        try {
            List<Reservation> staleReservations = reservationService.getStaleReservations(olderThanMinutes)
                    .stream()
                    .filter(r -> r.getTenantId().equals(tenantId))
                    .toList();
            
            int releasedCount = 0;
            for (Reservation reservation : staleReservations) {
                try {
                    reservationService.releaseReservation(reservation.getId(), tenantId);
                    releasedCount++;
                } catch (Exception e) {
                    log.error("Failed to release stale reservation {}", reservation.getId(), e);
                }
            }
            
            log.info("Manually released {} stale reservations for tenant {}", releasedCount, tenantId);
            return ResponseEntity.ok(String.format("Released %d stale reservations", releasedCount));
            
        } catch (Exception e) {
            log.error("Failed to release stale reservations for tenant {}", tenantId, e);
            return ResponseEntity.internalServerError().body("Failed to release stale reservations: " + e.getMessage());
        }
    }

    /**
     * Trigger manual cleanup of expired reservations
     */
    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup expired reservations", description = "Manually trigger cleanup of expired reservations")
    public ResponseEntity<String> cleanupExpiredReservations(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId) {
        
        try {
            int releasedCount = reservationService.cleanupExpiredReservations();
            
            log.info("Manual cleanup released {} expired reservations (all tenants)", releasedCount);
            return ResponseEntity.ok(String.format("Cleanup completed. Released %d expired reservations", releasedCount));
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired reservations", e);
            return ResponseEntity.internalServerError().body("Failed to cleanup expired reservations: " + e.getMessage());
        }
    }

    /**
     * Get available stock for a product variant
     */
    @GetMapping("/stock/{variantId}")
    @Operation(summary = "Get available stock", description = "Get available stock for a product variant considering active reservations")
    public ResponseEntity<StockInfo> getAvailableStock(
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") Long tenantId,
            @Parameter(description = "Product Variant ID") @PathVariable Long variantId) {
        
        try {
            int availableStock = reservationService.getAvailableStock(tenantId, variantId);
            
            StockInfo stockInfo = StockInfo.builder()
                    .variantId(variantId)
                    .availableStock(availableStock)
                    .tenantId(tenantId)
                    .build();
            
            return ResponseEntity.ok(stockInfo);
            
        } catch (Exception e) {
            log.error("Failed to get available stock for variant {} and tenant {}", variantId, tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DTO for reservation statistics
     */
    public static class ReservationStats {
        private Long activeReservations;
        private Long staleReservations;
        private Long veryStaleReservations;
        private Long tenantId;

        public static ReservationStatsBuilder builder() {
            return new ReservationStatsBuilder();
        }

        public static class ReservationStatsBuilder {
            private Long activeReservations;
            private Long staleReservations;
            private Long veryStaleReservations;
            private Long tenantId;

            public ReservationStatsBuilder activeReservations(Long activeReservations) {
                this.activeReservations = activeReservations;
                return this;
            }

            public ReservationStatsBuilder staleReservations(Long staleReservations) {
                this.staleReservations = staleReservations;
                return this;
            }

            public ReservationStatsBuilder veryStaleReservations(Long veryStaleReservations) {
                this.veryStaleReservations = veryStaleReservations;
                return this;
            }

            public ReservationStatsBuilder tenantId(Long tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public ReservationStats build() {
                ReservationStats stats = new ReservationStats();
                stats.activeReservations = this.activeReservations;
                stats.staleReservations = this.staleReservations;
                stats.veryStaleReservations = this.veryStaleReservations;
                stats.tenantId = this.tenantId;
                return stats;
            }
        }

        // Getters
        public Long getActiveReservations() { return activeReservations; }
        public Long getStaleReservations() { return staleReservations; }
        public Long getVeryStaleReservations() { return veryStaleReservations; }
        public Long getTenantId() { return tenantId; }
    }

    /**
     * DTO for stock information
     */
    public static class StockInfo {
        private Long variantId;
        private Integer availableStock;
        private Long tenantId;

        public static StockInfoBuilder builder() {
            return new StockInfoBuilder();
        }

        public static class StockInfoBuilder {
            private Long variantId;
            private Integer availableStock;
            private Long tenantId;

            public StockInfoBuilder variantId(Long variantId) {
                this.variantId = variantId;
                return this;
            }

            public StockInfoBuilder availableStock(Integer availableStock) {
                this.availableStock = availableStock;
                return this;
            }

            public StockInfoBuilder tenantId(Long tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public StockInfo build() {
                StockInfo info = new StockInfo();
                info.variantId = this.variantId;
                info.availableStock = this.availableStock;
                info.tenantId = this.tenantId;
                return info;
            }
        }

        // Getters
        public Long getVariantId() { return variantId; }
        public Integer getAvailableStock() { return availableStock; }
        public Long getTenantId() { return tenantId; }
    }
}
package com.bharatshop.shared.scheduler;

import com.bharatshop.shared.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



/**
 * Scheduled job to automatically clean up expired reservations
 * Runs every 5 minutes to release expired reservations and prevent stock leakage
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupScheduler {
    
    // Manual log field to bypass Lombok issues
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReservationCleanupScheduler.class);

    private final ReservationService reservationService;

    /**
     * Clean up expired reservations every 5 minutes
     * This prevents stock from being held indefinitely by abandoned carts
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupExpiredReservations() {
        try {
            System.out.println("Starting cleanup of expired reservations");
            
            int releasedCount = reservationService.cleanupExpiredReservations();
            
            if (releasedCount > 0) {
                System.out.println("Released " + releasedCount + " expired reservations");
            } else {
                System.out.println("No expired reservations found to release");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to cleanup expired reservations: " + e.getMessage());
        }
    }

    /**
     * Clean up stale reservations every hour
     * This handles reservations that may have been missed by the regular cleanup
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupStaleReservations() {
        try {
            System.out.println("Starting cleanup of stale reservations");
            
            // Get stale reservations (older than 2 hours)
            var staleReservations = reservationService.getStaleReservations(120); // 2 hours
            
            if (!staleReservations.isEmpty()) {
                System.out.println("Found " + staleReservations.size() + " stale reservations older than 2 hours");
                
                // Release each stale reservation
                int releasedCount = 0;
                for (var reservation : staleReservations) {
                    try {
                        reservationService.releaseReservation(reservation.getTenantId(), reservation.getId());
                        releasedCount++;
                    } catch (Exception e) {
                        System.out.println("Failed to release stale reservation " + reservation.getId() + ": " + e.getMessage());
                    }
                }
                
                System.out.println("Released " + releasedCount + " stale reservations");
            } else {
                System.out.println("No stale reservations found");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to cleanup stale reservations: " + e.getMessage());
        }
    }

    /**
     * Log reservation statistics every 30 minutes for monitoring
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void logReservationStatistics() {
        try {
            // This is a simple monitoring task - in production you might want to
            // send these metrics to a monitoring system like Prometheus
            System.out.println("Reservation cleanup scheduler is running normally");
        } catch (Exception e) {
            System.out.println("Failed to log reservation statistics: " + e.getMessage());
        }
    }
}
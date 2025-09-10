package com.bharatshop.shared.scheduler;

import com.bharatshop.shared.entity.Shipment;
import com.bharatshop.shared.entity.ShipmentTracking;
import com.bharatshop.shared.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to automatically update shipment tracking information
 * Supports both polling carrier APIs and processing webhook events
 * Runs periodically to sync tracking data and update order statuses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentTrackingScheduler {
    
    private final ShipmentTrackingService shipmentTrackingService;
    
    @Value("${bharatshop.tracking.polling.enabled:true}")
    private boolean pollingEnabled;
    
    @Value("${bharatshop.tracking.polling.interval:900000}") // 15 minutes default
    private long pollingInterval;
    
    @Value("${bharatshop.tracking.batch-size:50}")
    private int batchSize;
    
    @Value("${bharatshop.tracking.max-age-days:30}")
    private int maxAgeDays;

    /**
     * Poll carrier APIs for tracking updates every 15 minutes
     * Only processes active shipments that haven't been delivered yet
     */
    @Scheduled(fixedRateString = "${bharatshop.tracking.polling.interval:900000}") // 15 minutes
    public void pollCarrierTrackingUpdates() {
        if (!pollingEnabled) {
            log.debug("Carrier polling is disabled, skipping tracking update");
            return;
        }
        
        try {
            log.debug("Starting carrier tracking polling job");
            
            // Get active shipments that need tracking updates
            List<Shipment> activeShipments = shipmentTrackingService.getActiveShipmentsForPolling(batchSize);
            
            if (activeShipments.isEmpty()) {
                log.debug("No active shipments found for tracking updates");
                return;
            }
            
            log.info("Processing tracking updates for {} active shipments", activeShipments.size());
            
            int updatedCount = 0;
            int errorCount = 0;
            
            for (Shipment shipment : activeShipments) {
                try {
                    boolean hasUpdates = shipmentTrackingService.pollCarrierTracking(shipment);
                    if (hasUpdates) {
                        updatedCount++;
                        log.debug("Updated tracking for shipment: {}", shipment.getTrackingNumber());
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to update tracking for shipment {}: {}", 
                            shipment.getTrackingNumber(), e.getMessage());
                }
            }
            
            log.info("Tracking polling completed - Updated: {}, Errors: {}, Total: {}", 
                    updatedCount, errorCount, activeShipments.size());
            
        } catch (Exception e) {
            log.error("Failed to execute carrier tracking polling job", e);
        }
    }

    /**
     * Process pending webhook events every 5 minutes
     * Handles carrier webhook notifications that were queued for processing
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processWebhookEvents() {
        try {
            log.debug("Starting webhook event processing");
            
            int processedCount = shipmentTrackingService.processPendingWebhookEvents(batchSize);
            
            if (processedCount > 0) {
                log.info("Processed {} pending webhook events", processedCount);
            } else {
                log.debug("No pending webhook events to process");
            }
            
        } catch (Exception e) {
            log.error("Failed to process webhook events", e);
        }
    }

    /**
     * Update order statuses based on tracking information every 10 minutes
     * Syncs order status with latest shipment tracking data
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void updateOrderStatusFromTracking() {
        try {
            log.debug("Starting order status sync from tracking data");
            
            int updatedOrders = shipmentTrackingService.syncOrderStatusFromTracking(batchSize);
            
            if (updatedOrders > 0) {
                log.info("Updated status for {} orders based on tracking data", updatedOrders);
            } else {
                log.debug("No orders required status updates from tracking");
            }
            
        } catch (Exception e) {
            log.error("Failed to update order statuses from tracking", e);
        }
    }

    /**
     * Clean up old tracking events every 6 hours
     * Removes tracking events older than configured max age to prevent database bloat
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void cleanupOldTrackingEvents() {
        try {
            log.debug("Starting cleanup of old tracking events");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(maxAgeDays);
            int deletedCount = shipmentTrackingService.cleanupOldTrackingEvents(cutoffDate);
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} old tracking events older than {} days", deletedCount, maxAgeDays);
            } else {
                log.debug("No old tracking events found for cleanup");
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup old tracking events", e);
        }
    }

    /**
     * Send delivery notifications for recently delivered orders every 30 minutes
     * Notifies customers when their orders are marked as delivered
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void sendDeliveryNotifications() {
        try {
            log.debug("Starting delivery notification processing");
            
            int notificationsSent = shipmentTrackingService.sendPendingDeliveryNotifications(batchSize);
            
            if (notificationsSent > 0) {
                log.info("Sent {} delivery notifications", notificationsSent);
            } else {
                log.debug("No pending delivery notifications to send");
            }
            
        } catch (Exception e) {
            log.error("Failed to send delivery notifications", e);
        }
    }

    /**
     * Detect and handle delivery exceptions every hour
     * Identifies shipments with delivery issues and triggers appropriate actions
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void handleDeliveryExceptions() {
        try {
            log.debug("Starting delivery exception handling");
            
            int exceptionsHandled = shipmentTrackingService.handleDeliveryExceptions(batchSize);
            
            if (exceptionsHandled > 0) {
                log.info("Handled {} delivery exceptions", exceptionsHandled);
            } else {
                log.debug("No delivery exceptions found to handle");
            }
            
        } catch (Exception e) {
            log.error("Failed to handle delivery exceptions", e);
        }
    }

    /**
     * Log tracking statistics every hour for monitoring
     * Provides operational insights into tracking system performance
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logTrackingStatistics() {
        try {
            var stats = shipmentTrackingService.getTrackingStatistics();
            
            log.info("Tracking Statistics - Active Shipments: {}, Delivered Today: {}, " +
                    "Pending Updates: {}, Exception Count: {}, Avg Delivery Time: {} days",
                    stats.getActiveShipments(),
                    stats.getDeliveredToday(),
                    stats.getPendingUpdates(),
                    stats.getExceptionCount(),
                    stats.getAverageDeliveryDays());
            
        } catch (Exception e) {
            log.error("Failed to log tracking statistics", e);
        }
    }
}
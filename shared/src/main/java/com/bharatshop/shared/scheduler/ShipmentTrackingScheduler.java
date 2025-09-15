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
            System.out.println("Carrier polling is disabled, skipping tracking update");
            return;
        }
        
        try {
            System.out.println("Starting carrier tracking polling job");
            
            // Get active shipments that need tracking updates
            List<Shipment> activeShipments = shipmentTrackingService.getActiveShipmentsForPolling(batchSize);
            
            if (activeShipments.isEmpty()) {
                System.out.println("No active shipments found for tracking updates");
                return;
            }
            
            System.out.println("Processing tracking updates for " + activeShipments.size() + " active shipments");
            
            int updatedCount = 0;
            int errorCount = 0;
            
            for (Shipment shipment : activeShipments) {
                try {
                    boolean hasUpdates = shipmentTrackingService.pollCarrierTracking(shipment);
                    if (hasUpdates) {
                        updatedCount++;
                        System.out.println("Updated tracking for shipment: " + shipment.getTrackingNumber());
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.out.println("Failed to update tracking for shipment " + shipment.getTrackingNumber() + ": " + e.getMessage());
                }
            }
            
            System.out.println("Tracking polling completed - Updated: " + updatedCount + ", Errors: " + errorCount + ", Total: " + activeShipments.size());
            
        } catch (Exception e) {
            System.out.println("Failed to execute carrier tracking polling job: " + e.getMessage());
        }
    }

    /**
     * Process pending webhook events every 5 minutes
     * Handles carrier webhook notifications that were queued for processing
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processWebhookEvents() {
        try {
            System.out.println("Starting webhook event processing");
            
            int processedCount = shipmentTrackingService.processPendingWebhookEvents(batchSize);
            
            if (processedCount > 0) {
                System.out.println("Processed " + processedCount + " pending webhook events");
            } else {
                System.out.println("No pending webhook events to process");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to process webhook events: " + e.getMessage());
        }
    }

    /**
     * Update order statuses based on tracking information every 10 minutes
     * Syncs order status with latest shipment tracking data
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void updateOrderStatusFromTracking() {
        try {
            System.out.println("Starting order status sync from tracking data");
            
            int updatedOrders = shipmentTrackingService.syncOrderStatusFromTracking(batchSize);
            
            if (updatedOrders > 0) {
                System.out.println("Updated status for " + updatedOrders + " orders based on tracking data");
            } else {
                System.out.println("No orders required status updates from tracking");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to update order statuses from tracking: " + e.getMessage());
        }
    }

    /**
     * Clean up old tracking events every 6 hours
     * Removes tracking events older than configured max age to prevent database bloat
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void cleanupOldTrackingEvents() {
        try {
            System.out.println("Starting cleanup of old tracking events");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(maxAgeDays);
            int deletedCount = shipmentTrackingService.cleanupOldTrackingEvents(cutoffDate);
            
            if (deletedCount > 0) {
                System.out.println("Cleaned up " + deletedCount + " old tracking events older than " + maxAgeDays + " days");
            } else {
                System.out.println("No old tracking events found for cleanup");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to cleanup old tracking events: " + e.getMessage());
        }
    }

    /**
     * Send delivery notifications for recently delivered orders every 30 minutes
     * Notifies customers when their orders are marked as delivered
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void sendDeliveryNotifications() {
        try {
            System.out.println("Starting delivery notification processing");
            
            int notificationsSent = shipmentTrackingService.sendPendingDeliveryNotifications(batchSize);
            
            if (notificationsSent > 0) {
                System.out.println("Sent " + notificationsSent + " delivery notifications");
            } else {
                System.out.println("No pending delivery notifications to send");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to send delivery notifications: " + e.getMessage());
        }
    }

    /**
     * Detect and handle delivery exceptions every hour
     * Identifies shipments with delivery issues and triggers appropriate actions
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void handleDeliveryExceptions() {
        try {
            System.out.println("Starting delivery exception handling");
            
            int exceptionsHandled = shipmentTrackingService.handleDeliveryExceptions(batchSize);
            
            if (exceptionsHandled > 0) {
                System.out.println("Handled " + exceptionsHandled + " delivery exceptions");
            } else {
                System.out.println("No delivery exceptions found to handle");
            }
            
        } catch (Exception e) {
            System.out.println("Failed to handle delivery exceptions: " + e.getMessage());
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
            
            System.out.println("Tracking Statistics - Active Shipments: " + stats.getActiveShipments() + 
                    ", Delivered Today: " + stats.getDeliveredToday() + 
                    ", Pending Updates: " + stats.getPendingUpdates() + 
                    ", Exception Count: " + stats.getExceptionCount() + 
                    ", Avg Delivery Time: " + stats.getAverageDeliveryDays() + " days");
            
        } catch (Exception e) {
            System.out.println("Failed to log tracking statistics: " + e.getMessage());
        }
    }
}
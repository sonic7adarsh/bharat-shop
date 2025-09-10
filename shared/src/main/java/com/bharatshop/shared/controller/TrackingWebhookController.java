package com.bharatshop.shared.controller;

import com.bharatshop.shared.service.ShipmentTrackingService;
import com.bharatshop.shared.entity.Shipment;
import com.bharatshop.shared.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * REST controller for handling carrier tracking webhooks
 * Receives real-time tracking updates from shipping carriers
 * Supports multiple carriers with pluggable webhook formats
 */
@RestController
@RequestMapping("/api/webhooks/tracking")
@RequiredArgsConstructor
@Slf4j
public class TrackingWebhookController {

    private final ShipmentTrackingService shipmentTrackingService;
    private final ShipmentRepository shipmentRepository;
    
    @Value("${bharatshop.tracking.webhook.secret:default-webhook-secret}")
    private String webhookSecret;
    
    @Value("${bharatshop.tracking.webhook.delhivery.enabled:true}")
    private boolean delhiveryWebhookEnabled;
    
    @Value("${bharatshop.tracking.webhook.bluedart.enabled:true}")
    private boolean bluedartWebhookEnabled;

    /**
     * Handle Delhivery tracking webhooks
     * POST /api/webhooks/tracking/delhivery
     */
    @PostMapping("/delhivery")
    public ResponseEntity<Map<String, String>> handleDelhiveryWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Delhivery-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        if (!delhiveryWebhookEnabled) {
            log.warn("Delhivery webhook received but webhooks are disabled");
            return ResponseEntity.ok(Map.of("status", "disabled"));
        }
        
        try {
            log.info("Received Delhivery webhook: {}", payload);
            
            // Verify webhook signature if provided
            if (signature != null && !verifyDelhiverySignature(payload, signature)) {
                log.warn("Invalid Delhivery webhook signature");
                return ResponseEntity.badRequest().body(Map.of("status", "invalid_signature"));
            }
            
            // Extract tracking information
            String trackingNumber = extractTrackingNumber(payload, "waybill");
            if (trackingNumber == null) {
                log.warn("No tracking number found in Delhivery webhook payload");
                return ResponseEntity.badRequest().body(Map.of("status", "missing_tracking_number"));
            }
            
            // Find shipment
            Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipmentOpt.isEmpty()) {
                log.warn("Shipment not found for tracking number: {}", trackingNumber);
                return ResponseEntity.ok(Map.of("status", "shipment_not_found"));
            }
            
            // Process tracking event
            ShipmentTrackingService.TrackingEvent event = parseDelhiveryWebhook(payload);
            if (event != null) {
                boolean processed = shipmentTrackingService.processTrackingEvent(shipmentOpt.get(), event);
                log.info("Delhivery webhook processed for {}: {}", trackingNumber, processed);
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
            
        } catch (Exception e) {
            log.error("Failed to process Delhivery webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage())
            );
        }
    }

    /**
     * Handle BlueDart tracking webhooks
     * POST /api/webhooks/tracking/bluedart
     */
    @PostMapping("/bluedart")
    public ResponseEntity<Map<String, String>> handleBluedartWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-BlueDart-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        if (!bluedartWebhookEnabled) {
            log.warn("BlueDart webhook received but webhooks are disabled");
            return ResponseEntity.ok(Map.of("status", "disabled"));
        }
        
        try {
            log.info("Received BlueDart webhook: {}", payload);
            
            // Verify webhook signature if provided
            if (signature != null && !verifyBluedartSignature(payload, signature)) {
                log.warn("Invalid BlueDart webhook signature");
                return ResponseEntity.badRequest().body(Map.of("status", "invalid_signature"));
            }
            
            // Extract tracking information
            String trackingNumber = extractTrackingNumber(payload, "awb_number");
            if (trackingNumber == null) {
                log.warn("No tracking number found in BlueDart webhook payload");
                return ResponseEntity.badRequest().body(Map.of("status", "missing_tracking_number"));
            }
            
            // Find shipment
            Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipmentOpt.isEmpty()) {
                log.warn("Shipment not found for tracking number: {}", trackingNumber);
                return ResponseEntity.ok(Map.of("status", "shipment_not_found"));
            }
            
            // Process tracking event
            ShipmentTrackingService.TrackingEvent event = parseBluedartWebhook(payload);
            if (event != null) {
                boolean processed = shipmentTrackingService.processTrackingEvent(shipmentOpt.get(), event);
                log.info("BlueDart webhook processed for {}: {}", trackingNumber, processed);
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
            
        } catch (Exception e) {
            log.error("Failed to process BlueDart webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage())
            );
        }
    }

    /**
     * Handle DTDC tracking webhooks
     * POST /api/webhooks/tracking/dtdc
     */
    @PostMapping("/dtdc")
    public ResponseEntity<Map<String, String>> handleDTDCWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-DTDC-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        try {
            log.info("Received DTDC webhook: {}", payload);
            
            // Extract tracking information
            String trackingNumber = extractTrackingNumber(payload, "reference_no");
            if (trackingNumber == null) {
                log.warn("No tracking number found in DTDC webhook payload");
                return ResponseEntity.badRequest().body(Map.of("status", "missing_tracking_number"));
            }
            
            // Find shipment
            Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipmentOpt.isEmpty()) {
                log.warn("Shipment not found for tracking number: {}", trackingNumber);
                return ResponseEntity.ok(Map.of("status", "shipment_not_found"));
            }
            
            // Process tracking event
            ShipmentTrackingService.TrackingEvent event = parseDTDCWebhook(payload);
            if (event != null) {
                boolean processed = shipmentTrackingService.processTrackingEvent(shipmentOpt.get(), event);
                log.info("DTDC webhook processed for {}: {}", trackingNumber, processed);
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
            
        } catch (Exception e) {
            log.error("Failed to process DTDC webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage())
            );
        }
    }

    /**
     * Generic webhook handler for other carriers
     * POST /api/webhooks/tracking/generic
     */
    @PostMapping("/generic")
    public ResponseEntity<Map<String, String>> handleGenericWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestParam("carrier") String carrierName,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        try {
            log.info("Received generic webhook for carrier {}: {}", carrierName, payload);
            
            // Extract tracking information (try common field names)
            String trackingNumber = extractTrackingNumber(payload, 
                    "tracking_number", "waybill", "awb_number", "reference_no", "shipment_id");
            
            if (trackingNumber == null) {
                log.warn("No tracking number found in generic webhook payload for carrier: {}", carrierName);
                return ResponseEntity.badRequest().body(Map.of("status", "missing_tracking_number"));
            }
            
            // Find shipment
            Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipmentOpt.isEmpty()) {
                log.warn("Shipment not found for tracking number: {} (carrier: {})", trackingNumber, carrierName);
                return ResponseEntity.ok(Map.of("status", "shipment_not_found"));
            }
            
            // Process tracking event
            ShipmentTrackingService.TrackingEvent event = parseGenericWebhook(payload, carrierName);
            if (event != null) {
                boolean processed = shipmentTrackingService.processTrackingEvent(shipmentOpt.get(), event);
                log.info("Generic webhook processed for {} ({}): {}", trackingNumber, carrierName, processed);
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
            
        } catch (Exception e) {
            log.error("Failed to process generic webhook for carrier {}: {}", carrierName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage())
            );
        }
    }

    /**
     * Health check endpoint for webhook service
     * GET /api/webhooks/tracking/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", LocalDateTime.now(),
                "webhooks", Map.of(
                        "delhivery", delhiveryWebhookEnabled,
                        "bluedart", bluedartWebhookEnabled
                )
        ));
    }

    // Helper methods

    /**
     * Extract tracking number from payload using multiple possible field names
     */
    private String extractTrackingNumber(Map<String, Object> payload, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object value = payload.get(fieldName);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return null;
    }

    /**
     * Verify Delhivery webhook signature
     */
    private boolean verifyDelhiverySignature(Map<String, Object> payload, String signature) {
        try {
            // Implementation depends on Delhivery's signature algorithm
            // This is a placeholder implementation
            String payloadString = payload.toString();
            String expectedSignature = generateHmacSha256(payloadString, webhookSecret);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Failed to verify Delhivery signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify BlueDart webhook signature
     */
    private boolean verifyBluedartSignature(Map<String, Object> payload, String signature) {
        try {
            // Implementation depends on BlueDart's signature algorithm
            // This is a placeholder implementation
            String payloadString = payload.toString();
            String expectedSignature = generateHmacSha256(payloadString, webhookSecret);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Failed to verify BlueDart signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Parse Delhivery webhook payload
     */
    private ShipmentTrackingService.TrackingEvent parseDelhiveryWebhook(Map<String, Object> payload) {
        try {
            // Parse Delhivery-specific webhook format
            // This is a simplified implementation - actual format depends on Delhivery API
            String status = (String) payload.get("status");
            String location = (String) payload.get("location");
            String description = (String) payload.get("instructions");
            String eventCode = (String) payload.get("status_code");
            
            return ShipmentTrackingService.TrackingEvent.builder()
                    .status(status)
                    .eventDate(LocalDateTime.now())
                    .location(location)
                    .description(description)
                    .carrierEventCode(eventCode)
                    .eventType("WEBHOOK")
                    .isMilestone(isMilestoneStatus(status))
                    .isException(isExceptionStatus(status))
                    .rawData(payload.toString())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse Delhivery webhook: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse BlueDart webhook payload
     */
    private ShipmentTrackingService.TrackingEvent parseBluedartWebhook(Map<String, Object> payload) {
        try {
            // Parse BlueDart-specific webhook format
            // This is a simplified implementation - actual format depends on BlueDart API
            String status = (String) payload.get("status");
            String location = (String) payload.get("location");
            String description = (String) payload.get("remarks");
            String eventCode = (String) payload.get("event_code");
            
            return ShipmentTrackingService.TrackingEvent.builder()
                    .status(status)
                    .eventDate(LocalDateTime.now())
                    .location(location)
                    .description(description)
                    .carrierEventCode(eventCode)
                    .eventType("WEBHOOK")
                    .isMilestone(isMilestoneStatus(status))
                    .isException(isExceptionStatus(status))
                    .rawData(payload.toString())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse BlueDart webhook: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse DTDC webhook payload
     */
    private ShipmentTrackingService.TrackingEvent parseDTDCWebhook(Map<String, Object> payload) {
        try {
            // Parse DTDC-specific webhook format
            String status = (String) payload.get("status");
            String location = (String) payload.get("location");
            String description = (String) payload.get("description");
            
            return ShipmentTrackingService.TrackingEvent.builder()
                    .status(status)
                    .eventDate(LocalDateTime.now())
                    .location(location)
                    .description(description)
                    .eventType("WEBHOOK")
                    .isMilestone(isMilestoneStatus(status))
                    .isException(isExceptionStatus(status))
                    .rawData(payload.toString())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse DTDC webhook: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse generic webhook payload
     */
    private ShipmentTrackingService.TrackingEvent parseGenericWebhook(Map<String, Object> payload, String carrierName) {
        try {
            // Parse generic webhook format
            String status = extractField(payload, "status", "event_type", "state");
            String location = extractField(payload, "location", "city", "place");
            String description = extractField(payload, "description", "message", "remarks", "notes");
            String eventCode = extractField(payload, "event_code", "code", "status_code");
            
            return ShipmentTrackingService.TrackingEvent.builder()
                    .status(status)
                    .eventDate(LocalDateTime.now())
                    .location(location)
                    .description(description)
                    .carrierEventCode(eventCode)
                    .eventType("WEBHOOK")
                    .isMilestone(isMilestoneStatus(status))
                    .isException(isExceptionStatus(status))
                    .rawData(payload.toString())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse generic webhook for {}: {}", carrierName, e.getMessage());
            return null;
        }
    }

    /**
     * Extract field value using multiple possible field names
     */
    private String extractField(Map<String, Object> payload, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object value = payload.get(fieldName);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return null;
    }

    /**
     * Check if status represents a milestone event
     */
    private boolean isMilestoneStatus(String status) {
        if (status == null) return false;
        String lowerStatus = status.toLowerCase();
        return lowerStatus.contains("delivered") ||
               lowerStatus.contains("out for delivery") ||
               lowerStatus.contains("shipped") ||
               lowerStatus.contains("in transit");
    }

    /**
     * Check if status represents an exception event
     */
    private boolean isExceptionStatus(String status) {
        if (status == null) return false;
        String lowerStatus = status.toLowerCase();
        return lowerStatus.contains("exception") ||
               lowerStatus.contains("failed") ||
               lowerStatus.contains("undelivered") ||
               lowerStatus.contains("returned") ||
               lowerStatus.contains("rto");
    }
}
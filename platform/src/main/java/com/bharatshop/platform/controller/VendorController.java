package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.PlatformVendorService;
import com.bharatshop.shared.entity.Vendor;
import com.bharatshop.shared.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
// import java.util.UUID; // Replaced with Long

/**
 * REST controller for vendor management operations.
 * Handles vendor profile management, domain availability, and template setup.
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Slf4j
public class VendorController {
    
    private final PlatformVendorService vendorService;
    private final FeatureFlagService featureFlagService;
    
    /**
     * Get current vendor profile
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> getVendorProfile(Principal principal) {
        try {
            // Extract vendor ID from principal (this should be implemented based on your JWT structure)
            Long vendorId = extractVendorIdFromPrincipal(principal);
            
            Optional<Vendor> vendor = vendorService.getVendorById(vendorId);
            
            if (vendor.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(vendor.get());
            
        } catch (Exception e) {
            log.error("Error getting vendor profile", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get vendor profile"));
        }
    }
    
    /**
     * Update current vendor profile
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> updateVendorProfile(
            @RequestBody Vendor vendorUpdate,
            Principal principal) {
        try {
            // Extract vendor ID from principal
            Long vendorId = extractVendorIdFromPrincipal(principal);
            
            Vendor updatedVendor = vendorService.updateVendor(vendorId, vendorUpdate);
            
            return ResponseEntity.ok(updatedVendor);
            
        } catch (RuntimeException e) {
            log.error("Error updating vendor profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating vendor profile", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to update vendor profile"));
        }
    }
    
    /**
     * Check domain availability
     */
    @PostMapping("/domain-availability")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> checkDomainAvailability(@RequestBody Map<String, String> request, Principal principal) {
        try {
            Long vendorId = extractVendorIdFromPrincipal(principal);
            
            // Enforce custom domain feature access
            featureFlagService.enforceFeatureAccess(vendorId, "customDomain");
            
            String domain = request.get("domain");
            
            if (domain == null || domain.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Domain is required"));
            }
            
            boolean isAvailable = vendorService.isDomainAvailable(domain.trim().toLowerCase());
            
            return ResponseEntity.ok(Map.of(
                "domain", domain,
                "available", isAvailable
            ));
            
        } catch (Exception e) {
            log.error("Error checking domain availability", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to check domain availability"));
        }
    }
    
    /**
     * Setup vendor template
     */
    @PostMapping("/setup-template")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> setupTemplate(
            @RequestBody Map<String, String> request,
            Principal principal) {
        try {
            String templateConfig = request.get("templateConfig");
            
            if (templateConfig == null || templateConfig.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Template configuration is required"));
            }
            
            // Extract vendor ID from principal
            Long vendorId = extractVendorIdFromPrincipal(principal);
            
            Vendor updatedVendor = vendorService.setupVendorTemplate(vendorId, templateConfig);
            
            return ResponseEntity.ok(Map.of(
                "message", "Template setup successful",
                "vendor", updatedVendor
            ));
            
        } catch (RuntimeException e) {
            log.error("Error setting up template: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting up template", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to setup template"));
        }
    }
    
    /**
     * Extract vendor ID from JWT principal
     * This is a placeholder implementation - should be implemented based on your JWT structure
     */
    private Long extractVendorIdFromPrincipal(Principal principal) {
        // This should extract the vendor ID from the JWT token or user context
        // For now, returning a placeholder Long
        // In a real implementation, you would:
        // 1. Extract the user ID from the JWT token
        // 2. Look up the vendor associated with that user
        // 3. Return the vendor ID
        
        log.warn("Using placeholder vendor ID extraction - implement based on your JWT structure");
        return 1L;
    }
}
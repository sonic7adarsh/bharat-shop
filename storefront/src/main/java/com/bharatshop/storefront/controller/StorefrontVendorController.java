package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.service.VendorService;
import com.bharatshop.shared.entity.Vendor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for storefront vendor queries.
 * Provides public access to vendor settings and configuration for storefronts.
 */
@RestController
@RequestMapping("/api/storefront/vendor")
@RequiredArgsConstructor
@Slf4j
public class StorefrontVendorController {
    
    private final VendorService vendorService;
    
    /**
     * Get vendor settings by domain
     * This endpoint allows storefronts to retrieve vendor configuration
     * based on the domain they are accessing from
     */
    @GetMapping("/settings")
    public ResponseEntity<?> getVendorSettingsByDomain(
            @RequestParam("domain") String domain) {
        try {
            if (domain == null || domain.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .build();
            }
            
            Optional<Vendor> vendor = vendorService.getVendorByDomain(domain.trim().toLowerCase());
            
            if (vendor.isEmpty()) {
                return ResponseEntity.notFound()
                    .build();
            }
            
            Vendor vendorData = vendor.get();
            
            // Return only public vendor settings (exclude sensitive information)
            Map<String, Object> publicSettings = new HashMap<>();
            publicSettings.put("id", vendorData.getId());
            publicSettings.put("name", vendorData.getName());
            publicSettings.put("storeName", vendorData.getStoreName());
            publicSettings.put("description", vendorData.getDescription());
            publicSettings.put("logo", vendorData.getLogo() != null ? vendorData.getLogo() : "");
            publicSettings.put("themeConfig", vendorData.getThemeConfig() != null ? vendorData.getThemeConfig() : "{}");
            publicSettings.put("domain", vendorData.getDomain());
            publicSettings.put("currency", vendorData.getCurrency());
            publicSettings.put("preferredLanguage", vendorData.getPreferredLanguage());
            publicSettings.put("status", vendorData.getStatus());
            publicSettings.put("isActive", vendorData.getIsActive());
            
            return ResponseEntity.ok(publicSettings);
            
        } catch (Exception e) {
            log.error("Error getting vendor settings for domain: {}", domain, e);
            return ResponseEntity.internalServerError()
                .build();
        }
    }
    
    /**
     * Get vendor settings by vendor ID
     * Alternative endpoint for retrieving vendor settings when domain is not available
     */
    @GetMapping("/settings/{vendorId}")
    public ResponseEntity<?> getVendorSettingsById(@PathVariable UUID vendorId) {
        try {
            Optional<Vendor> vendor = vendorService.getVendorById(vendorId);
            
            if (vendor.isEmpty()) {
                return ResponseEntity.notFound()
                    .build();
            }
            
            Vendor vendorData = vendor.get();
            
            // Check if vendor is active
            if (!vendorData.getIsActive()) {
                return ResponseEntity.notFound()
                    .build();
            }
            
            // Return only public vendor settings
            Map<String, Object> publicSettings = new HashMap<>();
            publicSettings.put("id", vendorData.getId());
            publicSettings.put("name", vendorData.getName());
            publicSettings.put("storeName", vendorData.getStoreName());
            publicSettings.put("description", vendorData.getDescription());
            publicSettings.put("logo", vendorData.getLogo() != null ? vendorData.getLogo() : "");
            publicSettings.put("themeConfig", vendorData.getThemeConfig() != null ? vendorData.getThemeConfig() : "{}");
            publicSettings.put("domain", vendorData.getDomain());
            publicSettings.put("currency", vendorData.getCurrency());
            publicSettings.put("preferredLanguage", vendorData.getPreferredLanguage());
            publicSettings.put("status", vendorData.getStatus());
            publicSettings.put("isActive", vendorData.getIsActive());
            
            return ResponseEntity.ok(publicSettings);
            
        } catch (Exception e) {
            log.error("Error getting vendor settings for ID: {}", vendorId, e);
            return ResponseEntity.internalServerError()
                .build();
        }
    }
    
    /**
     * Get vendor theme configuration
     * Dedicated endpoint for retrieving theme configuration
     */
    @GetMapping("/theme")
    public ResponseEntity<?> getVendorTheme(@RequestParam("domain") String domain) {
        try {
            if (domain == null || domain.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .build();
            }
            
            Optional<Vendor> vendor = vendorService.getVendorByDomain(domain.trim().toLowerCase());
            
            if (vendor.isEmpty()) {
                return ResponseEntity.notFound()
                    .build();
            }
            
            Vendor vendorData = vendor.get();
            
            // Check if vendor is active
            if (!vendorData.getIsActive()) {
                return ResponseEntity.notFound()
                    .build();
            }
            
            Map<String, Object> themeData = new HashMap<>();
            themeData.put("domain", vendorData.getDomain());
            themeData.put("storeName", vendorData.getStoreName());
            themeData.put("logo", vendorData.getLogo() != null ? vendorData.getLogo() : "");
            themeData.put("themeConfig", vendorData.getThemeConfig() != null ? vendorData.getThemeConfig() : "{}");
            themeData.put("currency", vendorData.getCurrency());
            themeData.put("preferredLanguage", vendorData.getPreferredLanguage());
            
            return ResponseEntity.ok(themeData);
            
        } catch (Exception e) {
            log.error("Error getting vendor theme for domain: {}", domain, e);
            return ResponseEntity.internalServerError()
                .build();
        }
    }
    
    /**
     * Health check endpoint for vendor domain
     * Allows checking if a vendor domain is active and accessible
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkVendorHealth(@RequestParam("domain") String domain) {
        try {
            if (domain == null || domain.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Domain parameter is required"));
            }
            
            Optional<Vendor> vendor = vendorService.getVendorByDomain(domain.trim().toLowerCase());
            
            if (vendor.isEmpty()) {
                Map<String, Object> healthData = new HashMap<>();
                healthData.put("domain", domain);
                healthData.put("status", "not_found");
                healthData.put("active", false);
                return ResponseEntity.ok(healthData);
            }
            
            Vendor vendorData = vendor.get();
            
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("domain", domain);
            healthData.put("status", vendorData.getStatus().toString().toLowerCase());
            healthData.put("active", vendorData.getIsActive());
            healthData.put("storeName", vendorData.getStoreName());
            return ResponseEntity.ok(healthData);
            
        } catch (Exception e) {
            log.error("Error checking vendor health for domain: {}", domain, e);
            return ResponseEntity.internalServerError()
                .build();
        }
    }
}
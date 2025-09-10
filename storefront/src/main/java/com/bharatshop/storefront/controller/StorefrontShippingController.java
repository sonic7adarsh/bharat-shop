package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.ShippingQuoteRequest;
import com.bharatshop.storefront.dto.ShippingQuoteResponse;
import com.bharatshop.storefront.service.ShippingService;
import com.bharatshop.storefront.shared.ApiResponse;
import com.bharatshop.shared.service.ShippingValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for storefront shipping operations.
 * Provides shipping quote calculation and delivery information for customers.
 */
@RestController
@RequestMapping("/store/shipping")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Storefront Shipping", description = "Public APIs for shipping quotes and delivery information")
public class StorefrontShippingController {

    private final ShippingService shippingService;
    private final ShippingValidationService validationService;

    /**
     * Get shipping quote for destination and package details
     * GET /store/shipping/quote?destPincode=110001&weight=2.5&cod=true&orderValue=1500
     */
    @GetMapping("/quote")
    @Operation(summary = "Get shipping quote", 
               description = "Calculate shipping cost and delivery estimate for given destination and package details")
    public ResponseEntity<ApiResponse<ShippingQuoteResponse>> getShippingQuote(
            @Parameter(description = "Destination pincode (6 digits)", required = true, example = "110001")
            @RequestParam("destPincode") 
            @Pattern(regexp = "^[0-9]{6}$", message = "Destination pincode must be a valid 6-digit pincode") 
            String destPincode,
            
            @Parameter(description = "Package weight in kg", required = true, example = "2.5")
            @RequestParam("weight") 
            @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg") 
            BigDecimal weight,
            
            @Parameter(description = "Cash on Delivery flag", example = "true")
            @RequestParam(value = "cod", defaultValue = "false") 
            boolean cod,
            
            @Parameter(description = "Order value for COD charge calculation", example = "1500.00")
            @RequestParam(value = "orderValue", required = false) 
            BigDecimal orderValue,
            
            @Parameter(description = "Express delivery preference", example = "false")
            @RequestParam(value = "express", defaultValue = "false") 
            boolean express,
            
            @Parameter(description = "Package dimensions", example = "30x20x10 cm")
            @RequestParam(value = "dimensions", required = false) 
            String dimensions,
            
            @Parameter(description = "Number of packages", example = "1")
            @RequestParam(value = "packageCount", defaultValue = "1") 
            Integer packageCount,
            
            @Parameter(description = "Fragile item flag", example = "false")
            @RequestParam(value = "fragile", defaultValue = "false") 
            boolean fragile,
            
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            // Build shipping quote request
            ShippingQuoteRequest request = ShippingQuoteRequest.builder()
                    .destPincode(destPincode)
                    .weight(weight)
                    .cod(cod)
                    .orderValue(orderValue)
                    .expressDelivery(express)
                    .dimensions(dimensions)
                    .packageCount(packageCount)
                    .fragile(fragile)
                    .build();
            
            // Basic validation (detailed validation removed due to circular dependency)
            if (destPincode == null || weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Shipping quote request validation failed: missing or invalid required fields");
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Validation failed: missing or invalid required fields")
                );
            }
            
            log.info("Processing shipping quote request for pincode: {}, weight: {}kg, COD: {}, tenant: {}",
                    destPincode, weight, cod, tenantId);
            
            // Calculate shipping quote
            ShippingQuoteResponse response;
            if (express) {
                response = shippingService.calculateExpressShippingQuote(request, tenantId);
            } else {
                response = shippingService.calculateShippingQuote(request, tenantId);
            }
            
            if (!response.isAvailable()) {
                log.warn("Shipping not available for pincode: {} and tenant: {}", destPincode, tenantId);
                return ResponseEntity.ok(
                        ApiResponse.success(response, response.getMessage())
                );
            }
            
            log.info("Shipping quote calculated successfully - Cost: {}, Total: {}, SLA: {} days",
                    response.getShippingCost(), response.getTotalCost(), response.getSlaDays());
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Shipping quote calculated successfully")
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid shipping quote request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Invalid request: " + e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error calculating shipping quote: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Unable to calculate shipping quote. Please try again later.")
            );
        }
    }
    
    /**
     * Check if delivery is available to a pincode
     * GET /store/shipping/availability?pincode=110001
     */
    @GetMapping("/availability")
    @Operation(summary = "Check delivery availability", 
               description = "Check if delivery is available to a specific pincode")
    public ResponseEntity<ApiResponse<Boolean>> checkDeliveryAvailability(
            @Parameter(description = "Pincode to check", required = true, example = "110001")
            @RequestParam("pincode") 
            @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be a valid 6-digit pincode") 
            String pincode,
            
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            log.info("Checking delivery availability for pincode: {}, tenant: {}", pincode, tenantId);
            
            boolean available = shippingService.isDeliveryAvailable(pincode, tenantId);
            
            String message = available ? 
                    "Delivery is available to this location" : 
                    "Delivery is not available to this location";
            
            return ResponseEntity.ok(
                    ApiResponse.success(available, message)
            );
            
        } catch (Exception e) {
            log.error("Error checking delivery availability: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Unable to check delivery availability. Please try again later.")
            );
        }
    }
    
    /**
     * Get all service zones for the tenant (for debugging/admin purposes)
     * GET /store/shipping/zones
     */
    @GetMapping("/zones")
    @Operation(summary = "Get service zones", 
               description = "Get all active service zones for the current tenant")
    public ResponseEntity<ApiResponse<List<com.bharatshop.shared.entity.ServiceZone>>> getServiceZones(
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            log.info("Retrieving service zones for tenant: {}", tenantId);
            
            List<com.bharatshop.shared.entity.ServiceZone> zones = shippingService.getServiceZonesByTenant(tenantId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(zones, "Service zones retrieved successfully")
            );
            
        } catch (Exception e) {
            log.error("Error retrieving service zones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Unable to retrieve service zones. Please try again later.")
            );
        }
    }
    
    /**
     * Extract tenant ID from request headers or domain
     * This should match the pattern used in other controllers
     */
    private Long extractTenantId(HttpServletRequest request) {
        // Implementation should match the tenant resolution strategy used in other controllers
        // This is a placeholder - actual implementation depends on how tenant is identified
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader != null) {
            try {
                return Long.parseLong(tenantHeader);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid tenant ID format");
            }
        }
        
        // Alternative: extract from subdomain or other mechanism
        // For now, throw exception to indicate missing tenant
        throw new IllegalArgumentException("Tenant ID is required");
    }
}
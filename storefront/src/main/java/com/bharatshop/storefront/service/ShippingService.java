package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.ServiceZone;
import com.bharatshop.storefront.dto.ShippingQuoteRequest;
import com.bharatshop.storefront.dto.ShippingQuoteResponse;
import com.bharatshop.storefront.repository.ServiceZoneRepository;
import com.bharatshop.shared.service.ShippingValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling shipping calculations and quotes.
 * Provides shipping cost estimation based on service zones and delivery options.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ServiceZoneRepository serviceZoneRepository;
    private final ShippingValidationService validationService;

    /**
     * Calculate shipping quote for given parameters
     *
     * @param request Shipping quote request with destination, weight, and COD info
     * @param tenantId Tenant ID for zone lookup
     * @return Shipping quote response with cost and delivery estimates
     */
    public ShippingQuoteResponse calculateShippingQuote(ShippingQuoteRequest request, Long tenantId) {
        log.info("Calculating shipping quote for pincode: {}, weight: {}kg, COD: {}, tenant: {}",
                request.getDestPincode(), request.getWeight(), request.isCod(), tenantId);

        // Basic validation (detailed validation removed due to circular dependency)
        if (request == null || request.getDestPincode() == null || request.getWeight() == null) {
            log.warn("Invalid shipping quote request: missing required fields");
            return ShippingQuoteResponse.builder()
                    .available(false)
                    .message("Invalid request: missing required fields")
                    .build();
        }

        // Find applicable service zone
        Optional<ServiceZone> serviceZone = findApplicableServiceZone(request.getDestPincode(), tenantId);

        if (serviceZone.isEmpty()) {
            log.warn("No service zone found for pincode: {} and tenant: {}", request.getDestPincode(), tenantId);
            return ShippingQuoteResponse.builder()
                    .available(false)
                    .message("Delivery not available to this location")
                    .build();
        }

        ServiceZone zone = serviceZone.get();

        // Basic service zone validation (detailed validation removed due to circular dependency)
        if (zone.getBaseRate() == null || zone.getPerKgRate() == null) {
            log.error("Invalid service zone configuration for zone {}: missing rate information", zone.getId());
            return ShippingQuoteResponse.builder()
                    .available(false)
                    .message("Service configuration error. Please contact support.")
                    .build();
        }

        // Check if COD is supported
        if (request.isCod() && !zone.getCodAllowed()) {
            log.warn("COD not allowed for pincode: {} in zone: {}", request.getDestPincode(), zone.getName());
            return ShippingQuoteResponse.builder()
                    .available(false)
                    .message("Cash on Delivery not available for this location")
                    .build();
        }

        // Calculate shipping cost
        BigDecimal shippingCost = calculateShippingCost(zone, request.getWeight());
        BigDecimal codCharges = request.isCod() ? calculateCodCharges(zone, request.getOrderValue()) : BigDecimal.ZERO;
        BigDecimal totalCost = shippingCost.add(codCharges);

        // Calculate delivery dates
        LocalDateTime estimatedDeliveryDate = calculateEstimatedDeliveryDate(zone.getSlaDays());
        LocalDateTime promisedDeliveryDate = estimatedDeliveryDate.plusDays(1); // Add buffer day

        log.info("Shipping quote calculated - Cost: {}, COD: {}, Total: {}, SLA: {} days",
                shippingCost, codCharges, totalCost, zone.getSlaDays());

        return ShippingQuoteResponse.builder()
                .available(true)
                .serviceZoneId(zone.getId())
                .serviceZoneName(zone.getName())
                .shippingCost(shippingCost)
                .codCharges(codCharges)
                .totalCost(totalCost)
                .slaDays(zone.getSlaDays())
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .promisedDeliveryDate(promisedDeliveryDate)
                .codAllowed(zone.getCodAllowed())
                .expressDeliveryAvailable(false) // Not implemented yet
                .message("Shipping available")
                .build();
    }

    /**
     * Find applicable service zone for given pincode and tenant
     */
    private Optional<ServiceZone> findApplicableServiceZone(String pincode, Long tenantId) {
        // First try to find by explicit pincode list
        List<ServiceZone> explicitZones = serviceZoneRepository.findByTenantIdAndExplicitPincodesContaining(tenantId, pincode);
        if (!explicitZones.isEmpty()) {
            return Optional.of(explicitZones.get(0)); // Return first match
        }

        // Then try to find by pincode range
        List<ServiceZone> rangeZones = serviceZoneRepository.findByTenantIdAndPincodeRange(tenantId, pincode);
        if (!rangeZones.isEmpty()) {
            return Optional.of(rangeZones.get(0)); // Return first match
        }

        return Optional.empty();
    }

    /**
     * Calculate shipping cost based on zone rates and weight
     */
    private BigDecimal calculateShippingCost(ServiceZone zone, BigDecimal weight) {
        BigDecimal baseRate = zone.getBaseRate();
        BigDecimal perKgRate = zone.getPerKgRate();
        BigDecimal minCharge = zone.getMinCharge();

        // Calculate weight-based cost
        BigDecimal weightCost = baseRate.add(perKgRate.multiply(weight));

        // Apply minimum charge
        BigDecimal finalCost = weightCost.max(minCharge);

        return finalCost.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate COD charges based on order value
     */
    private BigDecimal calculateCodCharges(ServiceZone zone, BigDecimal orderValue) {
        if (orderValue == null || orderValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Use the fixed COD charges from the zone configuration
        BigDecimal codCharges = zone.getCodCharges() != null ? zone.getCodCharges() : BigDecimal.ZERO;
        
        return codCharges.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate estimated delivery date based on SLA days
     */
    private LocalDateTime calculateEstimatedDeliveryDate(Integer slaDays) {
        if (slaDays == null || slaDays <= 0) {
            slaDays = 7; // Default 7 days
        }

        LocalDateTime now = LocalDateTime.now();
        
        // If order is placed after 6 PM, consider next day as start
        if (now.getHour() >= 18) {
            now = now.plusDays(1);
        }

        return now.plusDays(slaDays);
    }

    /**
     * Get all service zones for a tenant
     */
    public List<ServiceZone> getServiceZonesByTenant(Long tenantId) {
        return serviceZoneRepository.findByTenantIdAndDeletedAtIsNullOrderByName(tenantId);
    }

    /**
     * Check if delivery is available to a pincode
     */
    public boolean isDeliveryAvailable(String pincode, Long tenantId) {
        return findApplicableServiceZone(pincode, tenantId).isPresent();
    }

    /**
     * Get service zone by ID for a tenant
     */
    public Optional<ServiceZone> getServiceZoneById(Long serviceZoneId, Long tenantId) {
        return serviceZoneRepository.findByIdAndTenantIdAndDeletedAtIsNull(serviceZoneId, tenantId);
    }

    /**
     * Calculate express delivery options if available
     */
    public ShippingQuoteResponse calculateExpressShippingQuote(ShippingQuoteRequest request, Long tenantId) {
        ShippingQuoteResponse standardQuote = calculateShippingQuote(request, tenantId);
        
        if (!standardQuote.isAvailable() || !standardQuote.isExpressDeliveryAvailable()) {
            return standardQuote;
        }

        // Express delivery: 50% higher cost, half the delivery time
        BigDecimal expressCost = standardQuote.getShippingCost().multiply(new BigDecimal("1.5"));
        BigDecimal totalExpressCost = expressCost.add(standardQuote.getCodCharges());
        
        Integer expressSlaDays = Math.max(1, standardQuote.getSlaDays() / 2);
        LocalDateTime expressDeliveryDate = calculateEstimatedDeliveryDate(expressSlaDays);

        return ShippingQuoteResponse.builder()
                .available(true)
                .serviceZoneId(standardQuote.getServiceZoneId())
                .serviceZoneName(standardQuote.getServiceZoneName() + " (Express)")
                .shippingCost(expressCost)
                .codCharges(standardQuote.getCodCharges())
                .totalCost(totalExpressCost)
                .slaDays(expressSlaDays)
                .estimatedDeliveryDate(expressDeliveryDate)
                .promisedDeliveryDate(expressDeliveryDate)
                .codAllowed(standardQuote.isCodAllowed())
                .expressDeliveryAvailable(true)
                .message("Express delivery available")
                .build();
    }
}
package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.Vendor;
import com.bharatshop.shared.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


/**
 * Storefront vendor service for read-only vendor operations.
 * Provides vendor information for storefront queries.
 */
@Service("storefrontVendorService")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StorefrontVendorService {
    
    // Manual log field to bypass Lombok issues
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorefrontVendorService.class);
    
    private final VendorRepository vendorRepository;
    
    /**
     * Get vendor by domain
     * Used by storefronts to retrieve vendor configuration
     */
    public Optional<Vendor> getVendorByDomain(String domain) {
        log.debug("Getting vendor by domain: {}", domain);
        return vendorRepository.findByDomain(domain);
    }
    
    /**
     * Get vendor by ID
     * Alternative method for retrieving vendor information
     */
    public Optional<Vendor> getVendorById(Long vendorId) {
        log.debug("Getting vendor by ID: {}", vendorId);
        return vendorRepository.findById(vendorId)
            .filter(vendor -> !vendor.isDeleted() && vendor.getIsActive());
    }
    
    /**
     * Check if vendor exists and is active by domain
     */
    public boolean isVendorActiveByDomain(String domain) {
        return getVendorByDomain(domain)
            .map(vendor -> vendor.getIsActive() && !vendor.isDeleted())
            .orElse(false);
    }
    
    /**
     * Get vendor theme configuration by domain
     */
    public Optional<String> getVendorThemeConfig(String domain) {
        return getVendorByDomain(domain)
            .map(Vendor::getThemeConfig);
    }
    
    /**
     * Get vendor currency by domain
     */
    public Optional<String> getVendorCurrency(String domain) {
        return getVendorByDomain(domain)
            .map(Vendor::getCurrency);
    }
    
    /**
     * Get vendor preferred language by domain
     */
    public Optional<String> getVendorPreferredLanguage(String domain) {
        return getVendorByDomain(domain)
            .map(Vendor::getPreferredLanguage);
    }
}
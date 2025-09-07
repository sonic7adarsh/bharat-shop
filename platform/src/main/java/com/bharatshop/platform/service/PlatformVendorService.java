package com.bharatshop.platform.service;

import com.bharatshop.shared.entity.Vendor;
import com.bharatshop.shared.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
// import java.util.UUID; // Replaced with Long

/**
 * Service class for vendor operations.
 * Handles business logic for vendor management, domain validation, and template setup.
 */
@Service("platformVendorService")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformVendorService {
    
    private final VendorRepository vendorRepository;
    
    /**
     * Get vendor by ID for current tenant
     */
    @Transactional(readOnly = true)
    public Optional<Vendor> getVendorById(Long vendorId) {
        return vendorRepository.findByIdAndTenantId(vendorId, getCurrentTenantId());
    }
    
    /**
     * Get vendor by domain
     */
    @Transactional(readOnly = true)
    public Optional<Vendor> getVendorByDomain(String domain) {
        return vendorRepository.findByDomain(domain);
    }
    
    /**
     * Create or update vendor
     */
    public Vendor saveVendor(Vendor vendor) {
        log.info("Saving vendor: {}", vendor.getStoreName());
        return vendorRepository.save(vendor);
    }
    
    /**
     * Update vendor information
     */
    public Vendor updateVendor(Long vendorId, Vendor updatedVendor) {
        Optional<Vendor> existingVendor = getVendorById(vendorId);
        
        if (existingVendor.isEmpty()) {
            throw new RuntimeException("Vendor not found with ID: " + vendorId);
        }
        
        Vendor vendor = existingVendor.get();
        
        // Update fields
        if (updatedVendor.getName() != null) {
            vendor.setName(updatedVendor.getName());
        }
        if (updatedVendor.getStoreName() != null) {
            vendor.setStoreName(updatedVendor.getStoreName());
        }
        if (updatedVendor.getDescription() != null) {
            vendor.setDescription(updatedVendor.getDescription());
        }
        if (updatedVendor.getLogo() != null) {
            vendor.setLogo(updatedVendor.getLogo());
        }
        if (updatedVendor.getThemeConfig() != null) {
            vendor.setThemeConfig(updatedVendor.getThemeConfig());
        }
        if (updatedVendor.getCurrency() != null) {
            vendor.setCurrency(updatedVendor.getCurrency());
        }
        if (updatedVendor.getPreferredLanguage() != null) {
            vendor.setPreferredLanguage(updatedVendor.getPreferredLanguage());
        }
        if (updatedVendor.getSubscriptionId() != null) {
            vendor.setSubscriptionId(updatedVendor.getSubscriptionId());
        }
        
        log.info("Updated vendor: {}", vendor.getStoreName());
        return vendorRepository.save(vendor);
    }
    
    /**
     * Check if domain is available
     */
    @Transactional(readOnly = true)
    public boolean isDomainAvailable(String domain) {
        return vendorRepository.isDomainAvailable(domain);
    }
    
    /**
     * Setup vendor with template configuration
     */
    public Vendor setupVendorTemplate(Long vendorId, String templateConfig) {
        Optional<Vendor> existingVendor = getVendorById(vendorId);
        
        if (existingVendor.isEmpty()) {
            throw new RuntimeException("Vendor not found with ID: " + vendorId);
        }
        
        Vendor vendor = existingVendor.get();
        vendor.setThemeConfig(templateConfig);
        
        log.info("Setup template for vendor: {}", vendor.getStoreName());
        return vendorRepository.save(vendor);
    }
    
    /**
     * Get all vendors by status
     */
    @Transactional(readOnly = true)
    public List<Vendor> getVendorsByStatus(Vendor.VendorStatus status) {
        return vendorRepository.findByStatus(status.name());
    }
    
    /**
     * Activate vendor
     */
    public Vendor activateVendor(Long vendorId) {
        Optional<Vendor> existingVendor = getVendorById(vendorId);
        
        if (existingVendor.isEmpty()) {
            throw new RuntimeException("Vendor not found with ID: " + vendorId);
        }
        
        Vendor vendor = existingVendor.get();
        vendor.setStatus(Vendor.VendorStatus.ACTIVE);
        vendor.setIsActive(true);
        
        log.info("Activated vendor: {}", vendor.getStoreName());
        return vendorRepository.save(vendor);
    }
    
    /**
     * Deactivate vendor
     */
    public Vendor deactivateVendor(Long vendorId) {
        Optional<Vendor> existingVendor = getVendorById(vendorId);
        
        if (existingVendor.isEmpty()) {
            throw new RuntimeException("Vendor not found with ID: " + vendorId);
        }
        
        Vendor vendor = existingVendor.get();
        vendor.setStatus(Vendor.VendorStatus.INACTIVE);
        vendor.setIsActive(false);
        
        log.info("Deactivated vendor: {}", vendor.getStoreName());
        return vendorRepository.save(vendor);
    }
    
    /**
     * Get current tenant ID (placeholder - should be implemented based on your tenant context)
     */
    private Long getCurrentTenantId() {
        // This should be implemented based on your tenant context mechanism
        // For now, returning a placeholder ID
        return 1L;
    }
}
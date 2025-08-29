package com.bharatshop.shared.tenant;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing tenant context in multi-tenant applications.
 * Provides thread-local storage for current tenant information.
 */
@Slf4j
public class TenantContext {
    
    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    /**
     * Set the current tenant ID for the current thread
     * 
     * @param tenantId the tenant ID to set
     */
    public static void setCurrentTenant(String tenantId) {
        log.debug("Setting current tenant to: {}", tenantId);
        currentTenant.set(tenantId);
    }
    
    /**
     * Get the current tenant ID for the current thread
     * 
     * @return the current tenant ID, or null if not set
     */
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    /**
     * Clear the current tenant context for the current thread
     */
    public static void clear() {
        log.debug("Clearing tenant context");
        currentTenant.remove();
    }
    
    /**
     * Check if a tenant is currently set
     * 
     * @return true if a tenant is set, false otherwise
     */
    public static boolean hasTenant() {
        return currentTenant.get() != null;
    }
    
    /**
     * Get the current tenant ID, throwing an exception if not set
     * 
     * @return the current tenant ID
     * @throws IllegalStateException if no tenant is set
     */
    public static String requireCurrentTenant() {
        String tenantId = getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context is set for the current thread");
        }
        return tenantId;
    }
}
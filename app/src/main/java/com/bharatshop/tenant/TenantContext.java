package com.bharatshop.tenant;

/**
 * Thread-local context for storing current tenant information.
 * Provides tenant isolation across the application.
 */
public final class TenantContext {
    
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    
    private TenantContext() {
        // Utility class
    }
    
    public static void setCurrentTenant(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public static Long getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
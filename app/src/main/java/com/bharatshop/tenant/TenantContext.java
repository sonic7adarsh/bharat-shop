package com.bharatshop.tenant;

/**
 * Thread-local context for storing current tenant information.
 * Provides tenant isolation across the application.
 */
public final class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    
    private TenantContext() {
        // Utility class
    }
    
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
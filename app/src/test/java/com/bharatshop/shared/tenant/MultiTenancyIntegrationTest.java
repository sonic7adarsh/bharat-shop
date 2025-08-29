package com.bharatshop.shared.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for multi-tenancy functionality.
 * Tests core tenant context and resolver functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
class MultiTenancyIntegrationTest {

    @Test
    void testTenantContextCleanup() {
        UUID testTenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(testTenantId.toString());
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(testTenantId.toString());
        
        TenantContext.clear();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void testTenantContextHasTenant() {
        // Test when no tenant is set
        TenantContext.clear();
        assertThat(TenantContext.hasTenant()).isFalse();
        
        // Test when tenant is set
        TenantContext.setCurrentTenant("test-tenant");
        assertThat(TenantContext.hasTenant()).isTrue();
        
        // Clean up
        TenantContext.clear();
    }
}
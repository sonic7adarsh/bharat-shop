package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationTemplate entities.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    /**
     * Find active template by tenant, event type, channel, and locale
     */
    Optional<NotificationTemplate> findByTenantIdAndEventTypeAndChannelAndLocaleAndIsActiveTrue(
            String tenantId, String eventType, String channel, String locale);
    
    /**
     * Find active template with fallback to default locale if specific locale not found
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId " +
           "AND nt.eventType = :eventType AND nt.channel = :channel " +
           "AND nt.locale IN (:locale, 'en_US') AND nt.isActive = true " +
           "ORDER BY CASE WHEN nt.locale = :locale THEN 0 ELSE 1 END")
    List<NotificationTemplate> findByTenantIdAndEventTypeAndChannelWithLocaleFallback(
            @Param("tenantId") String tenantId, 
            @Param("eventType") String eventType, 
            @Param("channel") String channel, 
            @Param("locale") String locale);
    
    /**
     * Find all active templates for a tenant
     */
    List<NotificationTemplate> findByTenantIdAndIsActiveTrueOrderByEventTypeAscChannelAscLocaleAsc(
            String tenantId);
    
    /**
     * Find all templates for a specific event type and tenant
     */
    List<NotificationTemplate> findByTenantIdAndEventTypeAndIsActiveTrueOrderByChannelAscLocaleAsc(
            String tenantId, String eventType);
    
    /**
     * Find all templates for a specific channel and tenant
     */
    List<NotificationTemplate> findByTenantIdAndChannelAndIsActiveTrueOrderByEventTypeAscLocaleAsc(
            String tenantId, String channel);
    
    /**
     * Check if template exists for given parameters
     */
    boolean existsByTenantIdAndEventTypeAndChannelAndLocale(
            String tenantId, String eventType, String channel, String locale);
    
    /**
     * Find templates by event type across all tenants (for system-wide templates)
     */
    List<NotificationTemplate> findByEventTypeAndIsActiveTrueOrderByTenantIdAscChannelAscLocaleAsc(
            String eventType);
}
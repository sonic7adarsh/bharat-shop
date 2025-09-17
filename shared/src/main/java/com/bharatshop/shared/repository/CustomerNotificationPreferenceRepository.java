package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.CustomerNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CustomerNotificationPreference entity.
 */
@Repository
public interface CustomerNotificationPreferenceRepository extends JpaRepository<CustomerNotificationPreference, Long> {
    
    /**
     * Find preference by tenant, customer, event type, and channel.
     */
    Optional<CustomerNotificationPreference> findByTenantIdAndCustomerIdAndEventTypeAndChannel(
            String tenantId, String customerId, String eventType, String channel);
    
    /**
     * Find all preferences for a customer in a tenant.
     */
    List<CustomerNotificationPreference> findByTenantIdAndCustomerIdOrderByEventTypeAscChannelAsc(
            String tenantId, String customerId);
    
    /**
     * Find all enabled preferences for a customer and event type.
     */
    List<CustomerNotificationPreference> findByTenantIdAndCustomerIdAndEventTypeAndIsEnabledTrue(
            String tenantId, String customerId, String eventType);
    
    /**
     * Find all preferences for a specific channel.
     */
    List<CustomerNotificationPreference> findByTenantIdAndCustomerIdAndChannelAndIsEnabledTrue(
            String tenantId, String customerId, String channel);
    
    /**
     * Find preferences that need verification.
     */
    List<CustomerNotificationPreference> findByTenantIdAndIsVerifiedFalseAndContactInfoIsNotNull(
            String tenantId);
    
    /**
     * Find preference by verification token.
     */
    Optional<CustomerNotificationPreference> findByVerificationToken(String verificationToken);
    
    /**
     * Check if preference exists.
     */
    boolean existsByTenantIdAndCustomerIdAndEventTypeAndChannel(
            String tenantId, String customerId, String eventType, String channel);
    
    /**
     * Find all customers who have enabled notifications for a specific event type and channel.
     */
    @Query("SELECT DISTINCT p.customerId FROM CustomerNotificationPreference p " +
           "WHERE p.tenantId = :tenantId AND p.eventType = :eventType AND p.channel = :channel " +
           "AND p.isEnabled = true AND p.isVerified = true")
    List<String> findCustomersWithEnabledNotifications(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("channel") String channel);
    
    /**
     * Find preferences for bulk notification sending.
     */
    @Query("SELECT p FROM CustomerNotificationPreference p " +
           "WHERE p.tenantId = :tenantId AND p.eventType = :eventType " +
           "AND p.isEnabled = true AND p.isVerified = true " +
           "AND p.customerId IN :customerIds")
    List<CustomerNotificationPreference> findEnabledPreferencesForCustomers(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("customerIds") List<String> customerIds);
    
    /**
     * Find preferences with specific frequency.
     */
    List<CustomerNotificationPreference> findByTenantIdAndFrequencyAndIsEnabledTrue(
            String tenantId, CustomerNotificationPreference.NotificationFrequency frequency);
    
    /**
     * Find preferences by contact info (for deduplication).
     */
    List<CustomerNotificationPreference> findByTenantIdAndChannelAndContactInfoAndIsEnabledTrue(
            String tenantId, String channel, String contactInfo);
    
    /**
     * Count total preferences for a customer.
     */
    long countByTenantIdAndCustomerId(String tenantId, String customerId);
    
    /**
     * Count enabled preferences for a customer.
     */
    long countByTenantIdAndCustomerIdAndIsEnabledTrue(String tenantId, String customerId);
    
    /**
     * Find preferences that need contact info verification.
     */
    @Query("SELECT p FROM CustomerNotificationPreference p " +
           "WHERE p.tenantId = :tenantId AND p.isVerified = false " +
           "AND p.contactInfo IS NOT NULL AND p.verificationToken IS NOT NULL")
    List<CustomerNotificationPreference> findPendingVerifications(@Param("tenantId") String tenantId);
    
    /**
     * Find preferences by locale.
     */
    List<CustomerNotificationPreference> findByTenantIdAndLocaleAndIsEnabledTrue(
            String tenantId, String locale);
    
    /**
     * Find preferences with quiet hours configured.
     */
    @Query("SELECT p FROM CustomerNotificationPreference p " +
           "WHERE p.tenantId = :tenantId AND p.isEnabled = true " +
           "AND p.quietHoursStart IS NOT NULL AND p.quietHoursEnd IS NOT NULL")
    List<CustomerNotificationPreference> findPreferencesWithQuietHours(@Param("tenantId") String tenantId);
    
    /**
     * Delete preferences for a customer (GDPR compliance).
     */
    void deleteByTenantIdAndCustomerId(String tenantId, String customerId);
    
    /**
     * Find preferences created by a specific user.
     */
    List<CustomerNotificationPreference> findByTenantIdAndCreatedBy(String tenantId, String createdBy);
}
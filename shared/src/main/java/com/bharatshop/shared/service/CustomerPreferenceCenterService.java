package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.CustomerNotificationPreference;
import com.bharatshop.shared.repository.CustomerNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing customer notification preferences.
 * Provides preference center functionality for customers to control their notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerPreferenceCenterService {
    
    // Manual log field since @Slf4j isn't working
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerPreferenceCenterService.class);
    
    private final CustomerNotificationPreferenceRepository preferenceRepository;
    
    // Default event types and channels
    private static final List<String> DEFAULT_EVENT_TYPES = List.of(
            "OrderPlaced", "PaymentCaptured", "OrderShipped", 
            "OrderDelivered", "RefundProcessed", "AbandonedCart"
    );
    
    private static final List<String> DEFAULT_CHANNELS = List.of(
            "EMAIL", "SMS", "WHATSAPP"
    );
    
    /**
     * Get all preferences for a customer.
     */
    public List<CustomerNotificationPreference> getCustomerPreferences(String tenantId, String customerId) {
        log.debug("Getting preferences for customer: {} in tenant: {}", customerId, tenantId);
        return preferenceRepository.findByTenantIdAndCustomerIdOrderByEventTypeAscChannelAsc(tenantId, customerId);
    }
    
    /**
     * Get preference for specific event type and channel.
     */
    public Optional<CustomerNotificationPreference> getPreference(String tenantId, String customerId, 
                                                                String eventType, String channel) {
        return preferenceRepository.findByTenantIdAndCustomerIdAndEventTypeAndChannel(
                tenantId, customerId, eventType, channel);
    }
    
    /**
     * Update or create a preference.
     */
    @Transactional
    public CustomerNotificationPreference updatePreference(CustomerNotificationPreference preference) {
        log.info("Updating preference for customer: {}, event: {}, channel: {}", 
                preference.getCustomerId(), preference.getEventType(), preference.getChannel());
        
        Optional<CustomerNotificationPreference> existing = preferenceRepository
                .findByTenantIdAndCustomerIdAndEventTypeAndChannel(
                        preference.getTenantId(), preference.getCustomerId(), 
                        preference.getEventType(), preference.getChannel());
        
        if (existing.isPresent()) {
            CustomerNotificationPreference existingPref = existing.get();
            existingPref.setIsEnabled(preference.getIsEnabled());
            existingPref.setLocale(preference.getLocale());
            existingPref.setFrequency(preference.getFrequency());
            existingPref.setQuietHoursStart(preference.getQuietHoursStart());
            existingPref.setQuietHoursEnd(preference.getQuietHoursEnd());
            existingPref.setTimezone(preference.getTimezone());
            existingPref.setContactInfo(preference.getContactInfo());
            existingPref.setMetadata(preference.getMetadata());
            existingPref.setUpdatedBy(preference.getUpdatedBy());
            
            // Reset verification if contact info changed
            if (!Objects.equals(existingPref.getContactInfo(), preference.getContactInfo())) {
                existingPref.setIsVerified(false);
                existingPref.setVerificationToken(generateVerificationToken());
                existingPref.setVerifiedAt(null);
            }
            
            return preferenceRepository.save(existingPref);
        } else {
            // Create new preference
            preference.setVerificationToken(generateVerificationToken());
            return preferenceRepository.save(preference);
        }
    }
    
    /**
     * Bulk update preferences for a customer.
     */
    @Transactional
    public List<CustomerNotificationPreference> updateCustomerPreferences(
            String tenantId, String customerId, List<CustomerNotificationPreference> preferences) {
        
        log.info("Bulk updating {} preferences for customer: {}", preferences.size(), customerId);
        
        return preferences.stream()
                .peek(pref -> {
                    pref.setTenantId(tenantId);
                    pref.setCustomerId(customerId);
                })
                .map(this::updatePreference)
                .collect(Collectors.toList());
    }
    
    /**
     * Enable/disable all notifications for a customer.
     */
    @Transactional
    public void setAllNotificationsEnabled(String tenantId, String customerId, boolean enabled) {
        log.info("Setting all notifications {} for customer: {}", 
                enabled ? "enabled" : "disabled", customerId);
        
        List<CustomerNotificationPreference> preferences = 
                preferenceRepository.findByTenantIdAndCustomerIdOrderByEventTypeAscChannelAsc(tenantId, customerId);
        
        preferences.forEach(pref -> pref.setIsEnabled(enabled));
        preferenceRepository.saveAll(preferences);
    }
    
    /**
     * Enable/disable notifications for a specific channel.
     */
    @Transactional
    public void setChannelNotificationsEnabled(String tenantId, String customerId, 
                                             String channel, boolean enabled) {
        log.info("Setting {} notifications {} for customer: {}", 
                channel, enabled ? "enabled" : "disabled", customerId);
        
        List<CustomerNotificationPreference> preferences = 
                preferenceRepository.findByTenantIdAndCustomerIdAndChannelAndIsEnabledTrue(
                        tenantId, customerId, channel);
        
        preferences.forEach(pref -> pref.setIsEnabled(enabled));
        preferenceRepository.saveAll(preferences);
    }
    
    /**
     * Create default preferences for a new customer.
     */
    @Transactional
    public List<CustomerNotificationPreference> createDefaultPreferences(
            String tenantId, String customerId, String createdBy) {
        
        log.info("Creating default preferences for new customer: {}", customerId);
        
        List<CustomerNotificationPreference> defaultPreferences = new ArrayList<>();
        
        for (String eventType : DEFAULT_EVENT_TYPES) {
            for (String channel : DEFAULT_CHANNELS) {
                // Check if preference already exists
                if (!preferenceRepository.existsByTenantIdAndCustomerIdAndEventTypeAndChannel(
                        tenantId, customerId, eventType, channel)) {
                    
                    CustomerNotificationPreference preference = CustomerNotificationPreference.builder()
                            .tenantId(tenantId)
                            .customerId(customerId)
                            .eventType(eventType)
                            .channel(channel)
                            .isEnabled(getDefaultEnabledState(eventType, channel))
                            .frequency(getDefaultFrequency(eventType))
                            .locale("en_US")
                            .timezone("UTC")
                            .isVerified(false)
                            .verificationToken(generateVerificationToken())
                            .createdBy(createdBy)
                            .updatedBy(createdBy)
                            .build();
                    
                    defaultPreferences.add(preference);
                }
            }
        }
        
        return preferenceRepository.saveAll(defaultPreferences);
    }
    
    /**
     * Verify contact information using verification token.
     */
    @Transactional
    public boolean verifyContactInfo(String verificationToken) {
        Optional<CustomerNotificationPreference> preference = 
                preferenceRepository.findByVerificationToken(verificationToken);
        
        if (preference.isPresent()) {
            CustomerNotificationPreference pref = preference.get();
            pref.setIsVerified(true);
            pref.setVerifiedAt(Instant.now());
            pref.setVerificationToken(null); // Clear token after verification
            preferenceRepository.save(pref);
            
            log.info("Contact info verified for customer: {}, channel: {}", 
                    pref.getCustomerId(), pref.getChannel());
            return true;
        }
        
        log.warn("Invalid verification token: {}", verificationToken);
        return false;
    }
    
    /**
     * Get enabled preferences for a customer and event type.
     */
    public List<CustomerNotificationPreference> getEnabledPreferences(
            String tenantId, String customerId, String eventType) {
        return preferenceRepository.findByTenantIdAndCustomerIdAndEventTypeAndIsEnabledTrue(
                tenantId, customerId, eventType);
    }
    
    /**
     * Check if customer has enabled notifications for event and channel.
     */
    public boolean isNotificationEnabled(String tenantId, String customerId, 
                                       String eventType, String channel) {
        Optional<CustomerNotificationPreference> preference = 
                preferenceRepository.findByTenantIdAndCustomerIdAndEventTypeAndChannel(
                        tenantId, customerId, eventType, channel);
        
        return preference.map(pref -> 
                pref.getIsEnabled() && pref.getIsVerified() && pref.isNotificationAllowedNow()
        ).orElse(false);
    }
    
    /**
     * Get preference statistics for a customer.
     */
    public Map<String, Object> getPreferenceStatistics(String tenantId, String customerId) {
        long totalPreferences = preferenceRepository.countByTenantIdAndCustomerId(tenantId, customerId);
        long enabledPreferences = preferenceRepository.countByTenantIdAndCustomerIdAndIsEnabledTrue(
                tenantId, customerId);
        
        List<CustomerNotificationPreference> preferences = 
                preferenceRepository.findByTenantIdAndCustomerIdOrderByEventTypeAscChannelAsc(
                        tenantId, customerId);
        
        long verifiedPreferences = preferences.stream()
                .filter(CustomerNotificationPreference::getIsVerified)
                .count();
        
        Map<String, Long> channelCounts = preferences.stream()
                .filter(CustomerNotificationPreference::getIsEnabled)
                .collect(Collectors.groupingBy(
                        CustomerNotificationPreference::getChannel,
                        Collectors.counting()
                ));
        
        return Map.of(
                "totalPreferences", totalPreferences,
                "enabledPreferences", enabledPreferences,
                "verifiedPreferences", verifiedPreferences,
                "channelCounts", channelCounts,
                "verificationRate", totalPreferences > 0 ? 
                        (double) verifiedPreferences / totalPreferences : 0.0
        );
    }
    
    /**
     * Delete all preferences for a customer (GDPR compliance).
     */
    @Transactional
    public void deleteCustomerPreferences(String tenantId, String customerId) {
        log.info("Deleting all preferences for customer: {} (GDPR)", customerId);
        preferenceRepository.deleteByTenantIdAndCustomerId(tenantId, customerId);
    }
    
    // Helper methods
    
    private boolean getDefaultEnabledState(String eventType, String channel) {
        // Enable email for all events by default
        if ("EMAIL".equals(channel)) {
            return true;
        }
        
        // Enable SMS only for critical events
        if ("SMS".equals(channel)) {
            return List.of("OrderPlaced", "PaymentCaptured", "OrderDelivered").contains(eventType);
        }
        
        // Disable WhatsApp by default (requires opt-in)
        return false;
    }
    
    private CustomerNotificationPreference.NotificationFrequency getDefaultFrequency(String eventType) {
        // Immediate for transactional events
        if (List.of("OrderPlaced", "PaymentCaptured", "OrderShipped", "OrderDelivered", "RefundProcessed")
                .contains(eventType)) {
            return CustomerNotificationPreference.NotificationFrequency.IMMEDIATE;
        }
        
        // Daily for marketing events
        return CustomerNotificationPreference.NotificationFrequency.DAILY;
    }
    
    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
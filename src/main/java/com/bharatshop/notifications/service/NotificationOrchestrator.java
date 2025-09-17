package com.bharatshop.notifications.service;

import com.bharatshop.notifications.dto.NotificationRequest;
import com.bharatshop.notifications.dto.NotificationResponse;
import com.bharatshop.notifications.entity.CustomerNotificationPreference;
import com.bharatshop.notifications.entity.NotificationTemplate;
import com.bharatshop.notifications.enums.NotificationChannel;
import com.bharatshop.notifications.provider.NotificationProvider;
import com.bharatshop.notifications.service.CustomerPreferenceCenterService;
import com.bharatshop.notifications.service.NotificationTemplateService;
import com.bharatshop.notifications.service.TemplateRenderingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates the entire notification process from event to delivery.
 * Handles template resolution, preference checking, provider selection, and delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {
    
    private final NotificationTemplateService templateService;
    private final TemplateRenderingService renderingService;
    private final CustomerPreferenceCenterService preferenceService;
    private final NotificationProviderRegistry providerRegistry;
    
    /**
     * Process notification for a customer
     */
    @Transactional
    public List<NotificationResponse> processNotification(NotificationRequest request) {
        log.debug("Processing notification for customer: {} in tenant: {}, eventType: {}", 
                 request.getCustomerId(), request.getTenantId(), request.getEventType());
        
        List<NotificationResponse> responses = new ArrayList<>();
        
        try {
            // Get customer preferences for this event type
            List<CustomerNotificationPreference> preferences = 
                preferenceService.getPreferencesForCustomer(
                    request.getTenantId(), 
                    request.getCustomerId(), 
                    request.getEventType()
                );
            
            // Filter enabled preferences
            List<CustomerNotificationPreference> enabledPreferences = preferences.stream()
                .filter(CustomerNotificationPreference::isEnabled)
                .filter(pref -> pref.isNotificationAllowed(LocalDateTime.now()))
                .collect(Collectors.toList());
            
            if (enabledPreferences.isEmpty()) {
                log.debug("No enabled preferences found for customer: {} in tenant: {}, eventType: {}", 
                         request.getCustomerId(), request.getTenantId(), request.getEventType());
                return responses;
            }
            
            // Process each enabled channel
            for (CustomerNotificationPreference preference : enabledPreferences) {
                try {
                    NotificationResponse response = processChannelNotification(request, preference);
                    if (response != null) {
                        responses.add(response);
                    }
                } catch (Exception e) {
                    log.error("Failed to process notification for channel: {} for customer: {}", 
                             preference.getChannel(), request.getCustomerId(), e);
                    
                    // Create error response
                    NotificationResponse errorResponse = NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage("Failed to process notification: " + e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
                    responses.add(errorResponse);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing notification for customer: {}", request.getCustomerId(), e);
            
            NotificationResponse errorResponse = NotificationResponse.builder()
                .notificationId(request.getNotificationId())
                .status(NotificationResponse.NotificationStatus.FAILED)
                .errorMessage("Notification processing failed: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
            responses.add(errorResponse);
        }
        
        return responses;
    }
    
    /**
     * Process notification for a specific channel
     */
    private NotificationResponse processChannelNotification(NotificationRequest request, 
                                                           CustomerNotificationPreference preference) {
        
        NotificationChannel channel = preference.getChannel();
        log.debug("Processing {} notification for customer: {}", channel, request.getCustomerId());
        
        // Get template for this channel and locale
        Optional<NotificationTemplate> templateOpt = templateService.findTemplate(
            request.getTenantId(),
            request.getEventType(),
            channel,
            preference.getLocale()
        );
        
        if (templateOpt.isEmpty()) {
            log.warn("No template found for tenant: {}, eventType: {}, channel: {}, locale: {}", 
                    request.getTenantId(), request.getEventType(), channel, preference.getLocale());
            return createErrorResponse(request, "Template not found");
        }
        
        NotificationTemplate template = templateOpt.get();
        
        // Render template with variables
        try {
            String renderedSubject = renderingService.renderTemplate(
                template.getSubject(), request.getVariables());
            String renderedBody = renderingService.renderTemplate(
                template.getBody(), request.getVariables());
            String renderedHtmlBody = template.getHtmlBody() != null ? 
                renderingService.renderTemplate(template.getHtmlBody(), request.getVariables()) : null;
            
            // Create notification request for provider
            NotificationRequest providerRequest = createProviderRequest(
                request, preference, template, renderedSubject, renderedBody, renderedHtmlBody);
            
            // Get provider for channel
            Optional<NotificationProvider> providerOpt = providerRegistry.getProvider(channel);
            if (providerOpt.isEmpty()) {
                log.error("No provider found for channel: {}", channel);
                return createErrorResponse(request, "Provider not available for channel: " + channel);
            }
            
            NotificationProvider provider = providerOpt.get();
            
            // Check if provider can handle the request
            if (!provider.canHandle(providerRequest)) {
                log.warn("Provider {} cannot handle request for channel: {}", 
                        provider.getProviderName(), channel);
                return createErrorResponse(request, "Provider cannot handle request");
            }
            
            // Send notification
            CompletableFuture<NotificationResponse> futureResponse = provider.sendNotification(providerRequest);
            
            // For now, we'll wait for the response (could be made async)
            return futureResponse.get();
            
        } catch (Exception e) {
            log.error("Error rendering template or sending notification for channel: {}", channel, e);
            return createErrorResponse(request, "Template rendering or sending failed: " + e.getMessage());
        }
    }
    
    /**
     * Create provider-specific notification request
     */
    private NotificationRequest createProviderRequest(NotificationRequest originalRequest,
                                                     CustomerNotificationPreference preference,
                                                     NotificationTemplate template,
                                                     String renderedSubject,
                                                     String renderedBody,
                                                     String renderedHtmlBody) {
        
        // Get contact info from preference
        String recipientAddress = getRecipientAddress(preference);
        
        return NotificationRequest.builder()
            .notificationId(originalRequest.getNotificationId())
            .tenantId(originalRequest.getTenantId())
            .customerId(originalRequest.getCustomerId())
            .eventType(originalRequest.getEventType())
            .channel(preference.getChannel())
            .recipientAddress(recipientAddress)
            .recipientName(preference.getDisplayName())
            .subject(renderedSubject)
            .body(renderedBody)
            .htmlBody(renderedHtmlBody)
            .locale(preference.getLocale())
            .priority(originalRequest.getPriority())
            .scheduledAt(originalRequest.getScheduledAt())
            .variables(originalRequest.getVariables())
            .metadata(createMetadata(originalRequest, preference, template))
            .providerConfig(originalRequest.getProviderConfig())
            .build();
    }
    
    /**
     * Get recipient address from preference
     */
    private String getRecipientAddress(CustomerNotificationPreference preference) {
        Map<String, String> contactInfo = preference.getContactInfo();
        if (contactInfo == null) {
            return null;
        }
        
        return switch (preference.getChannel()) {
            case EMAIL -> contactInfo.get("email");
            case SMS -> contactInfo.get("phone");
            case WHATSAPP -> contactInfo.get("whatsapp");
            case PUSH -> contactInfo.get("deviceToken");
            default -> null;
        };
    }
    
    /**
     * Create metadata for provider request
     */
    private Map<String, String> createMetadata(NotificationRequest originalRequest,
                                              CustomerNotificationPreference preference,
                                              NotificationTemplate template) {
        Map<String, String> metadata = new HashMap<>();
        
        if (originalRequest.getMetadata() != null) {
            metadata.putAll(originalRequest.getMetadata());
        }
        
        metadata.put("templateId", template.getId());
        metadata.put("preferenceId", preference.getId());
        metadata.put("locale", preference.getLocale());
        metadata.put("channel", preference.getChannel().name());
        metadata.put("processedAt", LocalDateTime.now().toString());
        
        return metadata;
    }
    
    /**
     * Create error response
     */
    private NotificationResponse createErrorResponse(NotificationRequest request, String errorMessage) {
        return NotificationResponse.builder()
            .notificationId(request.getNotificationId())
            .status(NotificationResponse.NotificationStatus.FAILED)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Process bulk notifications
     */
    @Transactional
    public List<NotificationResponse> processBulkNotifications(List<NotificationRequest> requests) {
        log.info("Processing {} bulk notifications", requests.size());
        
        List<NotificationResponse> allResponses = new ArrayList<>();
        
        for (NotificationRequest request : requests) {
            try {
                List<NotificationResponse> responses = processNotification(request);
                allResponses.addAll(responses);
            } catch (Exception e) {
                log.error("Failed to process bulk notification: {}", request.getNotificationId(), e);
                
                NotificationResponse errorResponse = createErrorResponse(request, 
                    "Bulk processing failed: " + e.getMessage());
                allResponses.add(errorResponse);
            }
        }
        
        return allResponses;
    }
    
    /**
     * Process notification asynchronously
     */
    public CompletableFuture<List<NotificationResponse>> processNotificationAsync(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> processNotification(request));
    }
    
    /**
     * Check if notification can be sent to customer
     */
    public boolean canSendNotification(String tenantId, String customerId, String eventType, 
                                      NotificationChannel channel) {
        try {
            Optional<CustomerNotificationPreference> preferenceOpt = 
                preferenceService.getPreference(tenantId, customerId, eventType, channel);
            
            if (preferenceOpt.isEmpty()) {
                return false;
            }
            
            CustomerNotificationPreference preference = preferenceOpt.get();
            return preference.isEnabled() && preference.isNotificationAllowed(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error checking notification permission for customer: {}", customerId, e);
            return false;
        }
    }
    
    /**
     * Get available channels for customer and event type
     */
    public List<NotificationChannel> getAvailableChannels(String tenantId, String customerId, String eventType) {
        try {
            List<CustomerNotificationPreference> preferences = 
                preferenceService.getPreferencesForCustomer(tenantId, customerId, eventType);
            
            return preferences.stream()
                .filter(CustomerNotificationPreference::isEnabled)
                .filter(pref -> pref.isNotificationAllowed(LocalDateTime.now()))
                .map(CustomerNotificationPreference::getChannel)
                .distinct()
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting available channels for customer: {}", customerId, e);
            return Collections.emptyList();
        }
    }
}
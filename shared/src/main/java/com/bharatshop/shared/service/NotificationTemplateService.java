package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.NotificationTemplate;
import com.bharatshop.shared.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing notification templates with tenant and locale support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateService.class);
    private final NotificationTemplateRepository templateRepository;
    private static final String DEFAULT_LOCALE = "en_US";
    
    /**
     * Find template with locale fallback support
     */
    public Optional<NotificationTemplate> findTemplate(String tenantId, String eventType, 
                                                      String channel, String locale) {
        log.debug("Finding template for tenant: {}, event: {}, channel: {}, locale: {}", 
                 tenantId, eventType, channel, locale);
        
        // First try to find exact match
        Optional<NotificationTemplate> template = templateRepository
                .findByTenantIdAndEventTypeAndChannelAndLocaleAndIsActiveTrue(
                        tenantId, eventType, channel, locale);
        
        if (template.isPresent()) {
            log.debug("Found exact template match for locale: {}", locale);
            return template;
        }
        
        // Fallback to default locale if specific locale not found
        if (!DEFAULT_LOCALE.equals(locale)) {
            log.debug("Template not found for locale: {}, falling back to default locale: {}", 
                     locale, DEFAULT_LOCALE);
            
            template = templateRepository
                    .findByTenantIdAndEventTypeAndChannelAndLocaleAndIsActiveTrue(
                            tenantId, eventType, channel, DEFAULT_LOCALE);
            
            if (template.isPresent()) {
                log.debug("Found template with default locale fallback");
                return template;
            }
        }
        
        log.warn("No template found for tenant: {}, event: {}, channel: {}, locale: {} (with fallback)", 
                tenantId, eventType, channel, locale);
        return Optional.empty();
    }
    
    /**
     * Find template with advanced fallback logic
     */
    public Optional<NotificationTemplate> findTemplateWithFallback(String tenantId, String eventType, 
                                                                  String channel, String locale) {
        List<NotificationTemplate> templates = templateRepository
                .findByTenantIdAndEventTypeAndChannelWithLocaleFallback(
                        tenantId, eventType, channel, locale);
        
        return templates.isEmpty() ? Optional.empty() : Optional.of(templates.get(0));
    }
    
    /**
     * Create or update a notification template
     */
    @Transactional
    public NotificationTemplate saveTemplate(NotificationTemplate template) {
        log.info("Saving template for tenant: {}, event: {}, channel: {}, locale: {}", 
                template.getTenantId(), template.getEventType(), 
                template.getChannel(), template.getLocale());
        
        return templateRepository.save(template);
    }
    
    /**
     * Deactivate a template
     */
    @Transactional
    public void deactivateTemplate(Long templateId) {
        Optional<NotificationTemplate> template = templateRepository.findById(templateId);
        if (template.isPresent()) {
            NotificationTemplate t = template.get();
            t.setIsActive(false);
            templateRepository.save(t);
            log.info("Deactivated template ID: {}", templateId);
        } else {
            log.warn("Template not found for deactivation: {}", templateId);
        }
    }
    
    /**
     * Get all active templates for a tenant
     */
    public List<NotificationTemplate> getActiveTemplatesForTenant(String tenantId) {
        return templateRepository.findByTenantIdAndIsActiveTrueOrderByEventTypeAscChannelAscLocaleAsc(tenantId);
    }
    
    /**
     * Get templates for a specific event type and tenant
     */
    public List<NotificationTemplate> getTemplatesForEvent(String tenantId, String eventType) {
        return templateRepository.findByTenantIdAndEventTypeAndIsActiveTrueOrderByChannelAscLocaleAsc(
                tenantId, eventType);
    }
    
    /**
     * Get templates for a specific channel and tenant
     */
    public List<NotificationTemplate> getTemplatesForChannel(String tenantId, String channel) {
        return templateRepository.findByTenantIdAndChannelAndIsActiveTrueOrderByEventTypeAscLocaleAsc(
                tenantId, channel);
    }
    
    /**
     * Check if template exists
     */
    public boolean templateExists(String tenantId, String eventType, String channel, String locale) {
        return templateRepository.existsByTenantIdAndEventTypeAndChannelAndLocale(
                tenantId, eventType, channel, locale);
    }
    
    /**
     * Create default templates for a new tenant
     */
    @Transactional
    public void createDefaultTemplatesForTenant(String tenantId) {
        log.info("Creating default templates for tenant: {}", tenantId);
        
        // Create default email templates for each event type
        createDefaultEmailTemplates(tenantId);
        
        // Create default SMS templates for each event type
        createDefaultSmsTemplates(tenantId);
        
        log.info("Default templates created for tenant: {}", tenantId);
    }
    
    private void createDefaultEmailTemplates(String tenantId) {
        // Order Placed Email Template
        NotificationTemplate orderPlacedEmail = new NotificationTemplate();
        orderPlacedEmail.setTenantId(tenantId);
        orderPlacedEmail.setEventType("OrderPlaced");
        orderPlacedEmail.setChannel("EMAIL");
        orderPlacedEmail.setLocale(DEFAULT_LOCALE);
        orderPlacedEmail.setName("Order Confirmation Email");
        orderPlacedEmail.setDescription("Email sent when customer places an order");
        orderPlacedEmail.setSubject("Order Confirmation - Order #{{orderId}}");
        orderPlacedEmail.setBody("Dear {{customerName}},\n\nThank you for your order! Your order #{{orderId}} has been placed successfully.\n\nOrder Total: {{totalAmount}} {{currency}}\n\nWe'll send you another email when your order ships.\n\nBest regards,\nBharatShop Team");
        orderPlacedEmail.setHtmlBody("<h2>Order Confirmation</h2><p>Dear {{customerName}},</p><p>Thank you for your order! Your order #{{orderId}} has been placed successfully.</p><p><strong>Order Total:</strong> {{totalAmount}} {{currency}}</p><p>We'll send you another email when your order ships.</p><p>Best regards,<br>BharatShop Team</p>");
        
        templateRepository.save(orderPlacedEmail);
        
        // Add other default email templates...
        // (PaymentCaptured, OrderShipped, OrderDelivered, RefundProcessed, AbandonedCart)
    }
    
    private void createDefaultSmsTemplates(String tenantId) {
        // Order Placed SMS Template
        NotificationTemplate orderPlacedSms = new NotificationTemplate();
        orderPlacedSms.setTenantId(tenantId);
        orderPlacedSms.setEventType("OrderPlaced");
        orderPlacedSms.setChannel("SMS");
        orderPlacedSms.setLocale(DEFAULT_LOCALE);
        orderPlacedSms.setName("Order Confirmation SMS");
        orderPlacedSms.setDescription("SMS sent when customer places an order");
        orderPlacedSms.setSubject("Order Confirmation");
        orderPlacedSms.setBody("Hi {{customerName}}, your order #{{orderId}} for {{totalAmount}} {{currency}} has been confirmed. Track at: {{trackingUrl}}");
        
        templateRepository.save(orderPlacedSms);
        
        // Add other default SMS templates...
    }
}
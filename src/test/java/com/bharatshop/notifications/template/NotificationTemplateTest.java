package com.bharatshop.notifications.template;

import com.bharatshop.notifications.enums.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for notification template system
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationTemplateTest {
    
    @Autowired
    private NotificationTemplateService templateService;
    
    @Autowired
    private NotificationTemplateRepository templateRepository;
    
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String TEST_EVENT_TYPE = "ORDER_PLACED";
    
    @BeforeEach
    void setUp() {
        // Clean up existing templates
        templateRepository.deleteAll();
        
        // Create test templates
        createTestTemplates();
    }
    
    @Test
    @DisplayName("Should find template by tenant, event type, channel and locale")
    void testFindTemplate() {
        // When
        Optional<NotificationTemplate> template = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "en");
        
        // Then
        assertThat(template).isPresent();
        assertThat(template.get().getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(template.get().getEventType()).isEqualTo(TEST_EVENT_TYPE);
        assertThat(template.get().getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(template.get().getLocale()).isEqualTo("en");
    }
    
    @Test
    @DisplayName("Should fallback to default locale when specific locale not found")
    void testLocaleFallback() {
        // When: Request template for non-existent locale
        Optional<NotificationTemplate> template = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "fr");
        
        // Then: Should fallback to English
        assertThat(template).isPresent();
        assertThat(template.get().getLocale()).isEqualTo("en");
    }
    
    @Test
    @DisplayName("Should render template with variables")
    void testTemplateRendering() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");
        variables.put("orderId", "ORDER-123");
        variables.put("orderTotal", new BigDecimal("99.99"));
        variables.put("currency", "USD");
        variables.put("estimatedDelivery", "2024-01-15");
        
        Optional<NotificationTemplate> template = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "en");
        
        // When
        String renderedSubject = templateService.renderTemplate(template.get().getSubject(), variables);
        String renderedBody = templateService.renderTemplate(template.get().getBody(), variables);
        
        // Then
        assertThat(renderedSubject).isEqualTo("Order Confirmation - John Doe");
        assertThat(renderedBody).contains("Dear John Doe");
        assertThat(renderedBody).contains("ORDER-123");
        assertThat(renderedBody).contains("$99.99");
        assertThat(renderedBody).contains("2024-01-15");
    }
    
    @Test
    @DisplayName("Should handle missing variables gracefully")
    void testMissingVariables() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");
        // Missing orderId, orderTotal, etc.
        
        Optional<NotificationTemplate> template = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "en");
        
        // When
        String renderedSubject = templateService.renderTemplate(template.get().getSubject(), variables);
        String renderedBody = templateService.renderTemplate(template.get().getBody(), variables);
        
        // Then: Missing variables should remain as placeholders or be empty
        assertThat(renderedSubject).isEqualTo("Order Confirmation - John Doe");
        assertThat(renderedBody).contains("Dear John Doe");
        // Missing variables should be handled gracefully (empty or placeholder)
    }
    
    @Test
    @DisplayName("Should create new template")
    void testCreateTemplate() {
        // Given
        NotificationTemplate newTemplate = NotificationTemplate.builder()
            .tenantId(TEST_TENANT_ID)
            .eventType("PAYMENT_FAILED")
            .channel(NotificationChannel.EMAIL)
            .locale("en")
            .subject("Payment Failed - {{orderId}}")
            .body("Dear {{customerName}}, payment for order {{orderId}} failed.")
            .isActive(true)
            .build();
        
        // When
        NotificationTemplate savedTemplate = templateService.createTemplate(newTemplate);
        
        // Then
        assertThat(savedTemplate.getId()).isNotNull();
        assertThat(savedTemplate.getCreatedAt()).isNotNull();
        assertThat(savedTemplate.getUpdatedAt()).isNotNull();
        
        // Verify it can be found
        Optional<NotificationTemplate> foundTemplate = templateService.findTemplate(
            TEST_TENANT_ID, "PAYMENT_FAILED", NotificationChannel.EMAIL, "en");
        assertThat(foundTemplate).isPresent();
    }
    
    @Test
    @DisplayName("Should update existing template")
    void testUpdateTemplate() {
        // Given
        Optional<NotificationTemplate> existingTemplate = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "en");
        assertThat(existingTemplate).isPresent();
        
        NotificationTemplate template = existingTemplate.get();
        String originalSubject = template.getSubject();
        String newSubject = "Updated Order Confirmation - {{customerName}}";
        template.setSubject(newSubject);
        
        // When
        NotificationTemplate updatedTemplate = templateService.updateTemplate(template);
        
        // Then
        assertThat(updatedTemplate.getSubject()).isEqualTo(newSubject);
        assertThat(updatedTemplate.getSubject()).isNotEqualTo(originalSubject);
        assertThat(updatedTemplate.getUpdatedAt()).isAfter(updatedTemplate.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should deactivate template")
    void testDeactivateTemplate() {
        // Given
        Optional<NotificationTemplate> template = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "en");
        assertThat(template).isPresent();
        assertThat(template.get().isActive()).isTrue();
        
        // When
        templateService.deactivateTemplate(template.get().getId());
        
        // Then
        Optional<NotificationTemplate> deactivatedTemplate = templateRepository.findById(template.get().getId());
        assertThat(deactivatedTemplate).isPresent();
        assertThat(deactivatedTemplate.get().isActive()).isFalse();
        
        // Should not be found by active template search
        Optional<NotificationTemplate> activeTemplate = templateService.findTemplate(
            TEST_TENANT_ID, TEST_EVENT_TYPE, NotificationChannel.EMAIL, "en");
        assertThat(activeTemplate).isEmpty();
    }
    
    @Test
    @DisplayName("Should find templates by tenant and event type")
    void testFindTemplatesByTenantAndEventType() {
        // When
        List<NotificationTemplate> templates = templateService.findTemplatesByTenantAndEventType(
            TEST_TENANT_ID, TEST_EVENT_TYPE);
        
        // Then
        assertThat(templates).hasSize(3); // EMAIL, SMS, WHATSAPP
        assertThat(templates).allMatch(t -> t.getTenantId().equals(TEST_TENANT_ID));
        assertThat(templates).allMatch(t -> t.getEventType().equals(TEST_EVENT_TYPE));
        assertThat(templates).extracting(NotificationTemplate::getChannel)
            .containsExactlyInAnyOrder(
                NotificationChannel.EMAIL, 
                NotificationChannel.SMS, 
                NotificationChannel.WHATSAPP
            );
    }
    
    @Test
    @DisplayName("Should find all templates for tenant")
    void testFindAllTemplatesForTenant() {
        // When
        List<NotificationTemplate> templates = templateService.findAllTemplatesForTenant(TEST_TENANT_ID);
        
        // Then
        assertThat(templates).hasSizeGreaterThanOrEqualTo(3);
        assertThat(templates).allMatch(t -> t.getTenantId().equals(TEST_TENANT_ID));
        assertThat(templates).allMatch(NotificationTemplate::isActive);
    }
    
    @Test
    @DisplayName("Should validate template syntax")
    void testTemplateValidation() {
        // Given: Template with valid syntax
        String validTemplate = "Hello {{customerName}}, your order {{orderId}} is ready!";
        
        // When & Then
        assertDoesNotThrow(() -> templateService.validateTemplate(validTemplate));
        
        // Given: Template with invalid syntax
        String invalidTemplate = "Hello {{customerName}, your order {{orderId}} is ready!";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> templateService.validateTemplate(invalidTemplate));
    }
    
    @Test
    @DisplayName("Should handle complex template variables")
    void testComplexTemplateVariables() {
        // Given
        String complexTemplate = "Dear {{customer.name}}, your order {{order.id}} with {{items.size}} items totaling {{order.total}} {{order.currency}} will be delivered to {{shipping.address.street}}, {{shipping.address.city}}.";
        
        Map<String, Object> variables = new HashMap<>();
        
        // Nested objects
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John Doe");
        variables.put("customer", customer);
        
        Map<String, Object> order = new HashMap<>();
        order.put("id", "ORDER-123");
        order.put("total", new BigDecimal("149.99"));
        order.put("currency", "USD");
        variables.put("order", order);
        
        Map<String, Object> items = new HashMap<>();
        items.put("size", 3);
        variables.put("items", items);
        
        Map<String, Object> shipping = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "New York");
        shipping.put("address", address);
        variables.put("shipping", shipping);
        
        // When
        String rendered = templateService.renderTemplate(complexTemplate, variables);
        
        // Then
        assertThat(rendered).contains("John Doe");
        assertThat(rendered).contains("ORDER-123");
        assertThat(rendered).contains("3 items");
        assertThat(rendered).contains("149.99 USD");
        assertThat(rendered).contains("123 Main St, New York");
    }
    
    @Test
    @DisplayName("Should support conditional template rendering")
    void testConditionalTemplateRendering() {
        // Given
        String conditionalTemplate = "Dear {{customerName}}, {{#hasDiscount}}you saved {{discountAmount}}! {{/hasDiscount}}Your order {{orderId}} total is {{orderTotal}}.";
        
        Map<String, Object> variablesWithDiscount = new HashMap<>();
        variablesWithDiscount.put("customerName", "John Doe");
        variablesWithDiscount.put("orderId", "ORDER-123");
        variablesWithDiscount.put("orderTotal", "$89.99");
        variablesWithDiscount.put("hasDiscount", true);
        variablesWithDiscount.put("discountAmount", "$10.00");
        
        Map<String, Object> variablesWithoutDiscount = new HashMap<>();
        variablesWithoutDiscount.put("customerName", "Jane Smith");
        variablesWithoutDiscount.put("orderId", "ORDER-456");
        variablesWithoutDiscount.put("orderTotal", "$99.99");
        variablesWithoutDiscount.put("hasDiscount", false);
        
        // When
        String renderedWithDiscount = templateService.renderTemplate(conditionalTemplate, variablesWithDiscount);
        String renderedWithoutDiscount = templateService.renderTemplate(conditionalTemplate, variablesWithoutDiscount);
        
        // Then
        assertThat(renderedWithDiscount).contains("you saved $10.00!");
        assertThat(renderedWithoutDiscount).doesNotContain("you saved");
    }
    
    private void createTestTemplates() {
        List<NotificationTemplate> templates = List.of(
            // EMAIL template
            NotificationTemplate.builder()
                .tenantId(TEST_TENANT_ID)
                .eventType(TEST_EVENT_TYPE)
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Order Confirmation - {{customerName}}")
                .body("Dear {{customerName}}, your order {{orderId}} for ${{orderTotal}} has been confirmed. Estimated delivery: {{estimatedDelivery}}.")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // SMS template
            NotificationTemplate.builder()
                .tenantId(TEST_TENANT_ID)
                .eventType(TEST_EVENT_TYPE)
                .channel(NotificationChannel.SMS)
                .locale("en")
                .body("Hi {{customerName}}, order {{orderId}} confirmed! Total: ${{orderTotal}}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // WhatsApp template
            NotificationTemplate.builder()
                .tenantId(TEST_TENANT_ID)
                .eventType(TEST_EVENT_TYPE)
                .channel(NotificationChannel.WHATSAPP)
                .locale("en")
                .subject("order_confirmation")
                .body("Your order {{orderId}} has been confirmed, {{customerName}}! Total: {{orderTotal}} {{currency}}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        templateRepository.saveAll(templates);
    }
}
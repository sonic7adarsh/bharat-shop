package com.bharatshop.notifications.config;

import com.bharatshop.notifications.preference.CustomerNotificationPreference;
import com.bharatshop.notifications.preference.CustomerNotificationPreferenceRepository;
import com.bharatshop.notifications.providers.fake.FakeEmailProvider;
import com.bharatshop.notifications.providers.fake.FakeSmsProvider;
import com.bharatshop.notifications.providers.fake.FakeWhatsAppProvider;
import com.bharatshop.notifications.registry.NotificationProviderRegistry;
import com.bharatshop.notifications.template.NotificationTemplate;
import com.bharatshop.notifications.template.NotificationTemplateRepository;
import com.bharatshop.notifications.enums.NotificationChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Test configuration for notification system tests
 */
@TestConfiguration
@Profile("test")
public class NotificationTestConfiguration {
    
    private final NotificationTemplateRepository templateRepository;
    private final CustomerNotificationPreferenceRepository preferenceRepository;
    private final NotificationProviderRegistry providerRegistry;
    
    public NotificationTestConfiguration(
            NotificationTemplateRepository templateRepository,
            CustomerNotificationPreferenceRepository preferenceRepository,
            NotificationProviderRegistry providerRegistry) {
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.providerRegistry = providerRegistry;
    }
    
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules();
    }
    
    @Bean("notificationTaskExecutor")
    @Primary
    public Executor testTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-notification-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    @Primary
    public FakeEmailProvider testEmailProvider() {
        return new FakeEmailProvider();
    }
    
    @Bean
    @Primary
    public FakeSmsProvider testSmsProvider() {
        return new FakeSmsProvider();
    }
    
    @Bean
    @Primary
    public FakeWhatsAppProvider testWhatsAppProvider() {
        return new FakeWhatsAppProvider();
    }
    
    @PostConstruct
    public void setupTestData() {
        setupTestTemplates();
        setupTestPreferences();
        setupTestProviders();
    }
    
    private void setupTestTemplates() {
        // Clear existing templates
        templateRepository.deleteAll();
        
        // Create test templates
        List<NotificationTemplate> templates = List.of(
            // ORDER_PLACED templates
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Order Confirmation - {{customerName}}")
                .body("Dear {{customerName}}, your order {{orderId}} for ${{orderTotal}} has been confirmed. Estimated delivery: {{estimatedDelivery}}.")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.SMS)
                .locale("en")
                .body("Hi {{customerName}}, order {{orderId}} confirmed! Total: ${{orderTotal}}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.WHATSAPP)
                .locale("en")
                .subject("order_confirmation")
                .body("Your order {{orderId}} has been confirmed, {{customerName}}! Total: {{orderTotal}} {{currency}}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // ORDER_SHIPPED templates
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ORDER_SHIPPED")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Order Shipped - {{orderId}}")
                .body("Hi {{customerName}}, your order {{orderId}} has been shipped! Tracking: {{trackingNumber}}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ORDER_SHIPPED")
                .channel(NotificationChannel.SMS)
                .locale("en")
                .body("Order {{orderId}} shipped! Track: {{trackingNumber}}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // PAYMENT_CAPTURED templates
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("PAYMENT_CAPTURED")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Payment Received - {{orderId}}")
                .body("Dear {{customerName}}, we have received your payment of ${{paymentAmount}} for order {{orderId}}.")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // ORDER_DELIVERED templates
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ORDER_DELIVERED")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Order Delivered - {{orderId}}")
                .body("Hi {{customerName}}, your order {{orderId}} has been delivered! Please rate your experience.")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // ABANDONED_CART templates
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("ABANDONED_CART")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Complete Your Purchase - {{customerName}}")
                .body("Hi {{customerName}}, you left items in your cart worth ${{cartTotal}}. Use code {{discountCode}} for 10% off!")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // REFUND_PROCESSED templates
            NotificationTemplate.builder()
                .tenantId("test-tenant")
                .eventType("REFUND_PROCESSED")
                .channel(NotificationChannel.EMAIL)
                .locale("en")
                .subject("Refund Processed - {{orderId}}")
                .body("Dear {{customerName}}, your refund of ${{refundAmount}} for order {{orderId}} has been processed.")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        templateRepository.saveAll(templates);
    }
    
    private void setupTestPreferences() {
        // Clear existing preferences
        preferenceRepository.deleteAll();
        
        // Create test customer preferences
        List<CustomerNotificationPreference> preferences = List.of(
            // Test customer with all channels enabled
            CustomerNotificationPreference.builder()
                .tenantId("test-tenant")
                .customerId("test-customer-123")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.EMAIL)
                .enabled(true)
                .locale("en")
                .contactInfo("test@example.com")
                .verified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            CustomerNotificationPreference.builder()
                .tenantId("test-tenant")
                .customerId("test-customer-123")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.SMS)
                .enabled(true)
                .locale("en")
                .contactInfo("+1234567890")
                .verified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            CustomerNotificationPreference.builder()
                .tenantId("test-tenant")
                .customerId("test-customer-123")
                .eventType("ORDER_SHIPPED")
                .channel(NotificationChannel.EMAIL)
                .enabled(true)
                .locale("en")
                .contactInfo("test@example.com")
                .verified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            CustomerNotificationPreference.builder()
                .tenantId("test-tenant")
                .customerId("test-customer-123")
                .eventType("ORDER_SHIPPED")
                .channel(NotificationChannel.SMS)
                .enabled(true)
                .locale("en")
                .contactInfo("+1234567890")
                .verified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // Test customer with only email enabled
            CustomerNotificationPreference.builder()
                .tenantId("test-tenant")
                .customerId("email-only-customer")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.EMAIL)
                .enabled(true)
                .locale("en")
                .contactInfo("emailonly@example.com")
                .verified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
                
            // Test customer with disabled notifications
            CustomerNotificationPreference.builder()
                .tenantId("test-tenant")
                .customerId("disabled-customer")
                .eventType("ORDER_PLACED")
                .channel(NotificationChannel.EMAIL)
                .enabled(false)
                .locale("en")
                .contactInfo("disabled@example.com")
                .verified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        preferenceRepository.saveAll(preferences);
    }
    
    private void setupTestProviders() {
        // Register fake providers for testing
        FakeEmailProvider emailProvider = testEmailProvider();
        FakeSmsProvider smsProvider = testSmsProvider();
        FakeWhatsAppProvider whatsAppProvider = testWhatsAppProvider();
        
        providerRegistry.registerProvider("fake-email", emailProvider);
        providerRegistry.registerProvider("fake-sms", smsProvider);
        providerRegistry.registerProvider("fake-whatsapp", whatsAppProvider);
        
        // Clear any existing sent messages
        emailProvider.clearSentEmails();
        smsProvider.clearSentSms();
        whatsAppProvider.clearSentMessages();
    }
}
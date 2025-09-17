package com.bharatshop.notifications;

import com.bharatshop.notifications.dto.NotificationRequest;
import com.bharatshop.notifications.dto.NotificationResponse;
import com.bharatshop.notifications.enums.NotificationChannel;
import com.bharatshop.notifications.enums.NotificationPriority;
import com.bharatshop.notifications.event.NotificationEventPublisher;
import com.bharatshop.notifications.outbox.OutboxEvent;
import com.bharatshop.notifications.outbox.OutboxEventRepository;
import com.bharatshop.notifications.outbox.OutboxEventService;
import com.bharatshop.notifications.providers.fake.FakeEmailProvider;
import com.bharatshop.notifications.providers.fake.FakeSmsProvider;
import com.bharatshop.notifications.providers.fake.FakeWhatsAppProvider;
import com.bharatshop.notifications.registry.NotificationProviderRegistry;
import com.bharatshop.notifications.service.NotificationOrchestrator;
import com.bharatshop.notifications.template.NotificationTemplate;
import com.bharatshop.notifications.template.NotificationTemplateRepository;
import com.bharatshop.shared.events.OrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete notification system
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationSystemIntegrationTest {
    
    @Autowired
    private NotificationOrchestrator notificationOrchestrator;
    
    @Autowired
    private NotificationProviderRegistry providerRegistry;
    
    @Autowired
    private NotificationEventPublisher eventPublisher;
    
    @Autowired
    private OutboxEventService outboxEventService;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private NotificationTemplateRepository templateRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private FakeEmailProvider fakeEmailProvider;
    private FakeSmsProvider fakeSmsProvider;
    private FakeWhatsAppProvider fakeWhatsAppProvider;
    
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String TEST_CUSTOMER_ID = "test-customer-123";
    
    @BeforeEach
    void setUp() {
        // Initialize fake providers
        fakeEmailProvider = new FakeEmailProvider();
        fakeSmsProvider = new FakeSmsProvider();
        fakeWhatsAppProvider = new FakeWhatsAppProvider();
        
        // Register providers
        providerRegistry.registerProvider("fake-email", fakeEmailProvider);
        providerRegistry.registerProvider("fake-sms", fakeSmsProvider);
        providerRegistry.registerProvider("fake-whatsapp", fakeWhatsAppProvider);
        
        // Clear any existing data
        fakeEmailProvider.clearSentEmails();
        fakeSmsProvider.clearSentSms();
        fakeWhatsAppProvider.clearSentMessages();
        
        // Create test templates
        createTestTemplates();
    }
    
    @Test
    void testCompleteNotificationFlow() {
        // Given: A notification request
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");
        variables.put("orderId", "ORDER-123");
        variables.put("orderTotal", new BigDecimal("99.99"));
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ORDER_PLACED")
            .variables(variables)
            .priority(NotificationPriority.HIGH)
            .build();
        
        // When: Processing the notification
        List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
        
        // Then: Verify responses
        assertThat(responses).isNotEmpty();
        assertThat(responses).allMatch(NotificationResponse::isSuccess);
        
        // Verify emails were sent
        assertThat(fakeEmailProvider.getSentEmails()).hasSize(1);
        var sentEmail = fakeEmailProvider.getSentEmails().get(0);
        assertThat(sentEmail.getTo()).contains("test@example.com");
        assertThat(sentEmail.getSubject()).contains("Order Confirmation");
        assertThat(sentEmail.getBody()).contains("John Doe");
        assertThat(sentEmail.getBody()).contains("ORDER-123");
    }
    
    @Test
    void testOutboxEventProcessing() {
        // Given: An order placed event
        OrderPlacedEvent event = OrderPlacedEvent.builder()
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .orderId("ORDER-456")
            .customerName("Jane Smith")
            .orderTotal(new BigDecimal("149.99"))
            .currency("USD")
            .orderDate(LocalDateTime.now())
            .estimatedDeliveryDate(LocalDateTime.now().plusDays(3))
            .items(List.of("Product A", "Product B"))
            .shippingAddress("123 Main St, City, State")
            .build();
        
        // When: Publishing the event
        eventPublisher.publishOrderPlacedEvent(event);
        
        // Then: Verify outbox event was created
        List<OutboxEvent> outboxEvents = outboxEventRepository.findReadyForProcessing(10);
        assertThat(outboxEvents).hasSize(1);
        
        OutboxEvent outboxEvent = outboxEvents.get(0);
        assertThat(outboxEvent.getEventType()).isEqualTo("ORDER_PLACED");
        assertThat(outboxEvent.getTenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(outboxEvent.getAggregateId()).isEqualTo("ORDER-456");
    }
    
    @Test
    void testMultiChannelNotification() {
        // Given: A notification request for multiple channels
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Alice Johnson");
        variables.put("orderId", "ORDER-789");
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ORDER_SHIPPED")
            .variables(variables)
            .channels(Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS))
            .priority(NotificationPriority.MEDIUM)
            .build();
        
        // When: Processing the notification
        List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
        
        // Then: Verify multiple channels were used
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(NotificationResponse::isSuccess);
        
        // Verify email was sent
        assertThat(fakeEmailProvider.getSentEmails()).hasSize(1);
        
        // Verify SMS was sent
        assertThat(fakeSmsProvider.getSentSms()).hasSize(1);
        var sentSms = fakeSmsProvider.getSentSms().get(0);
        assertThat(sentSms.getTo()).isEqualTo("+1234567890");
        assertThat(sentSms.getMessage()).contains("Alice Johnson");
    }
    
    @Test
    void testProviderFailureHandling() {
        // Given: Configure email provider to fail
        fakeEmailProvider.setSimulateFailure(true);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Bob Wilson");
        variables.put("orderId", "ORDER-999");
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ORDER_PLACED")
            .variables(variables)
            .channels(Set.of(NotificationChannel.EMAIL))
            .priority(NotificationPriority.HIGH)
            .build();
        
        // When: Processing the notification
        List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
        
        // Then: Verify failure was handled
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isSuccess()).isFalse();
        assertThat(responses.get(0).getErrorMessage()).contains("Simulated failure");
        
        // Reset failure simulation
        fakeEmailProvider.setSimulateFailure(false);
    }
    
    @Test
    void testBulkNotifications() {
        // Given: Multiple notification requests
        List<NotificationRequest> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", "Customer " + i);
            variables.put("orderId", "ORDER-" + i);
            
            NotificationRequest request = NotificationRequest.builder()
                .notificationId(UUID.randomUUID().toString())
                .tenantId(TEST_TENANT_ID)
                .customerId(TEST_CUSTOMER_ID + "-" + i)
                .eventType("ORDER_PLACED")
                .variables(variables)
                .priority(NotificationPriority.MEDIUM)
                .build();
            
            requests.add(request);
        }
        
        // When: Processing bulk notifications
        List<NotificationResponse> responses = notificationOrchestrator.processBulkNotifications(requests);
        
        // Then: Verify all were processed
        assertThat(responses).hasSize(5);
        assertThat(responses).allMatch(NotificationResponse::isSuccess);
        assertThat(fakeEmailProvider.getSentEmails()).hasSize(5);
    }
    
    @Test
    void testTemplateRendering() {
        // Given: A notification with template variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Test Customer");
        variables.put("orderId", "ORDER-TEMPLATE-TEST");
        variables.put("orderTotal", new BigDecimal("199.99"));
        variables.put("currency", "USD");
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ORDER_PLACED")
            .variables(variables)
            .channels(Set.of(NotificationChannel.EMAIL))
            .priority(NotificationPriority.HIGH)
            .build();
        
        // When: Processing the notification
        List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
        
        // Then: Verify template was rendered correctly
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isSuccess()).isTrue();
        
        var sentEmail = fakeEmailProvider.getSentEmails().get(0);
        assertThat(sentEmail.getSubject()).contains("Test Customer");
        assertThat(sentEmail.getBody()).contains("ORDER-TEMPLATE-TEST");
        assertThat(sentEmail.getBody()).contains("$199.99");
    }
    
    @Test
    void testProviderAvailability() {
        // When: Checking provider availability
        boolean emailAvailable = providerRegistry.isProviderAvailable("fake-email");
        boolean smsAvailable = providerRegistry.isProviderAvailable("fake-sms");
        boolean whatsappAvailable = providerRegistry.isProviderAvailable("fake-whatsapp");
        
        // Then: All providers should be available
        assertThat(emailAvailable).isTrue();
        assertThat(smsAvailable).isTrue();
        assertThat(whatsappAvailable).isTrue();
    }
    
    @Test
    void testScheduledNotifications() {
        // Given: A scheduled notification
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Scheduled Customer");
        variables.put("cartId", "CART-123");
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ABANDONED_CART")
            .variables(variables)
            .scheduledAt(LocalDateTime.now().plusHours(1))
            .priority(NotificationPriority.LOW)
            .build();
        
        // When: Processing the scheduled notification
        List<NotificationResponse> responses = notificationOrchestrator.processNotification(request);
        
        // Then: Should be scheduled, not sent immediately
        assertThat(responses).isEmpty(); // No immediate responses for scheduled notifications
        
        // Verify outbox event was created for future processing
        List<OutboxEvent> futureEvents = outboxEventRepository.findAll();
        assertThat(futureEvents).isNotEmpty();
    }
    
    private void createTestTemplates() {
        // Email template for ORDER_PLACED
        NotificationTemplate emailTemplate = NotificationTemplate.builder()
            .tenantId(TEST_TENANT_ID)
            .eventType("ORDER_PLACED")
            .channel(NotificationChannel.EMAIL)
            .locale("en")
            .subject("Order Confirmation - {{customerName}}")
            .body("Dear {{customerName}}, your order {{orderId}} for ${{orderTotal}} has been confirmed.")
            .isActive(true)
            .build();
        
        // SMS template for ORDER_PLACED
        NotificationTemplate smsTemplate = NotificationTemplate.builder()
            .tenantId(TEST_TENANT_ID)
            .eventType("ORDER_PLACED")
            .channel(NotificationChannel.SMS)
            .locale("en")
            .body("Hi {{customerName}}, order {{orderId}} confirmed!")
            .isActive(true)
            .build();
        
        // Email template for ORDER_SHIPPED
        NotificationTemplate shippedTemplate = NotificationTemplate.builder()
            .tenantId(TEST_TENANT_ID)
            .eventType("ORDER_SHIPPED")
            .channel(NotificationChannel.EMAIL)
            .locale("en")
            .subject("Order Shipped - {{orderId}}")
            .body("Hi {{customerName}}, your order {{orderId}} has been shipped!")
            .isActive(true)
            .build();
        
        templateRepository.saveAll(List.of(emailTemplate, smsTemplate, shippedTemplate));
    }
}
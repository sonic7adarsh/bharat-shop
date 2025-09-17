package com.bharatshop.notifications.providers;

import com.bharatshop.notifications.dto.NotificationRequest;
import com.bharatshop.notifications.dto.NotificationResponse;
import com.bharatshop.notifications.enums.NotificationChannel;
import com.bharatshop.notifications.enums.NotificationPriority;
import com.bharatshop.notifications.providers.fake.FakeEmailProvider;
import com.bharatshop.notifications.providers.fake.FakeSmsProvider;
import com.bharatshop.notifications.providers.fake.FakeWhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for notification providers
 */
class NotificationProviderTest {
    
    private FakeEmailProvider emailProvider;
    private FakeSmsProvider smsProvider;
    private FakeWhatsAppProvider whatsAppProvider;
    
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String TEST_CUSTOMER_ID = "customer-123";
    
    @BeforeEach
    void setUp() {
        emailProvider = new FakeEmailProvider();
        smsProvider = new FakeSmsProvider();
        whatsAppProvider = new FakeWhatsAppProvider();
        
        // Clear any existing data
        emailProvider.clearSentEmails();
        smsProvider.clearSentSms();
        whatsAppProvider.clearSentMessages();
    }
    
    @Test
    @DisplayName("Email provider should send notification successfully")
    void testEmailProviderSuccess() {
        // Given
        NotificationRequest request = createTestRequest(NotificationChannel.EMAIL);
        
        // When
        NotificationResponse response = emailProvider.sendNotification(request);
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNotificationId()).isEqualTo(request.getNotificationId());
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.getProviderId()).isEqualTo("fake-email");
        
        // Verify email was stored
        assertThat(emailProvider.getSentEmails()).hasSize(1);
        var sentEmail = emailProvider.getSentEmails().get(0);
        assertThat(sentEmail.getTo()).contains("test@example.com");
        assertThat(sentEmail.getSubject()).isEqualTo("Test Subject");
        assertThat(sentEmail.getBody()).isEqualTo("Test Body");
    }
    
    @Test
    @DisplayName("Email provider should handle failure simulation")
    void testEmailProviderFailure() {
        // Given
        emailProvider.setSimulateFailure(true);
        NotificationRequest request = createTestRequest(NotificationChannel.EMAIL);
        
        // When
        NotificationResponse response = emailProvider.sendNotification(request);
        
        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("Simulated failure");
        assertThat(emailProvider.getSentEmails()).isEmpty();
    }
    
    @Test
    @DisplayName("Email provider should handle delay simulation")
    void testEmailProviderDelay() {
        // Given
        emailProvider.setSimulateDelay(true);
        emailProvider.setDelayMs(100);
        NotificationRequest request = createTestRequest(NotificationChannel.EMAIL);
        
        // When
        long startTime = System.currentTimeMillis();
        NotificationResponse response = emailProvider.sendNotification(request);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(100);
    }
    
    @Test
    @DisplayName("SMS provider should send notification successfully")
    void testSmsProviderSuccess() {
        // Given
        NotificationRequest request = createTestRequest(NotificationChannel.SMS);
        
        // When
        NotificationResponse response = smsProvider.sendNotification(request);
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(response.getProviderId()).isEqualTo("fake-sms");
        
        // Verify SMS was stored
        assertThat(smsProvider.getSentSms()).hasSize(1);
        var sentSms = smsProvider.getSentSms().get(0);
        assertThat(sentSms.getTo()).isEqualTo("+1234567890");
        assertThat(sentSms.getMessage()).isEqualTo("Test Body");
    }
    
    @Test
    @DisplayName("SMS provider should track delivery status")
    void testSmsDeliveryStatus() {
        // Given
        NotificationRequest request = createTestRequest(NotificationChannel.SMS);
        
        // When
        NotificationResponse response = smsProvider.sendNotification(request);
        String messageId = response.getExternalId();
        
        // Then
        assertThat(smsProvider.getDeliveryStatus(messageId)).isEqualTo("SENT");
        
        // Simulate delivery
        smsProvider.simulateDelivery(messageId);
        assertThat(smsProvider.getDeliveryStatus(messageId)).isEqualTo("DELIVERED");
    }
    
    @Test
    @DisplayName("WhatsApp provider should send template message")
    void testWhatsAppTemplateMessage() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");
        variables.put("orderId", "ORDER-123");
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ORDER_PLACED")
            .channel(NotificationChannel.WHATSAPP)
            .subject("order_confirmation")
            .body("Your order {{orderId}} has been confirmed, {{customerName}}!")
            .variables(variables)
            .priority(NotificationPriority.HIGH)
            .build();
        
        // When
        NotificationResponse response = whatsAppProvider.sendNotification(request);
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.WHATSAPP);
        
        // Verify message was stored
        assertThat(whatsAppProvider.getSentMessages()).hasSize(1);
        var sentMessage = whatsAppProvider.getSentMessages().get(0);
        assertThat(sentMessage.getTo()).isEqualTo("+1234567890");
        assertThat(sentMessage.getTemplateName()).isEqualTo("order_confirmation");
    }
    
    @Test
    @DisplayName("WhatsApp provider should get available templates")
    void testWhatsAppAvailableTemplates() {
        // When
        List<String> templates = whatsAppProvider.getAvailableTemplates();
        
        // Then
        assertThat(templates).contains(
            "order_confirmation",
            "payment_received",
            "order_shipped",
            "order_delivered",
            "abandoned_cart"
        );
    }
    
    @Test
    @DisplayName("WhatsApp provider should track message status")
    void testWhatsAppMessageStatus() {
        // Given
        NotificationRequest request = createTestRequest(NotificationChannel.WHATSAPP);
        
        // When
        NotificationResponse response = whatsAppProvider.sendNotification(request);
        String messageId = response.getExternalId();
        
        // Then
        assertThat(whatsAppProvider.getMessageStatus(messageId)).isEqualTo("SENT");
        
        // Simulate read
        whatsAppProvider.simulateMessageRead(messageId);
        assertThat(whatsAppProvider.getMessageStatus(messageId)).isEqualTo("READ");
    }
    
    @Test
    @DisplayName("All providers should support availability check")
    void testProviderAvailability() {
        // When & Then
        assertThat(emailProvider.isAvailable()).isTrue();
        assertThat(smsProvider.isAvailable()).isTrue();
        assertThat(whatsAppProvider.isAvailable()).isTrue();
    }
    
    @Test
    @DisplayName("All providers should return correct supported channels")
    void testSupportedChannels() {
        // When & Then
        assertThat(emailProvider.getSupportedChannels()).containsExactly(NotificationChannel.EMAIL);
        assertThat(smsProvider.getSupportedChannels()).containsExactly(NotificationChannel.SMS);
        assertThat(whatsAppProvider.getSupportedChannels()).containsExactly(NotificationChannel.WHATSAPP);
    }
    
    @Test
    @DisplayName("Providers should handle bulk notifications")
    void testBulkNotifications() {
        // Given
        List<NotificationRequest> requests = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            requests.add(createTestRequest(NotificationChannel.EMAIL, "customer-" + i));
        }
        
        // When
        List<NotificationResponse> responses = emailProvider.sendBulkNotifications(requests);
        
        // Then
        assertThat(responses).hasSize(3);
        assertThat(responses).allMatch(NotificationResponse::isSuccess);
        assertThat(emailProvider.getSentEmails()).hasSize(3);
    }
    
    @Test
    @DisplayName("Providers should handle configuration")
    void testProviderConfiguration() {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("maxRetries", 5);
        config.put("timeout", 30000);
        
        // When
        emailProvider.configure(config);
        
        // Then
        Map<String, Object> schema = emailProvider.getConfigurationSchema();
        assertThat(schema).containsKeys("host", "port", "username", "password");
    }
    
    @Test
    @DisplayName("Providers should track statistics")
    void testProviderStatistics() {
        // Given
        NotificationRequest request1 = createTestRequest(NotificationChannel.EMAIL);
        NotificationRequest request2 = createTestRequest(NotificationChannel.EMAIL);
        
        // Configure one to fail
        emailProvider.setSimulateFailure(true);
        emailProvider.sendNotification(request1);
        
        emailProvider.setSimulateFailure(false);
        emailProvider.sendNotification(request2);
        
        // When
        Map<String, Object> stats = emailProvider.getStatistics();
        
        // Then
        assertThat(stats.get("totalSent")).isEqualTo(2L);
        assertThat(stats.get("successCount")).isEqualTo(1L);
        assertThat(stats.get("failureCount")).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("Email provider should handle attachments")
    void testEmailWithAttachments() {
        // Given
        Map<String, String> attachments = new HashMap<>();
        attachments.put("invoice.pdf", "base64-encoded-content");
        attachments.put("receipt.jpg", "base64-encoded-image");
        
        NotificationRequest request = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(TEST_CUSTOMER_ID)
            .eventType("ORDER_PLACED")
            .channel(NotificationChannel.EMAIL)
            .subject("Test Subject")
            .body("Test Body")
            .attachments(attachments)
            .priority(NotificationPriority.HIGH)
            .build();
        
        // When
        NotificationResponse response = emailProvider.sendNotification(request);
        
        // Then
        assertThat(response.isSuccess()).isTrue();
        
        var sentEmail = emailProvider.getSentEmails().get(0);
        assertThat(sentEmail.getAttachments()).hasSize(2);
        assertThat(sentEmail.getAttachments()).containsKeys("invoice.pdf", "receipt.jpg");
    }
    
    @Test
    @DisplayName("Providers should handle invalid requests gracefully")
    void testInvalidRequests() {
        // Given: Request with missing required fields
        NotificationRequest invalidRequest = NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .channel(NotificationChannel.EMAIL)
            // Missing customerId, subject, body
            .priority(NotificationPriority.HIGH)
            .build();
        
        // When
        NotificationResponse response = emailProvider.sendNotification(invalidRequest);
        
        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("Missing required field");
    }
    
    private NotificationRequest createTestRequest(NotificationChannel channel) {
        return createTestRequest(channel, TEST_CUSTOMER_ID);
    }
    
    private NotificationRequest createTestRequest(NotificationChannel channel, String customerId) {
        return NotificationRequest.builder()
            .notificationId(UUID.randomUUID().toString())
            .tenantId(TEST_TENANT_ID)
            .customerId(customerId)
            .eventType("TEST_EVENT")
            .channel(channel)
            .subject("Test Subject")
            .body("Test Body")
            .priority(NotificationPriority.HIGH)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
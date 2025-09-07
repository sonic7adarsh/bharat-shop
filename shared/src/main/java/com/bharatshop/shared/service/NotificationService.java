package com.bharatshop.shared.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling multi-channel notifications (Email, SMS, WhatsApp)
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private MessageService messageService;
    
    /**
     * Notification types
     */
    public enum NotificationType {
        ORDER_CONFIRMATION,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        ACCOUNT_CREATED,
        PASSWORD_RESET,
        STOCK_ALERT,
        PROMOTIONAL
    }
    
    /**
     * Notification channels
     */
    public enum NotificationChannel {
        EMAIL,
        SMS,
        WHATSAPP,
        PUSH
    }
    
    /**
     * Notification request
     */
    public static class NotificationRequest {
        private String recipientId;
        private String recipientEmail;
        private String recipientPhone;
        private NotificationType type;
        private Set<NotificationChannel> channels;
        private Map<String, Object> templateData;
        private String subject;
        private String message;
        private Locale locale;
        private Long tenantId;
        private boolean urgent;
        
        public NotificationRequest() {
            this.channels = new HashSet<>();
            this.templateData = new HashMap<>();
            this.locale = Locale.ENGLISH;
            this.urgent = false;
        }
        
        // Getters and Setters
        public String getRecipientId() { return recipientId; }
        public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
        
        public String getRecipientEmail() { return recipientEmail; }
        public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
        
        public String getRecipientPhone() { return recipientPhone; }
        public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
        
        public NotificationType getType() { return type; }
        public void setType(NotificationType type) { this.type = type; }
        
        public Set<NotificationChannel> getChannels() { return channels; }
        public void setChannels(Set<NotificationChannel> channels) { this.channels = channels; }
        
        public Map<String, Object> getTemplateData() { return templateData; }
        public void setTemplateData(Map<String, Object> templateData) { this.templateData = templateData; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Locale getLocale() { return locale; }
        public void setLocale(Locale locale) { this.locale = locale; }
        
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        
        public boolean isUrgent() { return urgent; }
        public void setUrgent(boolean urgent) { this.urgent = urgent; }
        
        // Helper methods
        public void addChannel(NotificationChannel channel) {
            this.channels.add(channel);
        }
        
        public void addTemplateData(String key, Object value) {
            this.templateData.put(key, value);
        }
    }
    
    /**
     * Notification result
     */
    public static class NotificationResult {
        private String notificationId;
        private boolean success;
        private Map<NotificationChannel, Boolean> channelResults;
        private String errorMessage;
        private LocalDateTime sentAt;
        
        public NotificationResult() {
            this.channelResults = new HashMap<>();
            this.sentAt = LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Map<NotificationChannel, Boolean> getChannelResults() { return channelResults; }
        public void setChannelResults(Map<NotificationChannel, Boolean> channelResults) { this.channelResults = channelResults; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
        
        public void addChannelResult(NotificationChannel channel, boolean success) {
            this.channelResults.put(channel, success);
        }
    }
    
    /**
     * Send notification through multiple channels
     * @param request Notification request
     * @return Notification result
     */
    public CompletableFuture<NotificationResult> sendNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            NotificationResult result = new NotificationResult();
            result.setNotificationId(UUID.randomUUID().toString());
            
            try {
                // Validate request
                if (!isValidRequest(request)) {
                    result.setSuccess(false);
                    result.setErrorMessage("Invalid notification request");
                    return result;
                }
                
                // Prepare message content
                String subject = prepareSubject(request);
                String message = prepareMessage(request);
                
                // Send through each channel
                boolean overallSuccess = true;
                for (NotificationChannel channel : request.getChannels()) {
                    boolean channelSuccess = sendThroughChannel(channel, request, subject, message);
                    result.addChannelResult(channel, channelSuccess);
                    if (!channelSuccess) {
                        overallSuccess = false;
                    }
                }
                
                result.setSuccess(overallSuccess);
                
                // Log notification
                logNotification(request, result);
                
            } catch (Exception e) {
                logger.error("Error sending notification: ", e);
                result.setSuccess(false);
                result.setErrorMessage(e.getMessage());
            }
            
            return result;
        });
    }
    
    /**
     * Send order confirmation notification
     * @param orderId Order ID
     * @param customerEmail Customer email
     * @param customerPhone Customer phone
     * @param orderDetails Order details
     * @param locale Customer locale
     * @return Notification result
     */
    public CompletableFuture<NotificationResult> sendOrderConfirmation(
            String orderId, String customerEmail, String customerPhone, 
            Map<String, Object> orderDetails, Locale locale) {
        
        NotificationRequest request = new NotificationRequest();
        request.setType(NotificationType.ORDER_CONFIRMATION);
        request.setRecipientEmail(customerEmail);
        request.setRecipientPhone(customerPhone);
        request.setLocale(locale);
        request.addChannel(NotificationChannel.EMAIL);
        request.addChannel(NotificationChannel.SMS);
        
        // Add order details to template data
        request.addTemplateData("orderId", orderId);
        request.addTemplateData("orderDetails", orderDetails);
        
        return sendNotification(request);
    }
    
    /**
     * Send shipping update notification
     * @param orderId Order ID
     * @param customerEmail Customer email
     * @param customerPhone Customer phone
     * @param trackingNumber Tracking number
     * @param estimatedDelivery Estimated delivery date
     * @param locale Customer locale
     * @return Notification result
     */
    public CompletableFuture<NotificationResult> sendShippingUpdate(
            String orderId, String customerEmail, String customerPhone,
            String trackingNumber, String estimatedDelivery, Locale locale) {
        
        NotificationRequest request = new NotificationRequest();
        request.setType(NotificationType.ORDER_SHIPPED);
        request.setRecipientEmail(customerEmail);
        request.setRecipientPhone(customerPhone);
        request.setLocale(locale);
        request.addChannel(NotificationChannel.EMAIL);
        request.addChannel(NotificationChannel.SMS);
        request.addChannel(NotificationChannel.WHATSAPP);
        
        // Add shipping details to template data
        request.addTemplateData("orderId", orderId);
        request.addTemplateData("trackingNumber", trackingNumber);
        request.addTemplateData("estimatedDelivery", estimatedDelivery);
        
        return sendNotification(request);
    }
    
    /**
     * Send delivery confirmation notification
     * @param orderId Order ID
     * @param customerEmail Customer email
     * @param customerPhone Customer phone
     * @param deliveryDate Delivery date
     * @param locale Customer locale
     * @return Notification result
     */
    public CompletableFuture<NotificationResult> sendDeliveryConfirmation(
            String orderId, String customerEmail, String customerPhone,
            String deliveryDate, Locale locale) {
        
        NotificationRequest request = new NotificationRequest();
        request.setType(NotificationType.ORDER_DELIVERED);
        request.setRecipientEmail(customerEmail);
        request.setRecipientPhone(customerPhone);
        request.setLocale(locale);
        request.addChannel(NotificationChannel.EMAIL);
        request.addChannel(NotificationChannel.SMS);
        
        // Add delivery details to template data
        request.addTemplateData("orderId", orderId);
        request.addTemplateData("deliveryDate", deliveryDate);
        
        return sendNotification(request);
    }
    
    /**
     * Validate notification request
     * @param request Notification request
     * @return true if valid, false otherwise
     */
    private boolean isValidRequest(NotificationRequest request) {
        if (request == null || request.getType() == null || request.getChannels().isEmpty()) {
            return false;
        }
        
        // Check if at least one contact method is provided
        boolean hasContact = false;
        for (NotificationChannel channel : request.getChannels()) {
            switch (channel) {
                case EMAIL:
                    if (StringUtils.hasText(request.getRecipientEmail())) {
                        hasContact = true;
                    }
                    break;
                case SMS:
                case WHATSAPP:
                    if (StringUtils.hasText(request.getRecipientPhone())) {
                        hasContact = true;
                    }
                    break;
            }
        }
        
        return hasContact;
    }
    
    /**
     * Prepare subject line for notification
     * @param request Notification request
     * @return Subject line
     */
    private String prepareSubject(NotificationRequest request) {
        if (StringUtils.hasText(request.getSubject())) {
            return request.getSubject();
        }
        
        // Generate subject based on notification type
        String messageKey = "notification." + request.getType().name().toLowerCase() + ".subject";
        Object[] args = extractTemplateArgs(request.getTemplateData());
        
        return messageService.getMessage(messageKey, args, request.getLocale());
    }
    
    /**
     * Prepare message content for notification
     * @param request Notification request
     * @return Message content
     */
    private String prepareMessage(NotificationRequest request) {
        if (StringUtils.hasText(request.getMessage())) {
            return request.getMessage();
        }
        
        // Generate message based on notification type
        String messageKey = "notification." + request.getType().name().toLowerCase() + ".message";
        Object[] args = extractTemplateArgs(request.getTemplateData());
        
        return messageService.getMessage(messageKey, args, request.getLocale());
    }
    
    /**
     * Extract template arguments from template data
     * @param templateData Template data map
     * @return Array of arguments
     */
    private Object[] extractTemplateArgs(Map<String, Object> templateData) {
        if (templateData == null || templateData.isEmpty()) {
            return new Object[0];
        }
        
        // Common template variables
        List<Object> args = new ArrayList<>();
        
        // Add common variables in expected order
        if (templateData.containsKey("orderId")) {
            args.add(templateData.get("orderId"));
        }
        if (templateData.containsKey("customerName")) {
            args.add(templateData.get("customerName"));
        }
        if (templateData.containsKey("amount")) {
            args.add(templateData.get("amount"));
        }
        if (templateData.containsKey("trackingNumber")) {
            args.add(templateData.get("trackingNumber"));
        }
        
        return args.toArray();
    }
    
    /**
     * Send notification through specific channel
     * @param channel Notification channel
     * @param request Notification request
     * @param subject Subject line
     * @param message Message content
     * @return true if successful, false otherwise
     */
    private boolean sendThroughChannel(NotificationChannel channel, NotificationRequest request, 
                                     String subject, String message) {
        try {
            switch (channel) {
                case EMAIL:
                    return sendEmail(request.getRecipientEmail(), subject, message);
                case SMS:
                    return sendSMS(request.getRecipientPhone(), message);
                case WHATSAPP:
                    return sendWhatsApp(request.getRecipientPhone(), message);
                case PUSH:
                    return sendPushNotification(request.getRecipientId(), subject, message);
                default:
                    logger.warn("Unsupported notification channel: {}", channel);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error sending notification through channel {}: ", channel, e);
            return false;
        }
    }
    
    /**
     * Send email notification
     * @param email Recipient email
     * @param subject Email subject
     * @param message Email message
     * @return true if successful, false otherwise
     */
    private boolean sendEmail(String email, String subject, String message) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        
        try {
            // TODO: Integrate with actual email service (e.g., SendGrid, AWS SES)
            logger.info("Sending email to: {} with subject: {}", email, subject);
            
            // Mock email sending - replace with actual implementation
            Thread.sleep(100); // Simulate network delay
            
            logger.info("Email sent successfully to: {}", email);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send email to {}: ", email, e);
            return false;
        }
    }
    
    /**
     * Send SMS notification
     * @param phone Recipient phone number
     * @param message SMS message
     * @return true if successful, false otherwise
     */
    private boolean sendSMS(String phone, String message) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        
        try {
            // TODO: Integrate with actual SMS service (e.g., Twilio, AWS SNS)
            logger.info("Sending SMS to: {} with message: {}", phone, message);
            
            // Mock SMS sending - replace with actual implementation
            Thread.sleep(50); // Simulate network delay
            
            logger.info("SMS sent successfully to: {}", phone);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: ", phone, e);
            return false;
        }
    }
    
    /**
     * Send WhatsApp notification
     * @param phone Recipient phone number
     * @param message WhatsApp message
     * @return true if successful, false otherwise
     */
    private boolean sendWhatsApp(String phone, String message) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        
        try {
            // TODO: Integrate with WhatsApp Business API
            logger.info("Sending WhatsApp message to: {} with message: {}", phone, message);
            
            // Mock WhatsApp sending - replace with actual implementation
            Thread.sleep(75); // Simulate network delay
            
            logger.info("WhatsApp message sent successfully to: {}", phone);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp message to {}: ", phone, e);
            return false;
        }
    }
    
    /**
     * Send push notification
     * @param userId User ID
     * @param title Notification title
     * @param message Notification message
     * @return true if successful, false otherwise
     */
    private boolean sendPushNotification(String userId, String title, String message) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        
        try {
            // TODO: Integrate with push notification service (e.g., Firebase, AWS SNS)
            logger.info("Sending push notification to user: {} with title: {}", userId, title);
            
            // Mock push notification sending - replace with actual implementation
            Thread.sleep(25); // Simulate network delay
            
            logger.info("Push notification sent successfully to user: {}", userId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send push notification to user {}: ", userId, e);
            return false;
        }
    }
    
    /**
     * Log notification for audit purposes
     * @param request Notification request
     * @param result Notification result
     */
    private void logNotification(NotificationRequest request, NotificationResult result) {
        logger.info("Notification sent - ID: {}, Type: {}, Channels: {}, Success: {}",
            result.getNotificationId(),
            request.getType(),
            request.getChannels(),
            result.isSuccess());
        
        // TODO: Store notification log in database for audit trail
    }
}
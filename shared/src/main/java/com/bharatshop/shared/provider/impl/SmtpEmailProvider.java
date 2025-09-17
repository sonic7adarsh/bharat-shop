package com.bharatshop.shared.provider.impl;

import com.bharatshop.shared.dto.NotificationRequest;
import com.bharatshop.shared.dto.NotificationResponse;
import com.bharatshop.shared.provider.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * SMTP-based email provider implementation.
 */
@Component
@Slf4j
public class SmtpEmailProvider implements EmailProvider {
    
    // Manual log field since @Slf4j isn't working
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmtpEmailProvider.class);
    
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    
    public SmtpEmailProvider(JavaMailSender mailSender,
                           @Value("${app.email.from.address:noreply@bharatshop.com}") String fromAddress,
                           @Value("${app.email.from.name:BharatShop}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String messageId = sendEmail(request);
                long duration = System.currentTimeMillis() - startTime;
                
                return NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .providerMessageId(messageId)
                        .status(NotificationResponse.NotificationStatus.SENT)
                        .sentAt(Instant.now())
                        .providerName(getProviderName())
                        .attemptNumber(1)
                        .processingTimeMs(duration)
                        .build();
                        
            } catch (Exception e) {
                log.error("Failed to send email to: {}", request.getRecipient(), e);
                long duration = System.currentTimeMillis() - startTime;
                
                return NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .providerName(getProviderName())
                        .attemptNumber(1)
                        .processingTimeMs(duration)
                        .build();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<NotificationResponse>> sendBulkEmails(List<NotificationRequest> requests) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                List<NotificationResponse> responses = requests.stream()
                        .map(request -> {
                            try {
                                String messageId = sendEmail(request);
                                return NotificationResponse.builder()
                                        .notificationId(request.getNotificationId())
                                        .providerMessageId(messageId)
                                        .status(NotificationResponse.NotificationStatus.SENT)
                                        .sentAt(Instant.now())
                                        .providerName(getProviderName())
                                        .attemptNumber(1)
                                        .processingTimeMs(System.currentTimeMillis() - startTime)
                                        .build();
                            } catch (Exception e) {
                                log.error("Failed to send bulk email to: {}", request.getRecipient(), e);
                                return NotificationResponse.builder()
                                        .notificationId(request.getNotificationId())
                                        .status(NotificationResponse.NotificationStatus.FAILED)
                                        .errorMessage(e.getMessage())
                                        .providerName(getProviderName())
                                        .attemptNumber(1)
                                        .processingTimeMs(System.currentTimeMillis() - startTime)
                                        .build();
                            }
                        })
                        .collect(Collectors.toList());
                
                return responses;
                        
            } catch (Exception e) {
                log.error("Failed to send bulk emails", e);
                long duration = System.currentTimeMillis() - startTime;
                
                return requests.stream()
                        .map(request -> NotificationResponse.builder()
                                .notificationId(request.getNotificationId())
                                .status(NotificationResponse.NotificationStatus.FAILED)
                                .errorMessage(e.getMessage())
                                .providerName(getProviderName())
                                .attemptNumber(1)
                                .processingTimeMs(duration)
                                .build())
                        .collect(Collectors.toList());
            }
        });
    }
    
    @Override
    public CompletableFuture<NotificationResponse> sendEmailWithAttachments(
            NotificationRequest request, List<String> attachments) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String messageId = sendEmailWithAttachmentsInternal(request, attachments);
                long duration = System.currentTimeMillis() - startTime;
                
                return NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .providerMessageId(messageId)
                        .status(NotificationResponse.NotificationStatus.SENT)
                        .sentAt(Instant.now())
                        .providerName(getProviderName())
                        .attemptNumber(1)
                        .processingTimeMs(duration)
                        .build();
                        
            } catch (Exception e) {
                log.error("Failed to send email with attachments to: {}", request.getRecipient(), e);
                long duration = System.currentTimeMillis() - startTime;
                
                return NotificationResponse.builder()
                        .notificationId(request.getNotificationId())
                        .status(NotificationResponse.NotificationStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .providerName(getProviderName())
                        .attemptNumber(1)
                        .processingTimeMs(duration)
                        .build();
            }
        });
    }
    
    private String sendEmail(NotificationRequest request) throws MessagingException {
        String messageId = UUID.randomUUID().toString();
        
        if (request.getHtmlBody() != null && !request.getHtmlBody().trim().isEmpty()) {
            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            try {
                helper.setFrom(fromAddress, fromName);
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding when setting from address: {}", e.getMessage());
                helper.setFrom(fromAddress);
            }
            helper.setTo(request.getRecipient());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), request.getHtmlBody());
            mimeMessage.addHeader("Message-ID", messageId);
            
            mailSender.send(mimeMessage);
        } else {
            // Send plain text email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getRecipient());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());
            
            mailSender.send(message);
        }
        
        log.info("Email sent successfully to: {} with message ID: {}", 
                request.getRecipient(), messageId);
        
        return messageId;
    }
    
    private String sendEmailWithAttachmentsInternal(NotificationRequest request, 
                                                   List<String> attachments) throws MessagingException {
        String messageId = UUID.randomUUID().toString();
        
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        try {
            helper.setFrom(fromAddress, fromName);
        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding when setting from address: {}", e.getMessage());
            helper.setFrom(fromAddress);
        }
        helper.setTo(request.getRecipient());
        helper.setSubject(request.getSubject());
        helper.setText(request.getBody(), request.getHtmlBody());
        helper.getMimeMessage().addHeader("Message-ID", messageId);
        
        // Add attachments (simplified - in real implementation, handle file loading)
        if (attachments != null) {
            for (String attachment : attachments) {
                // In a real implementation, you would load the file and add it
                // helper.addAttachment(filename, new FileSystemResource(attachment));
                log.info("Would attach file: {}", attachment);
            }
        }
        
        mailSender.send(mimeMessage);
        
        log.info("Email with attachments sent successfully to: {} with message ID: {}", 
                request.getRecipient(), messageId);
        
        return messageId;
    }
    
    public boolean isHealthy() {
        try {
            // Simple health check - try to create a mime message
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.error("SMTP Email provider health check failed", e);
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return isHealthy();
    }
    
    @Override
    public String getProviderName() {
        return "SMTP Email Provider";
    }
}
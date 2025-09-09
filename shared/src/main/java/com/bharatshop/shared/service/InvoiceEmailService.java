package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Invoice;
import com.bharatshop.shared.entity.MediaFile;
import com.bharatshop.shared.repository.MediaFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class InvoiceEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MediaFileRepository mediaFileRepository;
    private final InvoicePdfService invoicePdfService;

    public InvoiceEmailService(TemplateEngine templateEngine, 
                              MediaFileRepository mediaFileRepository,
                              InvoicePdfService invoicePdfService,
                              @Autowired(required = false) JavaMailSender mailSender) {
        this.templateEngine = templateEngine;
        this.mediaFileRepository = mediaFileRepository;
        this.invoicePdfService = invoicePdfService;
        this.mailSender = mailSender;
    }

    @Value("${app.email.from:noreply@bharatshop.com}")
    private String fromEmail;

    @Value("${app.email.from-name:BharatShop}")
    private String fromName;

    @Value("${app.company.name:BharatShop}")
    private String companyName;

    @Value("${app.company.support-email:support@bharatshop.com}")
    private String supportEmail;

    @Value("${app.company.website:https://bharatshop.com}")
    private String companyWebsite;

    /**
     * Send invoice email to customer
     */
    public void sendInvoiceEmail(Invoice invoice, String recipientEmail) {
        if (mailSender == null) {
            log.warn("Email service not configured. Skipping invoice email for: {}", invoice.getInvoiceNumber());
            return;
        }
        
        try {
            log.info("Sending invoice email for invoice: {} to: {}", invoice.getInvoiceNumber(), recipientEmail);
            
            String subject = String.format("Tax Invoice %s - %s", invoice.getInvoiceNumber(), companyName);
            String htmlContent = generateEmailContent(invoice);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Attach PDF if available
            attachInvoicePdf(helper, invoice);
            
            mailSender.send(message);
            log.info("Invoice email sent successfully for invoice: {}", invoice.getInvoiceNumber());
            
        } catch (Exception e) {
            log.error("Failed to send invoice email for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage(), e);
        }
    }

    /**
     * Send invoice email with custom recipient and message
     */
    public void sendInvoiceEmail(Invoice invoice, String recipientEmail, String customMessage) {
        if (mailSender == null) {
            log.warn("Email service not configured. Skipping custom invoice email for: {}", invoice.getInvoiceNumber());
            return;
        }
        
        try {
            log.info("Sending custom invoice email for invoice: {} to: {}", invoice.getInvoiceNumber(), recipientEmail);
            
            String subject = String.format("Tax Invoice %s - %s", invoice.getInvoiceNumber(), companyName);
            String htmlContent = generateEmailContent(invoice, customMessage);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Attach PDF if available
            attachInvoicePdf(helper, invoice);
            
            mailSender.send(message);
            log.info("Custom invoice email sent successfully for invoice: {}", invoice.getInvoiceNumber());
            
        } catch (Exception e) {
            log.error("Failed to send custom invoice email for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage(), e);
        }
    }

    /**
     * Generate email content using Thymeleaf template
     */
    private String generateEmailContent(Invoice invoice) {
        return generateEmailContent(invoice, null);
    }

    /**
     * Generate email content with custom message
     */
    private String generateEmailContent(Invoice invoice, String customMessage) {
        Context context = new Context();
        
        // Invoice details
        context.setVariable("invoice", invoice);
        context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
        context.setVariable("invoiceDate", invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        context.setVariable("dueDate", invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        context.setVariable("totalAmount", invoice.getTotalAmount());
        context.setVariable("currency", invoice.getCurrency());
        
        // Customer details
        context.setVariable("customerName", invoice.getBuyerName());
        context.setVariable("customerEmail", invoice.getBuyerEmail());
        
        // Company details
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("companyWebsite", companyWebsite);
        
        // Custom message
        context.setVariable("customMessage", customMessage);
        context.setVariable("hasCustomMessage", customMessage != null && !customMessage.trim().isEmpty());
        
        // Tax information
        context.setVariable("isIntraState", invoice.isIntraState());
        context.setVariable("isInterState", invoice.isInterState());
        
        // Payment status - tracked in Order entity, defaulting to pending for invoice emails
        context.setVariable("isPaid", false);
        context.setVariable("isPending", true);
        
        return templateEngine.process("invoice-email-template", context);
    }

    /**
     * Attach invoice PDF to email
     */
    private void attachInvoicePdf(MimeMessageHelper helper, Invoice invoice) throws MessagingException, IOException {
        byte[] pdfBytes = null;
        
        // Try to get existing PDF from MediaFile
        if (invoice.getPdfMediaId() != null) {
            Optional<MediaFile> mediaFileOpt = mediaFileRepository.findById(invoice.getPdfMediaId());
            if (mediaFileOpt.isPresent()) {
                MediaFile mediaFile = mediaFileOpt.get();
                // Note: MediaFile stores files in S3, not as binary data
                // For now, we'll regenerate the PDF instead of downloading from S3
                log.debug("MediaFile found for invoice: {}, but will regenerate PDF", invoice.getInvoiceNumber());
            }
        }
        
        // Generate PDF if not available
        if (pdfBytes == null) {
            log.debug("Generating new PDF for invoice: {}", invoice.getInvoiceNumber());
            pdfBytes = invoicePdfService.generateInvoicePdf(invoice);
        }
        
        if (pdfBytes != null && pdfBytes.length > 0) {
            String filename = String.format("invoice-%s.pdf", invoice.getInvoiceNumber());
            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);
            helper.addAttachment(filename, pdfResource);
            log.debug("PDF attached to email for invoice: {}", invoice.getInvoiceNumber());
        } else {
            log.warn("No PDF available to attach for invoice: {}", invoice.getInvoiceNumber());
        }
    }

    /**
     * Send payment reminder email
     */
    public void sendPaymentReminderEmail(Invoice invoice, String recipientEmail) {
        try {
            log.info("Sending payment reminder for invoice: {} to: {}", invoice.getInvoiceNumber(), recipientEmail);
            
            String subject = String.format("Payment Reminder - Invoice %s", invoice.getInvoiceNumber());
            String htmlContent = generatePaymentReminderContent(invoice);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Attach PDF
            attachInvoicePdf(helper, invoice);
            
            mailSender.send(message);
            log.info("Payment reminder sent successfully for invoice: {}", invoice.getInvoiceNumber());
            
        } catch (Exception e) {
            log.error("Failed to send payment reminder for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to send payment reminder: " + e.getMessage(), e);
        }
    }

    /**
     * Generate payment reminder email content
     */
    private String generatePaymentReminderContent(Invoice invoice) {
        Context context = new Context();
        
        context.setVariable("invoice", invoice);
        context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
        context.setVariable("invoiceDate", invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        context.setVariable("dueDate", invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        context.setVariable("totalAmount", invoice.getTotalAmount());
        context.setVariable("currency", invoice.getCurrency());
        context.setVariable("customerName", invoice.getBuyerName());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("companyWebsite", companyWebsite);
        
        // Calculate days overdue
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
            invoice.getDueDate(), 
            java.time.LocalDate.now()
        );
        context.setVariable("daysOverdue", Math.max(0, daysOverdue));
        context.setVariable("isOverdue", daysOverdue > 0);
        
        return templateEngine.process("payment-reminder-email-template", context);
    }

    /**
     * Send invoice status update email
     */
    public void sendInvoiceStatusUpdateEmail(Invoice invoice, String recipientEmail, 
                                           Invoice.InvoiceStatus oldStatus, Invoice.InvoiceStatus newStatus) {
        try {
            log.info("Sending status update email for invoice: {} from {} to {}", 
                invoice.getInvoiceNumber(), oldStatus, newStatus);
            
            String subject = String.format("Invoice %s Status Updated - %s", 
                invoice.getInvoiceNumber(), newStatus.name());
            String htmlContent = generateStatusUpdateContent(invoice, oldStatus, newStatus);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Status update email sent successfully for invoice: {}", invoice.getInvoiceNumber());
            
        } catch (Exception e) {
            log.error("Failed to send status update email for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to send status update email: " + e.getMessage(), e);
        }
    }

    /**
     * Generate status update email content
     */
    private String generateStatusUpdateContent(Invoice invoice, 
                                             Invoice.InvoiceStatus oldStatus, 
                                             Invoice.InvoiceStatus newStatus) {
        Context context = new Context();
        
        context.setVariable("invoice", invoice);
        context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
        context.setVariable("customerName", invoice.getBuyerName());
        context.setVariable("oldStatus", oldStatus.name());
        context.setVariable("newStatus", newStatus.name());
        context.setVariable("companyName", companyName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("companyWebsite", companyWebsite);
        
        return templateEngine.process("invoice-status-update-email-template", context);
    }

    /**
     * Validate email configuration
     */
    public boolean isEmailConfigured() {
        try {
            return mailSender != null && fromEmail != null && !fromEmail.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Email configuration validation failed", e);
            return false;
        }
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.*;
import com.bharatshop.shared.repository.*;
import com.bharatshop.shared.service.PriceCalculationService.PriceBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing GST-compliant invoices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceNumberSequenceRepository sequenceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PriceCalculationService priceCalculationService;
    private final NumberToWordsService numberToWordsService;
    private final InvoiceEmailService invoiceEmailService;
    private final InvoicePdfService invoicePdfService;

    /**
     * Generate invoice for an order with email and PDF options
     */
    @Transactional
    public Invoice generateInvoiceFromOrder(Long orderId, Long tenantId, boolean generatePdf, boolean sendEmail) {
        return generateInvoiceFromOrder(orderId, tenantId, generatePdf, sendEmail, null, null);
    }

    /**
     * Generate invoice for an order with custom email and message
     */
    @Transactional
    public Invoice generateInvoiceFromOrder(Long orderId, Long tenantId, boolean generatePdf, 
                                          boolean sendEmail, String customEmail, String customMessage) {
        // Get order details
        Orders order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Check if invoice already exists
        Optional<Invoice> existingInvoice = invoiceRepository.findByOrderIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId);
        if (existingInvoice.isPresent()) {
            Invoice invoice = existingInvoice.get();
            
            // Handle PDF generation if requested
            if (generatePdf && invoice.getPdfMediaId() == null) {
                generateAndSavePdf(invoice);
            }
            
            // Handle email sending if requested
            if (sendEmail && invoiceEmailService.isEmailConfigured()) {
                sendInvoiceEmail(invoice, customEmail, customMessage);
            }
            
            return invoice;
        }

        // Generate new invoice
        Invoice invoice = generateInvoice(orderId, tenantId);
        
        // Handle PDF generation if requested
        if (generatePdf) {
            generateAndSavePdf(invoice);
        }
        
        // Handle email sending if requested
        if (sendEmail && invoiceEmailService.isEmailConfigured()) {
            sendInvoiceEmail(invoice, customEmail, customMessage);
            // Mark as sent if email was sent successfully
            invoice.setStatus(Invoice.InvoiceStatus.SENT);
            invoice.setSentAt(LocalDateTime.now());
            invoice = invoiceRepository.save(invoice);
        }
        
        return invoice;
    }

    /**
     * Generate invoice for an order (internal method)
     */
    @Transactional
    public Invoice generateInvoice(Long orderId, Long tenantId) {
        // Get order details
        Orders order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Check if invoice already exists
        Optional<Invoice> existingInvoice = invoiceRepository.findByOrderIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(tenantId);

        // Create invoice
        Invoice invoice = createInvoiceFromOrder(order, invoiceNumber, tenantId);
        invoice = invoiceRepository.save(invoice);

        // Create invoice items
        List<InvoiceItem> invoiceItems = createInvoiceItems(invoice, order, tenantId);
        invoiceItemRepository.saveAll(invoiceItems);

        // Update invoice totals
        updateInvoiceTotals(invoice, invoiceItems);
        invoice = invoiceRepository.save(invoice);

        System.out.println("Generated invoice " + invoiceNumber + " for order " + orderId);
        return invoice;
    }

    /**
     * Generate unique invoice number
     */
    @Transactional
    public String generateInvoiceNumber(Long tenantId) {
        LocalDate now = LocalDate.now();
        String financialYear = InvoiceNumberSequence.getFinancialYear(now.getYear(), now.getMonthValue());

        // Get or create sequence for current financial year
        InvoiceNumberSequence sequence = sequenceRepository
                .findByTenantIdAndFinancialYearAndDeletedAtIsNull(tenantId, financialYear)
                .orElseGet(() -> {
                    InvoiceNumberSequence newSeq = InvoiceNumberSequence.builder()
                            .tenantId(tenantId)
                            .financialYear(financialYear)
                            .prefix("INV")
                            .currentNumber(0L)
                            .paddingLength(6)
                            .build();
                    return sequenceRepository.save(newSeq);
                });

        String invoiceNumber = sequence.generateNextNumber();
        sequenceRepository.save(sequence);

        return invoiceNumber;
    }

    /**
     * Create invoice from order
     */
    private Invoice createInvoiceFromOrder(Orders order, String invoiceNumber, Long tenantId) {
        // Determine tax type based on addresses
        String sellerStateCode = "MH"; // TODO: Get from tenant/merchant settings
        String buyerStateCode = extractStateCodeFromOrder(order);
        
        Invoice.TaxType taxType = sellerStateCode.equals(buyerStateCode) 
                ? Invoice.TaxType.INTRA_STATE 
                : Invoice.TaxType.INTER_STATE;

        return Invoice.builder()
                .tenantId(tenantId)
                .orderId(order.getId())
                .invoiceNumber(invoiceNumber)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                
                // Seller details (TODO: Get from tenant settings)
                .sellerName("BharatShop Merchant")
                .sellerAddress("123 Business Street, Mumbai, Maharashtra 400001")
                .sellerGstin("27ABCDE1234F1Z5")
                .sellerPan("ABCDE1234F")
                .sellerStateCode(sellerStateCode)
                .sellerEmail("merchant@bharatshop.com")
                .sellerPhone("+91-9876543210")
                
                // Buyer details
                .buyerName(order.getShippingName())
                .buyerAddress(order.getFullShippingAddress())
                .buyerStateCode(buyerStateCode)
                .buyerEmail("customer@example.com") // TODO: Get from Customer entity
                .buyerPhone(order.getShippingPhone())
                
                // Tax details
                .taxType(taxType)
                .placeOfSupply(buyerStateCode)
                
                // Initialize amounts (will be calculated later)
                .subtotal(BigDecimal.ZERO)
                .totalCgst(BigDecimal.ZERO)
                .totalSgst(BigDecimal.ZERO)
                .totalIgst(BigDecimal.ZERO)
                .totalCess(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                
                .currency("INR")
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();
    }

    /**
     * Create invoice items from order items
     */
    private List<InvoiceItem> createInvoiceItems(Invoice invoice, Orders order, Long tenantId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(order.getId());
        List<InvoiceItem> invoiceItems = new ArrayList<>();

        String sellerStateCode = "MH"; // TODO: Get from tenant settings
        String buyerStateCode = extractStateCodeFromOrder(order);

        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            if (product == null) {
                System.out.println("Product not found for order item " + orderItem.getId());
                continue;
            }

            // Calculate price with tax
            PriceBreakdown priceBreakdown = priceCalculationService.calculatePrice(
                    product, orderItem.getPrice(), buyerStateCode, sellerStateCode, tenantId);

            InvoiceItem invoiceItem = InvoiceItem.builder()
                    .tenantId(tenantId)
                    .invoiceId(invoice.getId())
                    .productId(product.getId())
                    .productVariantId(orderItem.getVariantId())
                    .orderItemId(orderItem.getId())
                    
                    // Product details
                    .productName(orderItem.getProductName())
                    .productDescription(product.getDescription())
                    .sku(orderItem.getProductSku())
                    .hsnCode(product.getHsnCode() != null ? product.getHsnCode() : "0000")
                    
                    // Quantity and pricing
                    .quantity(BigDecimal.valueOf(orderItem.getQuantity()))
                    .unit("PCS")
                    .unitPrice(orderItem.getPrice())
                    .netAmount(priceBreakdown.getNetPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    
                    // Tax details
                    .cgstAmount(priceBreakdown.getCgstAmount().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    .sgstAmount(priceBreakdown.getSgstAmount().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    .igstAmount(priceBreakdown.getIgstAmount().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    .cessAmount(priceBreakdown.getCessAmount().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    .totalTaxAmount(priceBreakdown.getTotalTaxAmount().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    .totalAmount(priceBreakdown.getTotalPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                    
                    .taxPreference(product.getTaxPreference())
                    .isTaxInclusive(product.getIsTaxInclusive())
                    .build();

            // Set tax rates (extract from price breakdown)
            if (priceBreakdown.getTaxType() == TaxRate.TaxType.INTRA_STATE) {
                if (priceBreakdown.getCgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                    invoiceItem.setCgstRate(calculateTaxRate(priceBreakdown.getCgstAmount(), priceBreakdown.getNetPrice()));
                }
                if (priceBreakdown.getSgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                    invoiceItem.setSgstRate(calculateTaxRate(priceBreakdown.getSgstAmount(), priceBreakdown.getNetPrice()));
                }
            } else {
                if (priceBreakdown.getIgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                    invoiceItem.setIgstRate(calculateTaxRate(priceBreakdown.getIgstAmount(), priceBreakdown.getNetPrice()));
                }
            }
            if (priceBreakdown.getCessAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoiceItem.setCessRate(calculateTaxRate(priceBreakdown.getCessAmount(), priceBreakdown.getNetPrice()));
            }

            invoiceItems.add(invoiceItem);
        }

        return invoiceItems;
    }

    /**
     * Update invoice totals from items
     */
    private void updateInvoiceTotals(Invoice invoice, List<InvoiceItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal totalCess = BigDecimal.ZERO;

        for (InvoiceItem item : items) {
            subtotal = subtotal.add(item.getNetAmount());
            if (item.getCgstAmount() != null) totalCgst = totalCgst.add(item.getCgstAmount());
            if (item.getSgstAmount() != null) totalSgst = totalSgst.add(item.getSgstAmount());
            if (item.getIgstAmount() != null) totalIgst = totalIgst.add(item.getIgstAmount());
            if (item.getCessAmount() != null) totalCess = totalCess.add(item.getCessAmount());
        }

        BigDecimal totalTax = totalCgst.add(totalSgst).add(totalIgst).add(totalCess);
        BigDecimal totalAmount = subtotal.add(totalTax);

        invoice.setSubtotal(subtotal);
        invoice.setTotalCgst(totalCgst);
        invoice.setTotalSgst(totalSgst);
        invoice.setTotalIgst(totalIgst);
        invoice.setTotalCess(totalCess);
        invoice.setTotalTax(totalTax);
        invoice.setTotalAmount(totalAmount);
        invoice.setAmountInWords(numberToWordsService.convertToWords(totalAmount));
    }

    /**
     * Calculate tax rate percentage from amount and base
     */
    private BigDecimal calculateTaxRate(BigDecimal taxAmount, BigDecimal baseAmount) {
        if (baseAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return taxAmount.multiply(BigDecimal.valueOf(100)).divide(baseAmount, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Extract state code from address (simplified implementation)
     */
    private String extractStateCodeFromAddress(String address) {
        // TODO: Implement proper address parsing
        // For now, use shipping state from order
        return "MH"; // Default state code
    }
    
    /**
     * Extract state code from order shipping details
     */
    private String extractStateCodeFromOrder(Orders order) {
        // TODO: Map state names to state codes
        // For now, return default state code
        return "MH"; // Default state code
    }

    /**
     * Mark invoice as sent
     */
    @Transactional
    public Invoice markAsSent(Long invoiceId, Long tenantId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        invoice.setStatus(Invoice.InvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());
        
        return invoiceRepository.save(invoice);
    }

    /**
     * Get invoice by ID
     */
    public Optional<Invoice> getInvoice(Long invoiceId, Long tenantId) {
        return invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId);
    }

    /**
     * Get invoice by order ID
     */
    public Optional<Invoice> getInvoiceByOrderId(Long orderId, Long tenantId) {
        return invoiceRepository.findByOrderIdAndTenantIdAndDeletedAtIsNull(orderId, tenantId);
    }

    /**
     * Generate and save PDF for invoice
     */
    @Transactional
    public void generateAndSavePdf(Invoice invoice) {
        try {
            log.info("Generating PDF for invoice: {}", invoice.getInvoiceNumber());
            byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoice);
            
            // TODO: Save PDF to MediaFile and update invoice.pdfFileId
            // This requires MediaFileService integration
            
            log.info("PDF generated successfully for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Send invoice email
     */
    private void sendInvoiceEmail(Invoice invoice, String customEmail, String customMessage) {
        try {
            String recipientEmail = customEmail != null ? customEmail : invoice.getBuyerEmail();
            
            if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                log.warn("No email address available for invoice: {}", invoice.getInvoiceNumber());
                return;
            }
            
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                invoiceEmailService.sendInvoiceEmail(invoice, recipientEmail, customMessage);
            } else {
                invoiceEmailService.sendInvoiceEmail(invoice, recipientEmail);
            }
            
            log.info("Invoice email sent successfully for invoice: {} to: {}", 
                invoice.getInvoiceNumber(), recipientEmail);
                
        } catch (Exception e) {
            log.error("Failed to send invoice email for invoice: {}", invoice.getInvoiceNumber(), e);
            // Don't throw exception here to avoid breaking the invoice generation process
        }
    }

    /**
     * Resend invoice email
     */
    @Transactional
    public void resendInvoiceEmail(Long invoiceId, Long tenantId, String customEmail, String customMessage) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        sendInvoiceEmail(invoice, customEmail, customMessage);
        
        // Update sent timestamp
        invoice.setSentAt(LocalDateTime.now());
        if (invoice.getStatus() == Invoice.InvoiceStatus.DRAFT) {
            invoice.setStatus(Invoice.InvoiceStatus.SENT);
        }
        invoiceRepository.save(invoice);
    }

    /**
     * Send payment reminder
     */
    @Transactional
    public void sendPaymentReminder(Long invoiceId, Long tenantId, String customEmail) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        String recipientEmail = customEmail != null ? customEmail : invoice.getBuyerEmail();
        
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new RuntimeException("No email address available for payment reminder");
        }
        
        invoiceEmailService.sendPaymentReminderEmail(invoice, recipientEmail);
        log.info("Payment reminder sent for invoice: {} to: {}", invoice.getInvoiceNumber(), recipientEmail);
    }

    /**
     * Update invoice status and send notification
     */
    @Transactional
    public Invoice updateInvoiceStatus(Long invoiceId, Long tenantId, Invoice.InvoiceStatus newStatus, 
                                     boolean sendNotification, String customEmail) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedAtIsNull(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        Invoice.InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(newStatus);
        
        // Update relevant timestamps
        switch (newStatus) {
            case SENT:
                if (invoice.getSentAt() == null) {
                    invoice.setSentAt(LocalDateTime.now());
                }
                break;
            case PAID:
                // Payment date tracking is handled by Payment entity
                break;
        }
        
        invoice = invoiceRepository.save(invoice);
        
        // Send status update notification if requested
        if (sendNotification && invoiceEmailService.isEmailConfigured()) {
            try {
                String recipientEmail = customEmail != null ? customEmail : invoice.getBuyerEmail();
                if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                    invoiceEmailService.sendInvoiceStatusUpdateEmail(invoice, recipientEmail, oldStatus, newStatus);
                }
            } catch (Exception e) {
                System.out.println("Failed to send status update notification for invoice: " + invoice.getInvoiceNumber() + ", error: " + e.getMessage());
                // Don't throw exception to avoid breaking the status update
            }
        }
        
        return invoice;
    }
}
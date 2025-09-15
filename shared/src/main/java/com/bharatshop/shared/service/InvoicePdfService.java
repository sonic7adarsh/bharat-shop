package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Invoice;
import com.bharatshop.shared.entity.InvoiceItem;
import com.bharatshop.shared.repository.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating GST-compliant invoice PDFs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private final TemplateEngine templateEngine;
    private final InvoiceItemRepository invoiceItemRepository;
    private final QrCodeService qrCodeService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generate PDF for an invoice
     */
    public byte[] generateInvoicePdf(Invoice invoice) {
        try {
            // Get invoice items
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdAndDeletedAtIsNullOrderByIdAsc(invoice.getId());

            // Prepare template context
            Context context = createTemplateContext(invoice, items);

            // Generate HTML from template
            String html = templateEngine.process("invoice-template", context);

            // Convert HTML to PDF
            return convertHtmlToPdf(html);

        } catch (Exception e) {
            System.out.println("Error generating PDF for invoice " + invoice.getInvoiceNumber() + ": " + e.getMessage());
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    /**
     * Create Thymeleaf context with invoice data
     */
    private Context createTemplateContext(Invoice invoice, List<InvoiceItem> items) {
        Context context = new Context();

        // Invoice basic details
        context.setVariable("invoice", invoice);
        context.setVariable("items", items);
        context.setVariable("itemCount", items.size());

        // Formatted dates
        context.setVariable("invoiceDate", invoice.getInvoiceDate().format(DATE_FORMATTER));
        context.setVariable("dueDate", invoice.getDueDate().format(DATE_FORMATTER));
        
        if (invoice.getSentAt() != null) {
            context.setVariable("sentAt", invoice.getSentAt().format(DATETIME_FORMATTER));
        }

        // Tax summary by HSN code
        Map<String, TaxSummary> taxSummaryByHsn = calculateTaxSummaryByHsn(items);
        context.setVariable("taxSummaryByHsn", taxSummaryByHsn);

        // Overall tax summary
        TaxSummary overallSummary = calculateOverallTaxSummary(items);
        context.setVariable("overallSummary", overallSummary);

        // QR Code (if enabled)
        try {
            String qrCodeData = generateQrCodeData(invoice);
            String qrCodeBase64 = qrCodeService.generateQrCodeBase64(qrCodeData, 150, 150);
            context.setVariable("qrCode", qrCodeBase64);
            context.setVariable("hasQrCode", true);
        } catch (Exception e) {
            System.out.println("Failed to generate QR code for invoice " + invoice.getInvoiceNumber() + ": " + e.getMessage());
            context.setVariable("hasQrCode", false);
        }

        // Additional formatting helpers
        context.setVariable("currencySymbol", "₹");
        context.setVariable("isIntraState", invoice.getTaxType() == Invoice.TaxType.INTRA_STATE);
        context.setVariable("isInterState", invoice.getTaxType() == Invoice.TaxType.INTER_STATE);

        return context;
    }

    /**
     * Calculate tax summary grouped by HSN code
     */
    private Map<String, TaxSummary> calculateTaxSummaryByHsn(List<InvoiceItem> items) {
        Map<String, TaxSummary> summaryMap = new HashMap<>();

        for (InvoiceItem item : items) {
            String hsnCode = item.getHsnCode();
            TaxSummary summary = summaryMap.computeIfAbsent(hsnCode, k -> new TaxSummary());

            summary.addItem(item);
        }

        return summaryMap;
    }

    /**
     * Calculate overall tax summary
     */
    private TaxSummary calculateOverallTaxSummary(List<InvoiceItem> items) {
        TaxSummary summary = new TaxSummary();
        for (InvoiceItem item : items) {
            summary.addItem(item);
        }
        return summary;
    }

    /**
     * Generate QR code data for GST invoice
     */
    private String generateQrCodeData(Invoice invoice) {
        // GST QR Code format as per government guidelines
        StringBuilder qrData = new StringBuilder();
        
        // Seller GSTIN
        qrData.append("GSTIN:").append(invoice.getSellerGstin()).append("\n");
        
        // Invoice Number
        qrData.append("INV:").append(invoice.getInvoiceNumber()).append("\n");
        
        // Invoice Date
        qrData.append("DATE:").append(invoice.getInvoiceDate().format(DATE_FORMATTER)).append("\n");
        
        // Total Amount
        qrData.append("AMT:").append(invoice.getTotalAmount()).append("\n");
        
        // Tax Amount
        qrData.append("TAX:").append(invoice.getTotalTax()).append("\n");
        
        // Buyer GSTIN (if available)
        if (invoice.getBuyerGstin() != null && !invoice.getBuyerGstin().isEmpty()) {
            qrData.append("BUYER_GSTIN:").append(invoice.getBuyerGstin()).append("\n");
        }

        return qrData.toString();
    }

    /**
     * Convert HTML to PDF using Flying Saucer
     */
    private byte[] convertHtmlToPdf(String html) throws IOException, com.lowagie.text.DocumentException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Inner class for tax summary calculations
     */
    public static class TaxSummary {
        private BigDecimal netAmount = BigDecimal.ZERO;
        private BigDecimal cgstAmount = BigDecimal.ZERO;
        private BigDecimal sgstAmount = BigDecimal.ZERO;
        private BigDecimal igstAmount = BigDecimal.ZERO;
        private BigDecimal cessAmount = BigDecimal.ZERO;
        private BigDecimal totalTaxAmount = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int itemCount = 0;

        public void addItem(InvoiceItem item) {
            netAmount = netAmount.add(item.getNetAmount());
            
            if (item.getCgstAmount() != null) {
                cgstAmount = cgstAmount.add(item.getCgstAmount());
            }
            if (item.getSgstAmount() != null) {
                sgstAmount = sgstAmount.add(item.getSgstAmount());
            }
            if (item.getIgstAmount() != null) {
                igstAmount = igstAmount.add(item.getIgstAmount());
            }
            if (item.getCessAmount() != null) {
                cessAmount = cessAmount.add(item.getCessAmount());
            }
            if (item.getTotalTaxAmount() != null) {
                totalTaxAmount = totalTaxAmount.add(item.getTotalTaxAmount());
            }
            if (item.getTotalAmount() != null) {
                totalAmount = totalAmount.add(item.getTotalAmount());
            }
            
            itemCount++;
        }

        // Getters
        public BigDecimal getNetAmount() { return netAmount; }
        public BigDecimal getCgstAmount() { return cgstAmount; }
        public BigDecimal getSgstAmount() { return sgstAmount; }
        public BigDecimal getIgstAmount() { return igstAmount; }
        public BigDecimal getCessAmount() { return cessAmount; }
        public BigDecimal getTotalTaxAmount() { return totalTaxAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public int getItemCount() { return itemCount; }
        
        public boolean hasCgst() { return cgstAmount.compareTo(BigDecimal.ZERO) > 0; }
        public boolean hasSgst() { return sgstAmount.compareTo(BigDecimal.ZERO) > 0; }
        public boolean hasIgst() { return igstAmount.compareTo(BigDecimal.ZERO) > 0; }
        public boolean hasCess() { return cessAmount.compareTo(BigDecimal.ZERO) > 0; }
    }
}
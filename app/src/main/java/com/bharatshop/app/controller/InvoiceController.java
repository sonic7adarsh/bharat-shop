package com.bharatshop.app.controller;

import com.bharatshop.shared.dto.InvoiceDto;
import com.bharatshop.shared.dto.InvoiceGenerationRequest;
import com.bharatshop.shared.dto.InvoiceListResponse;
import com.bharatshop.shared.dto.ApiResponse;
import com.bharatshop.shared.entity.Invoice;
import com.bharatshop.shared.service.InvoiceService;
import com.bharatshop.shared.repository.InvoiceRepository;
import com.bharatshop.shared.service.InvoicePdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfService invoicePdfService;

    /**
     * Generate invoice from order
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<InvoiceDto>> generateInvoice(
            @Valid @RequestBody InvoiceGenerationRequest request) {
        try {
            log.info("Generating invoice for order: {}", request.getOrderId());
            
            Invoice invoice = invoiceService.generateInvoiceFromOrder(
                request.getOrderId(), 
                request.getTenantId(),
                request.isGeneratePdf(),
                request.isSendEmail(),
                request.getCustomEmailAddress(),
                request.getNotes()
            );
            
            InvoiceDto invoiceDto = InvoiceDto.fromEntity(invoice);
            
            return ResponseEntity.ok(
                ApiResponse.success(invoiceDto, "Invoice generated successfully")
            );
        } catch (Exception e) {
            log.error("Error generating invoice for order: {}", request.getOrderId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate invoice: " + e.getMessage()));
        }
    }

    /**
     * Get invoice by ID
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable Long invoiceId,
            @RequestParam Long tenantId) {
        try {
            Invoice invoice = invoiceService.getInvoice(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
            InvoiceDto invoiceDto = InvoiceDto.fromEntity(invoice);
            
            return ResponseEntity.ok(
                ApiResponse.success(invoiceDto, "Invoice retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving invoice: {}", invoiceId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Invoice not found: " + e.getMessage()));
        }
    }

    /**
     * Get invoice by invoice number
     */
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceByNumber(
            @PathVariable String invoiceNumber,
            @RequestParam Long tenantId) {
        try {
            Invoice invoice = invoiceRepository.findByInvoiceNumberAndTenantIdAndDeletedAtIsNull(invoiceNumber, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
            InvoiceDto invoiceDto = InvoiceDto.fromEntity(invoice);
            
            return ResponseEntity.ok(
                ApiResponse.success(invoiceDto, "Invoice retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving invoice by number: {}", invoiceNumber, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Invoice not found: " + e.getMessage()));
        }
    }

    /**
     * Get invoices for an order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesByOrder(
            @PathVariable Long orderId,
            @RequestParam Long tenantId) {
        try {
            Optional<Invoice> invoiceOpt = invoiceService.getInvoiceByOrderId(orderId, tenantId);
            List<Invoice> invoices = invoiceOpt.map(List::of).orElse(List.of());
            List<InvoiceDto> invoiceDtos = invoices.stream()
                .map(InvoiceDto::fromEntity)
                .toList();
            
            return ResponseEntity.ok(
                ApiResponse.success(invoiceDtos, "Invoices retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving invoices for order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve invoices: " + e.getMessage()));
        }
    }

    /**
     * List invoices with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<ApiResponse<InvoiceListResponse>> listInvoices(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String buyerEmail,
            @RequestParam(required = false) String buyerGstin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
            LocalDateTime toDateTime = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;
            
            // TODO: Implement getInvoices method in InvoiceService
            List<Invoice> allInvoices = invoiceRepository.findByTenantIdAndDeletedAtIsNullOrderByInvoiceDateDesc(tenantId);
            Page<Invoice> invoicePage = new PageImpl<>(allInvoices.subList(
                Math.min(page * size, allInvoices.size()),
                Math.min((page + 1) * size, allInvoices.size())
            ), pageable, allInvoices.size());
            
            List<InvoiceDto> invoiceDtos = invoicePage.getContent().stream()
                .map(InvoiceDto::fromEntity)
                .toList();
            
            InvoiceListResponse response = InvoiceListResponse.builder()
                .invoices(invoiceDtos)
                .totalElements(invoicePage.getTotalElements())
                .totalPages(invoicePage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
            
            return ResponseEntity.ok(
                ApiResponse.success(response, "Invoices retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error listing invoices for tenant: {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve invoices: " + e.getMessage()));
        }
    }

    /**
     * Download invoice PDF
     */
    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long invoiceId,
            @RequestParam Long tenantId) {
        try {
            Invoice invoice = invoiceService.getInvoice(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
            byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoice);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                "attachment", 
                "invoice-" + invoice.getInvoiceNumber() + ".pdf"
            );
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating PDF for invoice: {}", invoiceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    /**
     * Regenerate invoice PDF
     */
    @PostMapping("/{invoiceId}/regenerate-pdf")
    public ResponseEntity<ApiResponse<InvoiceDto>> regenerateInvoicePdf(
            @PathVariable Long invoiceId,
            @RequestParam Long tenantId) {
        try {
            Invoice invoice = invoiceService.getInvoice(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
            invoiceService.generateAndSavePdf(invoice);
            InvoiceDto invoiceDto = InvoiceDto.fromEntity(invoice);
            
            return ResponseEntity.ok(
                ApiResponse.success(invoiceDto, "Invoice PDF regenerated successfully")
            );
        } catch (Exception e) {
            log.error("Error regenerating PDF for invoice: {}", invoiceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to regenerate PDF: " + e.getMessage()));
        }
    }

    /**
     * Resend invoice email
     */
    @PostMapping("/{invoiceId}/resend-email")
    public ResponseEntity<ApiResponse<String>> resendInvoiceEmail(
            @PathVariable Long invoiceId,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String customEmail,
            @RequestParam(required = false) String customMessage) {
        try {
            log.info("Resending invoice email for invoice: {}", invoiceId);
            
            invoiceService.resendInvoiceEmail(invoiceId, tenantId, customEmail, customMessage);
            
            return ResponseEntity.ok(
                ApiResponse.success("Email sent successfully", "Invoice email resent successfully")
            );
        } catch (Exception e) {
            log.error("Error resending invoice email for invoice: {}", invoiceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to resend invoice email: " + e.getMessage()));
        }
    }

    /**
     * Update invoice status
     */
    @PutMapping("/{invoiceId}/status")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoiceStatus(
            @PathVariable Long invoiceId,
            @RequestParam Long tenantId,
            @RequestParam String status,
            @RequestParam(defaultValue = "false") boolean sendNotification,
            @RequestParam(required = false) String customEmail) {
        try {
            log.info("Updating status for invoice: {} to: {}", invoiceId, status);
            
            Invoice.InvoiceStatus newStatus = Invoice.InvoiceStatus.valueOf(status.toUpperCase());
            
            Invoice updatedInvoice = invoiceService.updateInvoiceStatus(
                invoiceId, tenantId, newStatus, sendNotification, customEmail
            );
            
            InvoiceDto invoiceDto = InvoiceDto.fromEntity(updatedInvoice);
            
            return ResponseEntity.ok(
                ApiResponse.success(invoiceDto, "Invoice status updated successfully")
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid status provided for invoice: {}", invoiceId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid status: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating status for invoice: {}", invoiceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update invoice status: " + e.getMessage()));
        }
    }

    /**
     * Get invoice statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoiceStats(
            @RequestParam Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
            LocalDateTime toDateTime = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;
            
            // TODO: Implement invoice statistics method
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalInvoices", 0);
            stats.put("totalAmount", BigDecimal.ZERO);
            stats.put("paidAmount", BigDecimal.ZERO);
            stats.put("pendingAmount", BigDecimal.ZERO);
            
            return ResponseEntity.ok(
                ApiResponse.success(stats, "Invoice statistics retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving invoice statistics for tenant: {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve invoice statistics: " + e.getMessage()));
        }
    }

    /**
     * Send payment reminder
     */
    @PostMapping("/{invoiceId}/payment-reminder")
    public ResponseEntity<ApiResponse<String>> sendPaymentReminder(
            @PathVariable Long invoiceId,
            @RequestParam Long tenantId,
            @RequestParam(required = false) String customEmail) {
        try {
            log.info("Sending payment reminder for invoice: {}", invoiceId);
            
            invoiceService.sendPaymentReminder(invoiceId, tenantId, customEmail);
            
            return ResponseEntity.ok(
                ApiResponse.success("Reminder sent successfully", "Payment reminder sent successfully")
            );
        } catch (Exception e) {
            log.error("Error sending payment reminder for invoice: {}", invoiceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to send payment reminder: " + e.getMessage()));
        }
    }
}
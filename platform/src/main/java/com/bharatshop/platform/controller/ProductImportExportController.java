package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.ProductImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/products")
@RequiredArgsConstructor
public class ProductImportExportController {

    private final ProductImportExportService importExportService;

    @GetMapping("/export/csv")
    public ResponseEntity<Resource> exportProductsToCSV(Authentication authentication) {
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            byte[] csvData = importExportService.exportProductsToCSVFile(tenantId);
            
            String filename = "products_export_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            ByteArrayResource resource = new ByteArrayResource(csvData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(csvData.length)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import/csv")
    public ResponseEntity<Map<String, Object>> importProductsFromCSV(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "File is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            if (!isCSVFile(file)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "File must be a CSV file");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Check file size (limit to 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "File size exceeds 10MB limit");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            UUID tenantId = getTenantIdFromAuth(authentication);
            ProductImportExportService.ImportResult result = 
                    importExportService.importProductsFromCSV(file, tenantId);
            
            Map<String, Object> response = result.toMap();
            response.put("filename", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/import/csv/content")
    public ResponseEntity<Map<String, Object>> importProductsFromCSVContent(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        try {
            String csvContent = request.get("csvContent");
            
            if (csvContent == null || csvContent.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "CSV content is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            UUID tenantId = getTenantIdFromAuth(authentication);
            ProductImportExportService.ImportResult result = 
                    importExportService.importProductsFromCSVContent(csvContent, tenantId);
            
            Map<String, Object> response = result.toMap();
            
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing CSV content: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/import/template")
    public ResponseEntity<Resource> downloadCSVTemplate() {
        try {
            String templateContent = importExportService.generateCSVTemplate();
            byte[] templateData = templateContent.getBytes();
            
            String filename = "product_import_template.csv";
            ByteArrayResource resource = new ByteArrayResource(templateData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(templateData.length)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/import/template/content")
    public ResponseEntity<Map<String, Object>> getCSVTemplateContent() {
        try {
            String templateContent = importExportService.generateCSVTemplate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("template", templateContent);
            response.put("instructions", getImportInstructions());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating template: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/validate/csv")
    public ResponseEntity<Map<String, Object>> validateCSVFile(
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> validation = new HashMap<>();
        
        try {
            // Basic file validation
            if (file.isEmpty()) {
                validation.put("valid", false);
                validation.put("error", "File is empty");
                return ResponseEntity.ok(validation);
            }
            
            if (!isCSVFile(file)) {
                validation.put("valid", false);
                validation.put("error", "File must be a CSV file");
                return ResponseEntity.ok(validation);
            }
            
            if (file.getSize() > 10 * 1024 * 1024) {
                validation.put("valid", false);
                validation.put("error", "File size exceeds 10MB limit");
                return ResponseEntity.ok(validation);
            }
            
            // Additional validation can be added here
            validation.put("valid", true);
            validation.put("filename", file.getOriginalFilename());
            validation.put("fileSize", file.getSize());
            validation.put("contentType", file.getContentType());
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            validation.put("valid", false);
            validation.put("error", "Error validating file: " + e.getMessage());
            return ResponseEntity.ok(validation);
        }
    }

    private boolean isCSVFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        return (contentType != null && 
                (contentType.equals("text/csv") || 
                 contentType.equals("application/csv") ||
                 contentType.equals("text/plain"))) ||
               (filename != null && filename.toLowerCase().endsWith(".csv"));
    }

    private Map<String, Object> getImportInstructions() {
        Map<String, Object> instructions = new HashMap<>();
        
        instructions.put("format", "CSV (Comma Separated Values)");
        instructions.put("encoding", "UTF-8");
        instructions.put("maxFileSize", "10MB");
        
        Map<String, String> columns = new HashMap<>();
        columns.put("name", "Product name (required)");
        columns.put("slug", "URL-friendly identifier (required, unique)");
        columns.put("description", "Product description (optional)");
        columns.put("price", "Product price in decimal format (optional)");
        columns.put("stock", "Stock quantity as integer (optional)");
        columns.put("status", "Product status: ACTIVE, DRAFT, INACTIVE, OUT_OF_STOCK (optional, defaults to DRAFT)");
        columns.put("attributes", "JSON object with product attributes (optional)");
        
        instructions.put("columns", columns);
        
        Map<String, String> notes = new HashMap<>();
        notes.put("quotes", "Use double quotes for values containing commas or quotes");
        notes.put("escaping", "Escape quotes within quoted values by doubling them");
        notes.put("attributes", "Attributes should be valid JSON format");
        notes.put("duplicates", "Products with duplicate slugs will be rejected");
        
        instructions.put("notes", notes);
        
        return instructions;
    }

    private UUID getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return UUID.fromString("00000000-0000-0000-0000-000000000001"); // For now, return a default tenant ID
    }
}
package com.bharatshop.platform.service;

import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportExportService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    private static final String CSV_HEADER = "name,slug,description,price,stock,status,attributes";
    private static final String CSV_SEPARATOR = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export products to CSV format
     */
    public String exportProductsToCSV(UUID tenantId) {
        List<Product> products = productRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        
        StringBuilder csvContent = new StringBuilder();
        csvContent.append(CSV_HEADER).append("\n");
        
        for (Product product : products) {
            csvContent.append(productToCsvRow(product)).append("\n");
        }
        
        return csvContent.toString();
    }

    /**
     * Export products to CSV file as byte array
     */
    public byte[] exportProductsToCSVFile(UUID tenantId) {
        String csvContent = exportProductsToCSV(tenantId);
        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Import products from CSV file
     */
    public ImportResult importProductsFromCSV(MultipartFile file, UUID tenantId) {
        ImportResult result = new ImportResult();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null || !isValidHeader(headerLine)) {
                result.addError("Invalid CSV header. Expected: " + CSV_HEADER);
                return result;
            }
            
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    Product product = csvRowToProduct(line, tenantId);
                    Product savedProduct = productService.createProduct(product, tenantId);
                    result.addSuccess(savedProduct.getName());
                } catch (Exception e) {
                    result.addError("Line " + lineNumber + ": " + e.getMessage());
                    log.error("Error importing product at line {}: {}", lineNumber, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            result.addError("Error reading CSV file: " + e.getMessage());
            log.error("Error reading CSV file", e);
        }
        
        return result;
    }

    /**
     * Import products from CSV string content
     */
    public ImportResult importProductsFromCSVContent(String csvContent, UUID tenantId) {
        ImportResult result = new ImportResult();
        
        String[] lines = csvContent.split("\n");
        
        if (lines.length == 0 || !isValidHeader(lines[0])) {
            result.addError("Invalid CSV header. Expected: " + CSV_HEADER);
            return result;
        }
        
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            try {
                Product product = csvRowToProduct(line, tenantId);
                Product savedProduct = productService.createProduct(product, tenantId);
                result.addSuccess(savedProduct.getName());
            } catch (Exception e) {
                result.addError("Line " + (i + 1) + ": " + e.getMessage());
                log.error("Error importing product at line {}: {}", i + 1, e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * Generate CSV template for product import
     */
    public String generateCSVTemplate() {
        StringBuilder template = new StringBuilder();
        template.append(CSV_HEADER).append("\n");
        template.append("Sample Product,sample-product,This is a sample product description,29.99,100,ACTIVE,\"{\"color\":\"red\",\"size\":\"M\"}\"").append("\n");
        template.append("Another Product,another-product,Another sample description,49.99,50,DRAFT,\"{\"material\":\"cotton\"}\"");
        
        return template.toString();
    }

    private String productToCsvRow(Product product) {
        StringBuilder row = new StringBuilder();
        
        row.append(escapeCsvValue(product.getName())).append(CSV_SEPARATOR);
        row.append(escapeCsvValue(product.getSlug())).append(CSV_SEPARATOR);
        row.append(escapeCsvValue(product.getDescription())).append(CSV_SEPARATOR);
        row.append(product.getPrice() != null ? product.getPrice().toString() : "").append(CSV_SEPARATOR);
        row.append(product.getStock() != null ? product.getStock().toString() : "").append(CSV_SEPARATOR);
        row.append(product.getStatus() != null ? product.getStatus().toString() : "").append(CSV_SEPARATOR);
        
        // Convert attributes map to JSON string
        String attributesJson = "";
        if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
            try {
                attributesJson = objectMapper.writeValueAsString(product.getAttributes());
            } catch (JsonProcessingException e) {
                log.warn("Error serializing product attributes for product {}: {}", product.getId(), e.getMessage());
            }
        }
        row.append(escapeCsvValue(attributesJson));
        
        return row.toString();
    }

    private Product csvRowToProduct(String csvRow, UUID tenantId) {
        String[] values = parseCsvRow(csvRow);
        
        if (values.length < 7) {
            throw new IllegalArgumentException("Invalid CSV row format. Expected 7 columns, got " + values.length);
        }
        
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(values[0].trim());
        product.setSlug(values[1].trim());
        product.setDescription(values[2].trim());
        
        // Parse price
        if (!values[3].trim().isEmpty()) {
            try {
                product.setPrice(new BigDecimal(values[3].trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid price format: " + values[3]);
            }
        }
        
        // Parse stock
        if (!values[4].trim().isEmpty()) {
            try {
                product.setStock(Integer.parseInt(values[4].trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid stock format: " + values[4]);
            }
        }
        
        // Parse status
        if (!values[5].trim().isEmpty()) {
            try {
                product.setStatus(Product.ProductStatus.valueOf(values[5].trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + values[5]);
            }
        } else {
            product.setStatus(Product.ProductStatus.DRAFT);
        }
        
        // Parse attributes JSON
        if (!values[6].trim().isEmpty()) {
            try {
                // Validate JSON format by parsing it
                objectMapper.readValue(values[6].trim(), Map.class);
                // Set the raw JSON string
                product.setAttributes(values[6].trim());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid attributes JSON format: " + e.getMessage());
            }
        }
        
        return product;
    }

    private String[] parseCsvRow(String csvRow) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < csvRow.length(); i++) {
            char c = csvRow.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < csvRow.length() && csvRow.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentValue.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        
        values.add(currentValue.toString());
        return values.toArray(new String[0]);
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }

    private boolean isValidHeader(String header) {
        return CSV_HEADER.equals(header.trim());
    }

    /**
     * Result class for import operations
     */
    public static class ImportResult {
        private final List<String> successfulImports = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public void addSuccess(String productName) {
            successfulImports.add(productName);
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public List<String> getSuccessfulImports() {
            return new ArrayList<>(successfulImports);
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public int getSuccessCount() {
            return successfulImports.size();
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean isSuccessful() {
            return !successfulImports.isEmpty() && errors.isEmpty();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", getSuccessCount());
            result.put("errorCount", getErrorCount());
            result.put("successfulImports", getSuccessfulImports());
            result.put("errors", getErrors());
            result.put("hasErrors", hasErrors());
            result.put("isSuccessful", isSuccessful());
            return result;
        }
    }
}
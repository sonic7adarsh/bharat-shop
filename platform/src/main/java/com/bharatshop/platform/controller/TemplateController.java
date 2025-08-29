package com.bharatshop.platform.controller;

import com.bharatshop.platform.shared.ApiResponse;
import com.bharatshop.shared.entity.Template;
import com.bharatshop.shared.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing templates in the platform.
 * Provides APIs for vendors to select and manage templates.
 */
@RestController
@RequestMapping("/api/platform/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Template Management", description = "APIs for template management and selection")
public class TemplateController {
    
    private final TemplateService templateService;
    
    /**
     * Get all active templates
     * GET /api/platform/templates
     */
    @GetMapping
    @Operation(summary = "Get all templates", description = "Retrieve all active templates with pagination")
    public ResponseEntity<ApiResponse<Page<Template>>> getAllTemplates(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, 
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
            
            Page<Template> templates = templateService.getAllTemplates(pageable);
            
            return ResponseEntity.ok(ApiResponse.success(templates));
            
        } catch (Exception e) {
            log.error("Error fetching templates", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching templates: " + e.getMessage()));
        }
    }
    
    /**
     * Get template by ID
     * GET /api/platform/templates/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieve a specific template by its ID")
    public ResponseEntity<ApiResponse<Template>> getTemplateById(
            @Parameter(description = "Template ID")
            @PathVariable UUID id) {
        
        try {
            return templateService.getTemplateById(id)
                    .map(template -> ResponseEntity.ok(ApiResponse.success(template)))
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (Exception e) {
            log.error("Error fetching template by ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching template: " + e.getMessage()));
        }
    }
    
    /**
     * Get templates by category
     * GET /api/platform/templates/category/{category}
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get templates by category", description = "Retrieve templates filtered by category")
    public ResponseEntity<ApiResponse<List<Template>>> getTemplatesByCategory(
            @Parameter(description = "Template category")
            @PathVariable String category) {
        
        try {
            List<Template> templates = templateService.getTemplatesByCategory(category);
            return ResponseEntity.ok(ApiResponse.success(templates));
            
        } catch (Exception e) {
            log.error("Error fetching templates by category: {}", category, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching templates: " + e.getMessage()));
        }
    }
    
    /**
     * Get all template categories
     * GET /api/platform/templates/categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Get template categories", description = "Retrieve all available template categories")
    public ResponseEntity<ApiResponse<List<String>>> getTemplateCategories() {
        
        try {
            List<String> categories = templateService.getAllCategories();
            return ResponseEntity.ok(ApiResponse.success(categories));
            
        } catch (Exception e) {
            log.error("Error fetching template categories", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching categories: " + e.getMessage()));
        }
    }
    
    /**
     * Search templates
     * GET /api/platform/templates/search
     */
    @GetMapping("/search")
    @Operation(summary = "Search templates", description = "Search templates by name or description")
    public ResponseEntity<ApiResponse<List<Template>>> searchTemplates(
            @Parameter(description = "Search query")
            @RequestParam String query) {
        
        try {
            List<Template> templates = templateService.searchTemplates(query);
            return ResponseEntity.ok(ApiResponse.success(templates));
            
        } catch (Exception e) {
            log.error("Error searching templates with query: {}", query, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error searching templates: " + e.getMessage()));
        }
    }
    
    /**
     * Create new template
     * POST /api/platform/templates
     */
    @PostMapping
    @Operation(summary = "Create template", description = "Create a new template")
    public ResponseEntity<ApiResponse<Template>> createTemplate(
            @Parameter(description = "Template data")
            @Valid @RequestBody Template template) {
        
        try {
            Template createdTemplate = templateService.createTemplate(template);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdTemplate));
                    
        } catch (IllegalArgumentException e) {
            log.warn("Invalid template data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error creating template", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error creating template: " + e.getMessage()));
        }
    }
    
    /**
     * Update existing template
     * PUT /api/platform/templates/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Update an existing template")
    public ResponseEntity<ApiResponse<Template>> updateTemplate(
            @Parameter(description = "Template ID")
            @PathVariable UUID id,
            
            @Parameter(description = "Updated template data")
            @Valid @RequestBody Template template) {
        
        try {
            Template updatedTemplate = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(ApiResponse.success(updatedTemplate));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid template data or ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error updating template with ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error updating template: " + e.getMessage()));
        }
    }
    
    /**
     * Toggle template status (activate/deactivate)
     * PATCH /api/platform/templates/{id}/toggle-status
     */
    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle template status", description = "Activate or deactivate a template")
    public ResponseEntity<ApiResponse<Template>> toggleTemplateStatus(
            @Parameter(description = "Template ID")
            @PathVariable UUID id) {
        
        try {
            Template template = templateService.toggleTemplateStatus(id);
            return ResponseEntity.ok(ApiResponse.success(template));
            
        } catch (IllegalArgumentException e) {
            log.warn("Template not found with ID: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error toggling template status for ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error toggling template status: " + e.getMessage()));
        }
    }
    
    /**
     * Delete template
     * DELETE /api/platform/templates/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Soft delete a template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @Parameter(description = "Template ID")
            @PathVariable UUID id) {
        
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (IllegalArgumentException e) {
            log.warn("Template not found with ID: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting template with ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting template: " + e.getMessage()));
        }
    }
}
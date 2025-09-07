package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Template;
import com.bharatshop.shared.repository.TemplateRepository;
import com.bharatshop.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


/**
 * Service for managing templates.
 * Handles template CRUD operations, selection, and configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
    
    private final TemplateRepository templateRepository;
    
    /**
     * Get all active templates
     */
    @Transactional(readOnly = true)
    public List<Template> getAllActiveTemplates() {
        String tenantId = TenantContext.requireCurrentTenant();
        return templateRepository.findAllActiveTemplates(Long.parseLong(tenantId));
    }
    
    /**
     * Get all templates with pagination
     */
    @Transactional(readOnly = true)
    public Page<Template> getAllTemplates(Pageable pageable) {
        return templateRepository.findAllActive(pageable);
    }
    
    /**
     * Get template by ID
     */
    @Transactional(readOnly = true)
    public Optional<Template> getTemplateById(Long id) {
        String tenantId = TenantContext.requireCurrentTenant();
        return templateRepository.findActiveById(id);
    }
    
    /**
     * Get template by name
     */
    @Transactional(readOnly = true)
    public Optional<Template> getTemplateByName(String name) {
        String tenantId = TenantContext.requireCurrentTenant();
        return templateRepository.findActiveByName(name, Long.parseLong(tenantId));
    }
    
    /**
     * Get templates by category
     */
    @Transactional(readOnly = true)
    public List<Template> getTemplatesByCategory(String category) {
        String tenantId = TenantContext.requireCurrentTenant();
        return templateRepository.findActiveByCategory(category, Long.parseLong(tenantId));
    }
    
    /**
     * Get all template categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        String tenantId = TenantContext.requireCurrentTenant();
        return templateRepository.findAllCategories(Long.parseLong(tenantId));
    }
    
    /**
     * Search templates
     */
    @Transactional(readOnly = true)
    public List<Template> searchTemplates(String search) {
        String tenantId = TenantContext.requireCurrentTenant();
        return templateRepository.searchActiveTemplates(search, Long.parseLong(tenantId));
    }
    
    /**
     * Create new template
     */
    public Template createTemplate(Template template) {
        validateTemplate(template);
        
        String tenantId = TenantContext.requireCurrentTenant();
        if (templateRepository.existsByNameAndTenantId(template.getName(), Long.parseLong(tenantId))) {
            throw new IllegalArgumentException("Template with name '" + template.getName() + "' already exists");
        }
        
        log.info("Creating new template: {}", template.getName());
        return templateRepository.save(template);
    }
    
    /**
     * Update existing template
     */
    public Template updateTemplate(Long id, Template templateUpdate) {
        String tenantId = TenantContext.requireCurrentTenant();
        Template existingTemplate = templateRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
        
        validateTemplate(templateUpdate);
        
        // Check if name is being changed and if new name already exists
        if (!existingTemplate.getName().equals(templateUpdate.getName()) &&
            templateRepository.existsByNameAndTenantIdAndIdNot(templateUpdate.getName(), id, Long.parseLong(tenantId))) {
            throw new IllegalArgumentException("Template with name '" + templateUpdate.getName() + "' already exists");
        }
        
        // Update fields
        existingTemplate.setName(templateUpdate.getName());
        existingTemplate.setConfig(templateUpdate.getConfig());
        existingTemplate.setDescription(templateUpdate.getDescription());
        existingTemplate.setPreviewImage(templateUpdate.getPreviewImage());
        existingTemplate.setCategory(templateUpdate.getCategory());
        existingTemplate.setSortOrder(templateUpdate.getSortOrder());
        existingTemplate.setIsActive(templateUpdate.getIsActive());
        
        log.info("Updated template: {}", existingTemplate.getName());
        return templateRepository.save(existingTemplate);
    }
    
    /**
     * Delete template (soft delete)
     */
    public void deleteTemplate(Long id) {
        String tenantId = TenantContext.requireCurrentTenant();
        Template template = templateRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
        
        template.softDelete();
        templateRepository.save(template);
        
        log.info("Deleted template: {}", template.getName());
    }
    
    /**
     * Activate/Deactivate template
     */
    public Template toggleTemplateStatus(Long id) {
        String tenantId = TenantContext.requireCurrentTenant();
        Template template = templateRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
        
        template.setIsActive(!template.getIsActive());
        
        log.info("Toggled template status for: {} to {}", template.getName(), template.getIsActive());
        return templateRepository.save(template);
    }
    
    /**
     * Validate template data
     */
    private void validateTemplate(Template template) {
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Template name is required");
        }
        
        if (template.getConfig() == null || template.getConfig().trim().isEmpty()) {
            throw new IllegalArgumentException("Template configuration is required");
        }
        
        // Validate JSON format if needed
        try {
            // Basic JSON validation - you might want to use a proper JSON parser
            if (!isValidJson(template.getConfig())) {
                throw new IllegalArgumentException("Invalid JSON format in template configuration");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format in template configuration: " + e.getMessage());
        }
    }
    
    /**
     * Basic JSON validation
     */
    private boolean isValidJson(String json) {
        // Basic validation - starts with { or [ and ends with } or ]
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) || 
               (json.startsWith("[") && json.endsWith("]"));
    }
}
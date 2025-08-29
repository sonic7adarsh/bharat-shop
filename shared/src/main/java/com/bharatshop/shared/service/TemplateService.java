package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Template;
import com.bharatshop.shared.repository.TemplateRepository;
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
import java.util.UUID;

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
        return templateRepository.findAllActiveTemplates();
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
    public Optional<Template> getTemplateById(UUID id) {
        return templateRepository.findActiveById(id);
    }
    
    /**
     * Get template by name
     */
    @Transactional(readOnly = true)
    public Optional<Template> getTemplateByName(String name) {
        return templateRepository.findActiveByName(name);
    }
    
    /**
     * Get templates by category
     */
    @Transactional(readOnly = true)
    public List<Template> getTemplatesByCategory(String category) {
        return templateRepository.findActiveByCategory(category);
    }
    
    /**
     * Get all template categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return templateRepository.findAllCategories();
    }
    
    /**
     * Search templates
     */
    @Transactional(readOnly = true)
    public List<Template> searchTemplates(String search) {
        return templateRepository.searchActiveTemplates(search);
    }
    
    /**
     * Create new template
     */
    public Template createTemplate(Template template) {
        validateTemplate(template);
        
        if (templateRepository.existsByNameAndTenantId(template.getName())) {
            throw new IllegalArgumentException("Template with name '" + template.getName() + "' already exists");
        }
        
        log.info("Creating new template: {}", template.getName());
        return templateRepository.save(template);
    }
    
    /**
     * Update existing template
     */
    public Template updateTemplate(UUID id, Template templateUpdate) {
        Template existingTemplate = templateRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
        
        validateTemplate(templateUpdate);
        
        // Check if name is being changed and if new name already exists
        if (!existingTemplate.getName().equals(templateUpdate.getName()) &&
            templateRepository.existsByNameAndTenantIdAndIdNot(templateUpdate.getName(), id)) {
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
    public void deleteTemplate(UUID id) {
        Template template = templateRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
        
        template.softDelete();
        templateRepository.save(template);
        
        log.info("Deleted template: {}", template.getName());
    }
    
    /**
     * Activate/Deactivate template
     */
    public Template toggleTemplateStatus(UUID id) {
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
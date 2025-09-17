package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.NotificationTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for rendering notification templates with dynamic variables.
 * Supports variable substitution using {{variableName}} syntax.
 */
@Service
@Slf4j
public class TemplateRenderingService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TemplateRenderingService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * Render template subject with provided variables
     */
    public String renderSubject(NotificationTemplate template, Map<String, Object> variables) {
        return renderTemplate(template.getSubject(), variables);
    }
    
    /**
     * Render template body with provided variables
     */
    public String renderBody(NotificationTemplate template, Map<String, Object> variables) {
        return renderTemplate(template.getBody(), variables);
    }
    
    /**
     * Render HTML body with provided variables (if available)
     */
    public String renderHtmlBody(NotificationTemplate template, Map<String, Object> variables) {
        if (template.getHtmlBody() == null || template.getHtmlBody().trim().isEmpty()) {
            return null;
        }
        return renderTemplate(template.getHtmlBody(), variables);
    }
    
    /**
     * Render a template string with provided variables
     */
    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.trim().isEmpty()) {
            return template;
        }
        
        if (variables == null || variables.isEmpty()) {
            log.warn("No variables provided for template rendering");
            return template;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = getVariableValue(variables, variableName);
            String replacement = value != null ? value.toString() : "";
            
            // Escape special regex characters in replacement
            replacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(result, replacement);
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Get variable value with support for nested properties using dot notation
     */
    private Object getVariableValue(Map<String, Object> variables, String variableName) {
        if (variableName.contains(".")) {
            // Handle nested properties like "customer.name"
            String[] parts = variableName.split("\\.", 2);
            String rootKey = parts[0];
            String nestedKey = parts[1];
            
            Object rootValue = variables.get(rootKey);
            if (rootValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) rootValue;
                return getVariableValue(nestedMap, nestedKey);
            }
            
            log.warn("Cannot resolve nested variable: {} (root value is not a map)", variableName);
            return null;
        }
        
        Object value = variables.get(variableName);
        if (value == null) {
            log.warn("Variable '{}' not found in template variables", variableName);
        }
        
        return value;
    }
    
    /**
     * Validate that all required variables are present in the template
     */
    public boolean validateTemplate(NotificationTemplate template, Map<String, Object> variables) {
        boolean isValid = true;
        
        // Check subject
        if (!validateTemplateString(template.getSubject(), variables)) {
            log.error("Missing variables in template subject for template ID: {}", template.getId());
            isValid = false;
        }
        
        // Check body
        if (!validateTemplateString(template.getBody(), variables)) {
            log.error("Missing variables in template body for template ID: {}", template.getId());
            isValid = false;
        }
        
        // Check HTML body if present
        if (template.getHtmlBody() != null && !template.getHtmlBody().trim().isEmpty()) {
            if (!validateTemplateString(template.getHtmlBody(), variables)) {
                log.error("Missing variables in template HTML body for template ID: {}", template.getId());
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    /**
     * Validate that all variables in a template string are available
     */
    private boolean validateTemplateString(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return true;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            if (getVariableValue(variables, variableName) == null) {
                return false;
            }
        }
        
        return true;
    }
}
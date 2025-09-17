package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing notification templates with tenant and locale support.
 * Templates contain placeholders that can be replaced with dynamic values.
 */
@Entity
@Table(name = "notification_templates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "event_type", "channel", "locale"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @NotBlank
    @Column(name = "event_type", nullable = false)
    private String eventType; // OrderPlaced, PaymentCaptured, etc.
    
    @NotBlank
    @Column(name = "channel", nullable = false)
    private String channel; // EMAIL, SMS, WHATSAPP
    
    @NotBlank
    @Column(name = "locale", nullable = false)
    private String locale; // en_US, es_ES, fr_FR, etc.
    
    @NotBlank
    @Column(name = "name", nullable = false)
    private String name; // Human-readable template name
    
    @Column(name = "description")
    private String description;
    
    @NotBlank
    @Column(name = "subject", nullable = false)
    private String subject; // For email/SMS subject line
    
    @NotBlank
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body; // Template body with placeholders
    
    // Manual getId method for compilation compatibility
    public Long getId() {
        return this.id;
    }
    
    // Manual getHtmlBody method for compilation compatibility
    public String getHtmlBody() {
        return this.htmlBody;
    }
    
    // Manual getBody method for compilation compatibility
    public String getBody() {
        return this.body;
    }
    
    // Manual getSubject method for compilation compatibility
    public String getSubject() {
        return this.subject;
    }
    
    // Manual setter methods for compilation compatibility
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }
    
    // Manual getter methods for compilation compatibility
    public String getTenantId() {
        return this.tenantId;
    }
    
    public String getEventType() {
        return this.eventType;
    }
    
    public String getChannel() {
        return this.channel;
    }
    
    public String getLocale() {
        return this.locale;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsActive() {
        return this.isActive;
    }
    
    @Column(name = "html_body", columnDefinition = "TEXT")
    private String htmlBody; // HTML version for emails
    
    @ElementCollection
    @CollectionTable(name = "template_variables", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "variable_name")
    @Column(name = "variable_description")
    private Map<String, String> availableVariables; // Variable name -> description
    
    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
}
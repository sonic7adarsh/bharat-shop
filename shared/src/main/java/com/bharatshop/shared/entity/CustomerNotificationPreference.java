package com.bharatshop.shared.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

/**
 * Entity representing customer notification preferences.
 * Stores per-customer, per-tenant notification settings.
 */
@Entity
@Table(name = "customer_notification_preferences", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tenant_id", "customer_id", "event_type", "channel"})
       },
       indexes = {
           @Index(name = "idx_customer_prefs_tenant_customer", columnList = "tenant_id, customer_id"),
           @Index(name = "idx_customer_prefs_event_channel", columnList = "event_type, channel")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNotificationPreference {
    
    public static CustomerNotificationPreferenceBuilder builder() {
        return new CustomerNotificationPreferenceBuilder();
    }
    
    public static class CustomerNotificationPreferenceBuilder {
        private String tenantId;
        private String customerId;
        private String eventType;
        private String channel;
        private Boolean enabled;
        private Boolean isVerified;
        private String verificationToken;
        private Instant verifiedAt;
        private String timezone;
        private String createdBy;
        private String updatedBy;
        
        public CustomerNotificationPreferenceBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public CustomerNotificationPreferenceBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public CustomerNotificationPreferenceBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public CustomerNotificationPreferenceBuilder channel(String channel) {
            this.channel = channel;
            return this;
        }
        
        public CustomerNotificationPreferenceBuilder enabled(Boolean enabled) {
             this.enabled = enabled;
             return this;
         }
         
         public CustomerNotificationPreferenceBuilder isEnabled(boolean enabled) {
             this.enabled = enabled;
             return this;
         }
        
        public CustomerNotificationPreferenceBuilder isVerified(Boolean isVerified) {
            this.isVerified = isVerified;
            return this;
        }
        
        public CustomerNotificationPreferenceBuilder verificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
            return this;
        }
        
        public CustomerNotificationPreferenceBuilder verifiedAt(Instant verifiedAt) {
             this.verifiedAt = verifiedAt;
             return this;
         }
         
         public CustomerNotificationPreferenceBuilder frequency(NotificationFrequency frequency) {
             // This method can be added if needed
             return this;
         }
         
         public CustomerNotificationPreferenceBuilder locale(String locale) {
             // This method can be added if needed
             return this;
         }
         
         public CustomerNotificationPreferenceBuilder timezone(String timezone) {
              this.timezone = timezone;
              return this;
          }
          
          public CustomerNotificationPreferenceBuilder createdBy(String createdBy) {
              // This method can be added if needed
              return this;
          }
          
          public CustomerNotificationPreferenceBuilder updatedBy(String updatedBy) {
              this.updatedBy = updatedBy;
              return this;
          }
         
         public CustomerNotificationPreference build() {
            CustomerNotificationPreference preference = new CustomerNotificationPreference();
            preference.setTenantId(this.tenantId);
            preference.setCustomerId(this.customerId);
            preference.setEventType(this.eventType);
            preference.setChannel(this.channel);
            preference.setIsEnabled(this.enabled);
            preference.setIsVerified(this.isVerified);
            preference.setVerificationToken(this.verificationToken);
            preference.setVerifiedAt(this.verifiedAt);
            preference.setTimezone(this.timezone);
            preference.setUpdatedBy(this.updatedBy);
            return preference;
        }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;
    
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // OrderPlaced, PaymentCaptured, etc.
    
    @Column(name = "channel", nullable = false, length = 50)
    private String channel; // EMAIL, SMS, WHATSAPP
    
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
    
    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en_US";
    
    @Column(name = "frequency", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationFrequency frequency = NotificationFrequency.IMMEDIATE;
    
    @Column(name = "quiet_hours_start", length = 5)
    private String quietHoursStart; // Format: "22:00"
    
    @Column(name = "quiet_hours_end", length = 5)
    private String quietHoursEnd; // Format: "08:00"
    
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";
    
    @ElementCollection
    @CollectionTable(name = "customer_notification_preference_metadata",
                    joinColumns = @JoinColumn(name = "preference_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    @Column(name = "contact_info", length = 500)
    private String contactInfo; // Email address, phone number, etc.
    
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "verification_token", length = 100)
    private String verificationToken;
    
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    public enum NotificationFrequency {
        IMMEDIATE,
        HOURLY,
        DAILY,
        WEEKLY,
        DISABLED
    }
    
    // Manual getter and setter methods since @Data isn't working
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public NotificationFrequency getFrequency() { return frequency; }
    public void setFrequency(NotificationFrequency frequency) { this.frequency = frequency; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getChannel() {
        return channel;
    }
    
    public Boolean getIsVerified() {
        return isVerified;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    /**
     * Check if notifications are allowed at the current time considering quiet hours.
     */
    public boolean isNotificationAllowedNow() {
        if (!isEnabled || frequency == NotificationFrequency.DISABLED) {
            return false;
        }
        
        if (quietHoursStart == null || quietHoursEnd == null) {
            return true;
        }
        
        // Simplified quiet hours check - in real implementation, consider timezone
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.LocalTime start = java.time.LocalTime.parse(quietHoursStart);
        java.time.LocalTime end = java.time.LocalTime.parse(quietHoursEnd);
        
        if (start.isBefore(end)) {
            // Same day quiet hours (e.g., 22:00 to 23:59)
            return now.isBefore(start) || now.isAfter(end);
        } else {
            // Overnight quiet hours (e.g., 22:00 to 08:00)
            return now.isAfter(end) && now.isBefore(start);
        }
    }
    
    /**
     * Check if the contact info needs verification.
     */
    public boolean needsVerification() {
        return !isVerified && contactInfo != null;
    }
    
    /**
     * Get display name for the preference.
     */
    public String getDisplayName() {
        return String.format("%s via %s", eventType, channel);
    }
}
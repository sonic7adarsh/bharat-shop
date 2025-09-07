package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
public class MediaFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "file_key", nullable = false, unique = true)
    private String key;
    
    @Column(name = "url", nullable = false)
    private String url;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MediaType type;
    
    @Column(name = "size_bytes")
    private Long size;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MediaStatus status;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "content_type")
    private String contentType;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // Constructors
    public MediaFile() {
        this.createdAt = LocalDateTime.now();
        this.status = MediaStatus.PENDING;
    }
    
    public MediaFile(String tenantId, String key, String url, MediaType type, Long size) {
        this();
        this.tenantId = tenantId;
        this.key = key;
        this.url = url;
        this.type = type;
        this.size = size;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public MediaType getType() {
        return type;
    }
    
    public void setType(MediaType type) {
        this.type = type;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public MediaStatus getStatus() {
        return status;
    }
    
    public void setStatus(MediaStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    // Utility methods
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Enums
    public enum MediaType {
        IMAGE,
        LOGO,
        BANNER,
        DOCUMENT,
        VIDEO,
        OTHER
    }
    
    public enum MediaStatus {
        PENDING,
        UPLOADED,
        PROCESSING,
        ACTIVE,
        FAILED,
        DELETED
    }
}
package com.bharatshop.shared.dto.media;

import com.bharatshop.shared.entity.MediaFile;

import java.time.LocalDateTime;

public class MediaFileResponse {
    
    private Long id;
    private Long tenantId;
    private String key;
    private String url;
    private MediaFile.MediaType type;
    private Long size;
    private MediaFile.MediaStatus status;
    private String originalFilename;
    private String contentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public MediaFileResponse() {}
    
    public MediaFileResponse(MediaFile mediaFile) {
        this.id = mediaFile.getId();
        this.tenantId = mediaFile.getTenantId() != null ? Long.parseLong(mediaFile.getTenantId()) : null;
        this.key = mediaFile.getKey();
        this.url = mediaFile.getUrl();
        this.type = mediaFile.getType();
        this.size = mediaFile.getSize();
        this.status = mediaFile.getStatus();
        this.originalFilename = mediaFile.getOriginalFilename();
        this.contentType = mediaFile.getContentType();
        this.createdAt = mediaFile.getCreatedAt();
        this.updatedAt = mediaFile.getUpdatedAt();
    }
    
    // Static factory method
    public static MediaFileResponse from(MediaFile mediaFile) {
        return new MediaFileResponse(mediaFile);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
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
    
    public MediaFile.MediaType getType() {
        return type;
    }
    
    public void setType(MediaFile.MediaType type) {
        this.type = type;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public MediaFile.MediaStatus getStatus() {
        return status;
    }
    
    public void setStatus(MediaFile.MediaStatus status) {
        this.status = status;
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
    
    // Utility methods
    public String getFormattedSize() {
        if (size == null) return "Unknown";
        
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    @Override
    public String toString() {
        return "MediaFileResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", key='" + key + '\'' +
                ", url='" + url + '\'' +
                ", type=" + type +
                ", size=" + size +
                ", status=" + status +
                ", originalFilename='" + originalFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
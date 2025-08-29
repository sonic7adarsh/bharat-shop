package com.bharatshop.shared.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PresignUploadRequest {
    
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename must not exceed 255 characters")
    private String filename;
    
    @NotBlank(message = "Content type is required")
    private String contentType;
    
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;
    
    private String description;
    
    // Constructors
    public PresignUploadRequest() {}
    
    public PresignUploadRequest(String filename, String contentType, Long fileSize) {
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
    
    public PresignUploadRequest(String filename, String contentType, Long fileSize, String description) {
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.description = description;
    }
    
    // Getters and Setters
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "PresignUploadRequest{" +
                "filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", description='" + description + '\'' +
                '}';
    }
}
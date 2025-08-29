package com.bharatshop.shared.dto.media;

import com.bharatshop.shared.entity.MediaFile;
import com.bharatshop.shared.service.MediaService;

import java.util.Map;

public class StorageUsageResponse {
    
    private long totalFiles;
    private long totalSizeBytes;
    private String formattedTotalSize;
    private Map<MediaFile.MediaType, Long> filesByType;
    
    // Constructors
    public StorageUsageResponse() {}
    
    public StorageUsageResponse(MediaService.StorageUsage storageUsage) {
        this.totalFiles = storageUsage.getTotalFiles();
        this.totalSizeBytes = storageUsage.getTotalSizeBytes();
        this.formattedTotalSize = formatBytes(totalSizeBytes);
        this.filesByType = storageUsage.getFilesByType();
    }
    
    // Static factory method
    public static StorageUsageResponse from(MediaService.StorageUsage storageUsage) {
        return new StorageUsageResponse(storageUsage);
    }
    
    // Getters and Setters
    public long getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }
    
    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
        this.formattedTotalSize = formatBytes(totalSizeBytes);
    }
    
    public String getFormattedTotalSize() {
        return formattedTotalSize;
    }
    
    public void setFormattedTotalSize(String formattedTotalSize) {
        this.formattedTotalSize = formattedTotalSize;
    }
    
    public Map<MediaFile.MediaType, Long> getFilesByType() {
        return filesByType;
    }
    
    public void setFilesByType(Map<MediaFile.MediaType, Long> filesByType) {
        this.filesByType = filesByType;
    }
    
    // Utility methods
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    @Override
    public String toString() {
        return "StorageUsageResponse{" +
                "totalFiles=" + totalFiles +
                ", totalSizeBytes=" + totalSizeBytes +
                ", formattedTotalSize='" + formattedTotalSize + '\'' +
                ", filesByType=" + filesByType +
                '}';
    }
}
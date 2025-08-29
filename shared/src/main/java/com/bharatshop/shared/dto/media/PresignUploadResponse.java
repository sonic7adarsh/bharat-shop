package com.bharatshop.shared.dto.media;

import java.util.List;
import java.util.Map;

public class PresignUploadResponse {
    
    private Long mediaFileId;
    private String key;
    private String uploadUrl;
    private Map<String, List<String>> headers;
    private Integer expiresInMinutes;
    
    // Constructors
    public PresignUploadResponse() {}
    
    public PresignUploadResponse(Long mediaFileId, String key, String uploadUrl, 
                               Map<String, List<String>> headers) {
        this.mediaFileId = mediaFileId;
        this.key = key;
        this.uploadUrl = uploadUrl;
        this.headers = headers;
        this.expiresInMinutes = 15; // Default 15 minutes
    }
    
    public PresignUploadResponse(Long mediaFileId, String key, String uploadUrl, 
                               Map<String, List<String>> headers, Integer expiresInMinutes) {
        this.mediaFileId = mediaFileId;
        this.key = key;
        this.uploadUrl = uploadUrl;
        this.headers = headers;
        this.expiresInMinutes = expiresInMinutes;
    }
    
    // Getters and Setters
    public Long getMediaFileId() {
        return mediaFileId;
    }
    
    public void setMediaFileId(Long mediaFileId) {
        this.mediaFileId = mediaFileId;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getUploadUrl() {
        return uploadUrl;
    }
    
    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
    
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
    
    public Integer getExpiresInMinutes() {
        return expiresInMinutes;
    }
    
    public void setExpiresInMinutes(Integer expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
    
    @Override
    public String toString() {
        return "PresignUploadResponse{" +
                "mediaFileId=" + mediaFileId +
                ", key='" + key + '\'' +
                ", uploadUrl='" + uploadUrl + '\'' +
                ", expiresInMinutes=" + expiresInMinutes +
                '}';
    }
}
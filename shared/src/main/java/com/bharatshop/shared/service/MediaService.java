package com.bharatshop.shared.service;

import com.bharatshop.shared.config.S3Config;
import com.bharatshop.shared.entity.MediaFile;
import com.bharatshop.shared.repository.MediaFileRepository;
import com.bharatshop.shared.util.FileUploadValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class MediaService {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);
    
    private final MediaFileRepository mediaFileRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;
    private final FileUploadValidator fileUploadValidator;
    

    
    @Autowired
    public MediaService(MediaFileRepository mediaFileRepository, 
                       S3Client s3Client, 
                       S3Presigner s3Presigner, 
                       S3Config s3Config,
                       FileUploadValidator fileUploadValidator) {
        this.mediaFileRepository = mediaFileRepository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Config = s3Config;
        this.fileUploadValidator = fileUploadValidator;
    }
    
    /**
     * Generate presigned upload URL for file upload
     */
    public PresignedUploadResponse generatePresignedUploadUrl(Long tenantId, String filename, 
                                                            String contentType, Long fileSize) {
        // Validate file upload using FileUploadValidator
        FileUploadValidator.ValidationResult validationResult = 
            fileUploadValidator.validateFile(filename, contentType, fileSize);
        
        if (!validationResult.isValid()) {
            throw new RuntimeException("File validation failed: " + validationResult.getErrorMessage());
        }
        
        MediaFile.MediaType mediaType = validationResult.getMediaType();
        
        // Sanitize filename
        String sanitizedFilename = fileUploadValidator.sanitizeFilename(filename);
        
        // Generate unique key using sanitized filename
        String key = generateFileKey(tenantId, mediaType, sanitizedFilename);
        
        try {
            // Create MediaFile record
            MediaFile mediaFile = new MediaFile(tenantId, key, "", mediaType, fileSize);
            mediaFile.setOriginalFilename(filename); // Keep original filename for reference
            mediaFile.setContentType(contentType);
            mediaFile.setStatus(MediaFile.MediaStatus.PENDING);
            
            mediaFile = mediaFileRepository.save(mediaFile);
            
            // Generate presigned URL
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .build();
            
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15)) // 15 minutes expiry
                    .putObjectRequest(putObjectRequest)
                    .build();
            
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            
            logger.info("Generated presigned upload URL for tenant: {}, file: {}, key: {}", 
                       tenantId, filename, key);
            
            return new PresignedUploadResponse(
                mediaFile.getId(),
                key,
                presignedRequest.url().toString(),
                presignedRequest.httpRequest().headers()
            );
            
        } catch (Exception e) {
            logger.error("Error generating presigned upload URL for tenant: {}, file: {}", 
                        tenantId, filename, e);
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }
    
    /**
     * Confirm successful upload and update MediaFile status
     */
    public MediaFile confirmUpload(String key) {
        Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByKeyAndNotDeleted(key);
        
        if (mediaFileOpt.isEmpty()) {
            throw new RuntimeException("MediaFile not found for key: " + key);
        }
        
        MediaFile mediaFile = mediaFileOpt.get();
        
        try {
            // Check if file exists in S3
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();
            
            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            
            // Update MediaFile with actual URL and status
            String publicUrl = generatePublicUrl(key);
            mediaFile.setUrl(publicUrl);
            mediaFile.setStatus(MediaFile.MediaStatus.ACTIVE);
            mediaFile.setSize(headObjectResponse.contentLength());
            
            mediaFile = mediaFileRepository.save(mediaFile);
            
            logger.info("Confirmed upload for key: {}, URL: {}", key, publicUrl);
            
            return mediaFile;
            
        } catch (Exception e) {
            logger.error("Error confirming upload for key: {}", key, e);
            mediaFile.setStatus(MediaFile.MediaStatus.FAILED);
            mediaFileRepository.save(mediaFile);
            throw new RuntimeException("Failed to confirm upload", e);
        }
    }
    
    /**
     * Get MediaFile by ID and tenant
     */
    @Transactional(readOnly = true)
    public Optional<MediaFile> getMediaFile(Long id, Long tenantId) {
        return mediaFileRepository.findByIdAndTenantIdAndNotDeleted(id, tenantId);
    }
    
    /**
     * Get MediaFiles by tenant with pagination
     */
    @Transactional(readOnly = true)
    public Page<MediaFile> getMediaFiles(Long tenantId, Pageable pageable) {
        return mediaFileRepository.findByTenantIdAndNotDeleted(tenantId, pageable);
    }
    
    /**
     * Get MediaFiles by tenant and type
     */
    @Transactional(readOnly = true)
    public Page<MediaFile> getMediaFilesByType(Long tenantId, MediaFile.MediaType type, Pageable pageable) {
        return mediaFileRepository.findByTenantIdAndTypeAndNotDeleted(tenantId, type, pageable);
    }
    
    /**
     * Delete MediaFile (soft delete)
     */
    public void deleteMediaFile(Long id, Long tenantId) {
        Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByIdAndTenantIdAndNotDeleted(id, tenantId);
        
        if (mediaFileOpt.isEmpty()) {
            throw new RuntimeException("MediaFile not found");
        }
        
        MediaFile mediaFile = mediaFileOpt.get();
        mediaFile.markAsDeleted();
        mediaFileRepository.save(mediaFile);
        
        logger.info("Soft deleted MediaFile: {} for tenant: {}", id, tenantId);
    }
    
    /**
     * Get storage usage for tenant
     */
    @Transactional(readOnly = true)
    public StorageUsage getStorageUsage(Long tenantId) {
        long totalFiles = mediaFileRepository.countByTenantIdAndNotDeleted(tenantId);
        long totalSize = mediaFileRepository.calculateTotalSizeByTenantIdAndNotDeleted(tenantId);
        
        Map<MediaFile.MediaType, Long> typeCount = new HashMap<>();
        for (MediaFile.MediaType type : MediaFile.MediaType.values()) {
            long count = mediaFileRepository.countByTenantIdAndTypeAndNotDeleted(tenantId, type);
            if (count > 0) {
                typeCount.put(type, count);
            }
        }
        
        return new StorageUsage(totalFiles, totalSize, typeCount);
    }
    
    /**
     * Cleanup pending uploads older than specified hours
     */
    public void cleanupPendingUploads(int hoursOld) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursOld);
        List<MediaFile> pendingFiles = mediaFileRepository.findPendingUploadsOlderThan(cutoffTime);
        
        for (MediaFile mediaFile : pendingFiles) {
            mediaFile.setStatus(MediaFile.MediaStatus.FAILED);
            mediaFile.markAsDeleted();
        }
        
        if (!pendingFiles.isEmpty()) {
            mediaFileRepository.saveAll(pendingFiles);
            logger.info("Cleaned up {} pending uploads older than {} hours", pendingFiles.size(), hoursOld);
        }
    }
    
    // Private helper methods
    

    
    private String generateFileKey(Long tenantId, MediaFile.MediaType mediaType, String filename) {
        String extension = getFileExtension(filename);
        String uuid = UUID.randomUUID().toString();
        return String.format("tenant-%d/%s/%s%s", tenantId, mediaType.name().toLowerCase(), uuid, extension);
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    private String generatePublicUrl(String key) {
        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            // For MinIO or custom endpoints
            return String.format("%s/%s/%s", s3Config.getEndpoint(), s3Config.getBucketName(), key);
        } else {
            // For AWS S3
            return String.format("https://%s.s3.%s.amazonaws.com/%s", 
                               s3Config.getBucketName(), s3Config.getRegion(), key);
        }
    }
    
    // Inner classes for responses
    
    public static class PresignedUploadResponse {
        private final Long mediaFileId;
        private final String key;
        private final String uploadUrl;
        private final Map<String, List<String>> headers;
        
        public PresignedUploadResponse(Long mediaFileId, String key, String uploadUrl, Map<String, List<String>> headers) {
            this.mediaFileId = mediaFileId;
            this.key = key;
            this.uploadUrl = uploadUrl;
            this.headers = headers;
        }
        
        // Getters
        public Long getMediaFileId() { return mediaFileId; }
        public String getKey() { return key; }
        public String getUploadUrl() { return uploadUrl; }
        public Map<String, List<String>> getHeaders() { return headers; }
    }
    
    public static class StorageUsage {
        private final long totalFiles;
        private final long totalSizeBytes;
        private final Map<MediaFile.MediaType, Long> filesByType;
        
        public StorageUsage(long totalFiles, long totalSizeBytes, Map<MediaFile.MediaType, Long> filesByType) {
            this.totalFiles = totalFiles;
            this.totalSizeBytes = totalSizeBytes;
            this.filesByType = filesByType;
        }
        
        // Getters
        public long getTotalFiles() { return totalFiles; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        public Map<MediaFile.MediaType, Long> getFilesByType() { return filesByType; }
    }
}
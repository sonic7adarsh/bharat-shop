package com.bharatshop.shared.controller;

import com.bharatshop.shared.dto.media.*;
import com.bharatshop.shared.entity.MediaFile;
import com.bharatshop.shared.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "*")
public class MediaController {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    
    private final MediaService mediaService;
    
    @Autowired
    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }
    
    /**
     * Generate presigned upload URL
     * POST /api/media/presign-upload
     */
    @PostMapping("/presign-upload")
    public ResponseEntity<?> generatePresignedUploadUrl(
            @Valid @RequestBody PresignUploadRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract tenant ID from request (assuming it's in header or session)
            Long tenantId = extractTenantId(httpRequest);
            
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Tenant ID not found"));
            }
            
            logger.info("Generating presigned upload URL for tenant: {}, file: {}", 
                       tenantId, request.getFilename());
            
            MediaService.PresignedUploadResponse serviceResponse = mediaService.generatePresignedUploadUrl(
                tenantId, 
                request.getFilename(), 
                request.getContentType(), 
                request.getFileSize()
            );
            
            PresignUploadResponse response = new PresignUploadResponse(
                serviceResponse.getMediaFileId(),
                serviceResponse.getKey(),
                serviceResponse.getUploadUrl(),
                serviceResponse.getHeaders()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error generating presigned upload URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error generating presigned upload URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
        }
    }
    
    /**
     * Get media file by ID
     * GET /api/media/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMediaFile(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Tenant ID not found"));
            }
            
            Optional<MediaFile> mediaFileOpt = mediaService.getMediaFile(id, tenantId);
            
            if (mediaFileOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Media file not found"));
            }
            
            MediaFileResponse response = MediaFileResponse.from(mediaFileOpt.get());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving media file with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
        }
    }
    
    /**
     * Get media files with pagination
     * GET /api/media
     */
    @GetMapping
    public ResponseEntity<?> getMediaFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String type,
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Tenant ID not found"));
            }
            
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                               Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<MediaFile> mediaFiles;
            
            if (type != null && !type.isEmpty()) {
                try {
                    MediaFile.MediaType mediaType = MediaFile.MediaType.valueOf(type.toUpperCase());
                    mediaFiles = mediaService.getMediaFilesByType(tenantId, mediaType, pageable);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Invalid media type: " + type));
                }
            } else {
                mediaFiles = mediaService.getMediaFiles(tenantId, pageable);
            }
            
            Page<MediaFileResponse> response = mediaFiles.map(MediaFileResponse::from);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving media files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
        }
    }
    
    /**
     * Confirm upload completion
     * POST /api/media/confirm-upload
     */
    @PostMapping("/confirm-upload")
    public ResponseEntity<?> confirmUpload(
            @RequestParam String key,
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Tenant ID not found"));
            }
            
            MediaFile mediaFile = mediaService.confirmUpload(key);
            MediaFileResponse response = MediaFileResponse.from(mediaFile);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error confirming upload for key: {}", key, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error confirming upload for key: {}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
        }
    }
    
    /**
     * Delete media file
     * DELETE /api/media/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMediaFile(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Tenant ID not found"));
            }
            
            mediaService.deleteMediaFile(id, tenantId);
            
            return ResponseEntity.ok(new SuccessResponse("Media file deleted successfully"));
            
        } catch (RuntimeException e) {
            logger.error("Error deleting media file with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting media file with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
        }
    }
    
    /**
     * Get storage usage statistics
     * GET /api/media/storage-usage
     */
    @GetMapping("/storage-usage")
    public ResponseEntity<?> getStorageUsage(HttpServletRequest httpRequest) {
        
        try {
            Long tenantId = extractTenantId(httpRequest);
            
            if (tenantId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Tenant ID not found"));
            }
            
            MediaService.StorageUsage storageUsage = mediaService.getStorageUsage(tenantId);
            StorageUsageResponse response = StorageUsageResponse.from(storageUsage);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving storage usage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error"));
        }
    }
    
    // Private helper methods
    
    private Long extractTenantId(HttpServletRequest request) {
        // Try to get tenant ID from header first
        String tenantIdHeader = request.getHeader("X-Tenant-ID");
        if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
            try {
                return Long.parseLong(tenantIdHeader);
            } catch (NumberFormatException e) {
                logger.warn("Invalid tenant ID in header: {}", tenantIdHeader);
            }
        }
        
        // Try to get from request parameter
        String tenantIdParam = request.getParameter("tenantId");
        if (tenantIdParam != null && !tenantIdParam.isEmpty()) {
            try {
                return Long.parseLong(tenantIdParam);
            } catch (NumberFormatException e) {
                logger.warn("Invalid tenant ID in parameter: {}", tenantIdParam);
            }
        }
        
        // TODO: In a real application, you might extract tenant ID from JWT token or session
        // For now, return a default tenant ID for testing
        logger.warn("No tenant ID found in request, using default tenant ID: 1");
        return 1L;
    }
    
    // Response classes
    
    public static class ErrorResponse {
        private String error;
        private long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getError() { return error; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class SuccessResponse {
        private String message;
        private long timestamp;
        
        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}
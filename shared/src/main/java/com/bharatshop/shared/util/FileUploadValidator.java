package com.bharatshop.shared.util;

import com.bharatshop.shared.entity.MediaFile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class FileUploadValidator {
    
    // Maximum file sizes in bytes
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_LOGO_SIZE = 5 * 1024 * 1024;   // 5MB
    private static final long MAX_BANNER_SIZE = 15 * 1024 * 1024; // 15MB
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    
    // Allowed MIME types for each media type
    private static final Map<MediaFile.MediaType, Set<String>> ALLOWED_MIME_TYPES = Map.of(
        MediaFile.MediaType.IMAGE, Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml"
        ),
        MediaFile.MediaType.LOGO, Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml"
        ),
        MediaFile.MediaType.BANNER, Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml"
        ),
        MediaFile.MediaType.DOCUMENT, Set.of(
            "application/pdf", "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv"
        ),
        MediaFile.MediaType.VIDEO, Set.of(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo", "video/webm"
        )
    );
    
    // Allowed file extensions for each media type
    private static final Map<MediaFile.MediaType, Set<String>> ALLOWED_EXTENSIONS = Map.of(
        MediaFile.MediaType.IMAGE, Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg"
        ),
        MediaFile.MediaType.LOGO, Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg"
        ),
        MediaFile.MediaType.BANNER, Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg"
        ),
        MediaFile.MediaType.DOCUMENT, Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt", ".csv"
        ),
        MediaFile.MediaType.VIDEO, Set.of(
            ".mp4", ".mpeg", ".mov", ".avi", ".webm"
        )
    );
    
    // Dangerous file extensions that should never be allowed
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        ".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar",
        ".sh", ".php", ".asp", ".aspx", ".jsp", ".py", ".rb", ".pl", ".cgi"
    );
    
    // Pattern for valid filename (alphanumeric, spaces, hyphens, underscores, dots)
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\s\\-_\\.()\\[\\]]+$"
    );
    
    /**
     * Validates a file upload request
     */
    public ValidationResult validateFile(String filename, String contentType, long fileSize) {
        List<String> errors = new ArrayList<>();
        
        // Validate filename
        if (filename == null || filename.trim().isEmpty()) {
            errors.add("Filename cannot be empty");
            return new ValidationResult(false, errors, null);
        }
        
        filename = filename.trim();
        
        // Check filename length
        if (filename.length() > 255) {
            errors.add("Filename is too long (maximum 255 characters)");
        }
        
        // Check for valid filename pattern
        if (!VALID_FILENAME_PATTERN.matcher(filename).matches()) {
            errors.add("Filename contains invalid characters");
        }
        
        // Check for dangerous extensions
        String extension = getFileExtension(filename).toLowerCase();
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            errors.add("File type not allowed for security reasons");
        }
        
        // Validate content type
        if (contentType == null || contentType.trim().isEmpty()) {
            errors.add("Content type cannot be empty");
        }
        
        // Validate file size
        if (fileSize <= 0) {
            errors.add("File size must be greater than 0");
        }
        
        if (fileSize > MAX_DOCUMENT_SIZE) { // Use the largest allowed size as absolute maximum
            errors.add("File size exceeds maximum allowed size (50MB)");
        }
        
        // Determine media type and validate accordingly
        MediaFile.MediaType mediaType = determineMediaType(filename, contentType);
        if (mediaType == null) {
            errors.add("Unsupported file type");
        } else {
            // Validate against media type specific rules
            validateMediaTypeSpecific(filename, contentType, fileSize, mediaType, errors);
        }
        
        return new ValidationResult(errors.isEmpty(), errors, mediaType);
    }
    
    /**
     * Determines the media type based on filename and content type
     */
    public MediaFile.MediaType determineMediaType(String filename, String contentType) {
        if (filename == null || contentType == null) {
            return null;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        String mimeType = contentType.toLowerCase();
        
        // Check each media type
        for (MediaFile.MediaType type : MediaFile.MediaType.values()) {
            Set<String> allowedExtensions = ALLOWED_EXTENSIONS.get(type);
            Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES.get(type);
            
            if (allowedExtensions != null && allowedMimeTypes != null) {
                if (allowedExtensions.contains(extension) && allowedMimeTypes.contains(mimeType)) {
                    return type;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validates file against media type specific rules
     */
    private void validateMediaTypeSpecific(String filename, String contentType, long fileSize, 
                                         MediaFile.MediaType mediaType, List<String> errors) {
        
        String extension = getFileExtension(filename).toLowerCase();
        String mimeType = contentType.toLowerCase();
        
        // Check allowed extensions
        Set<String> allowedExtensions = ALLOWED_EXTENSIONS.get(mediaType);
        if (allowedExtensions != null && !allowedExtensions.contains(extension)) {
            errors.add("File extension '" + extension + "' not allowed for " + mediaType.name().toLowerCase());
        }
        
        // Check allowed MIME types
        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES.get(mediaType);
        if (allowedMimeTypes != null && !allowedMimeTypes.contains(mimeType)) {
            errors.add("Content type '" + mimeType + "' not allowed for " + mediaType.name().toLowerCase());
        }
        
        // Check file size limits
        long maxSize = getMaxSizeForMediaType(mediaType);
        if (fileSize > maxSize) {
            errors.add(String.format("File size (%s) exceeds maximum allowed size for %s (%s)",
                formatFileSize(fileSize), mediaType.name().toLowerCase(), formatFileSize(maxSize)));
        }
    }
    
    /**
     * Gets the maximum allowed file size for a media type
     */
    public long getMaxSizeForMediaType(MediaFile.MediaType mediaType) {
        return switch (mediaType) {
            case IMAGE -> MAX_IMAGE_SIZE;
            case LOGO -> MAX_LOGO_SIZE;
            case BANNER -> MAX_BANNER_SIZE;
            case DOCUMENT -> MAX_DOCUMENT_SIZE;
            case VIDEO -> MAX_VIDEO_SIZE;
            case OTHER -> MAX_DOCUMENT_SIZE; // Default to document size for OTHER type
        };
    }
    
    /**
     * Extracts file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex);
    }
    
    /**
     * Formats file size in human readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Sanitizes filename by removing or replacing invalid characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_file";
        }
        
        filename = filename.trim();
        
        // Replace invalid characters with underscores
        filename = filename.replaceAll("[^a-zA-Z0-9\\s\\-_\\.()\\[\\]]", "_");
        
        // Remove multiple consecutive spaces/underscores
        filename = filename.replaceAll("[\\s_]+", "_");
        
        // Remove leading/trailing dots and underscores
        filename = filename.replaceAll("^[\\._]+|[\\._]+$", "");
        
        // Ensure filename is not empty after sanitization
        if (filename.isEmpty()) {
            filename = "unnamed_file";
        }
        
        // Limit length
        if (filename.length() > 200) {
            String extension = getFileExtension(filename);
            String nameWithoutExt = filename.substring(0, filename.length() - extension.length());
            filename = nameWithoutExt.substring(0, 200 - extension.length()) + extension;
        }
        
        return filename;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final MediaFile.MediaType mediaType;
        
        public ValidationResult(boolean valid, List<String> errors, MediaFile.MediaType mediaType) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.mediaType = mediaType;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public MediaFile.MediaType getMediaType() {
            return mediaType;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Entity representing images attached to return requests.
 * Supports multiple images per return request for evidence and documentation.
 */
@Entity
@Table(name = "return_request_images")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE return_request_images SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ReturnRequestImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "return_request_id", nullable = false)
    private Long returnRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", insertable = false, updatable = false)
    private ReturnRequest returnRequest;

    @Column(name = "return_request_item_id")
    private Long returnRequestItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_item_id", insertable = false, updatable = false)
    private ReturnRequestItem returnRequestItem;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false)
    private ImageType imageType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    // Enums

    public enum ImageType {
        DEFECT_EVIDENCE("Image showing defect or damage"),
        PACKAGING_DAMAGE("Image showing packaging damage"),
        WRONG_ITEM("Image showing wrong item received"),
        SIZE_COMPARISON("Image showing size issues"),
        QUALITY_ISSUE("Image showing quality problems"),
        UNBOXING("Unboxing or received condition image"),
        GENERAL("General return request image"),
        RECEIPT("Purchase receipt or invoice"),
        OTHER("Other supporting image");

        private final String description;

        ImageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEvidenceType() {
            return this == DEFECT_EVIDENCE || this == PACKAGING_DAMAGE || 
                   this == WRONG_ITEM || this == QUALITY_ISSUE;
        }
    }

    // Business methods

    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    public boolean isValidImageType() {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("image/") && 
               (mimeType.equals("image/jpeg") || 
                mimeType.equals("image/jpg") || 
                mimeType.equals("image/png") || 
                mimeType.equals("image/gif") || 
                mimeType.equals("image/webp"));
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) {
            return "Unknown";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public boolean isWithinSizeLimit(long maxSizeInBytes) {
        return fileSize != null && fileSize <= maxSizeInBytes;
    }

    public void generateFileName() {
        if (returnRequestId != null && originalFileName != null) {
            String extension = getFileExtension();
            String timestamp = String.valueOf(System.currentTimeMillis());
            this.fileName = String.format("return_%d_%s_%s.%s", 
                    returnRequestId, imageType.name().toLowerCase(), timestamp, extension);
        }
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (fileName == null) {
            generateFileName();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
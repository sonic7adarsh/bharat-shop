package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    
    // Find by tenant and not deleted
    List<MediaFile> findByTenantIdAndDeletedAtIsNull(Long tenantId);
    
    Page<MediaFile> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);
    
    // Find by tenant, type and not deleted
    List<MediaFile> findByTenantIdAndTypeAndDeletedAtIsNull(Long tenantId, MediaFile.MediaType type);
    
    Page<MediaFile> findByTenantIdAndTypeAndDeletedAtIsNull(Long tenantId, MediaFile.MediaType type, Pageable pageable);
    
    // Find by tenant, status and not deleted
    List<MediaFile> findByTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, MediaFile.MediaStatus status);
    
    // Find by key and not deleted
    Optional<MediaFile> findByKeyAndDeletedAtIsNull(String key);
    
    // Find by tenant and key and not deleted
    Optional<MediaFile> findByTenantIdAndKeyAndDeletedAtIsNull(Long tenantId, String key);
    
    // Find by id and tenant and not deleted
    Optional<MediaFile> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);
    
    // Count by tenant and not deleted
    long countByTenantIdAndDeletedAtIsNull(Long tenantId);
    
    // Count by tenant, type and not deleted
    long countByTenantIdAndTypeAndDeletedAtIsNull(Long tenantId, MediaFile.MediaType type);
    
    // Alias methods for service compatibility
    default Optional<MediaFile> findByIdAndTenantIdAndNotDeleted(Long id, Long tenantId) {
        return findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId);
    }
    
    default Page<MediaFile> findByTenantIdAndNotDeleted(Long tenantId, Pageable pageable) {
        return findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }
    
    default Page<MediaFile> findByTenantIdAndTypeAndNotDeleted(Long tenantId, MediaFile.MediaType type, Pageable pageable) {
        return findByTenantIdAndTypeAndDeletedAtIsNull(tenantId, type, pageable);
    }
    
    default long countByTenantIdAndNotDeleted(Long tenantId) {
        return countByTenantIdAndDeletedAtIsNull(tenantId);
    }
    
    default long countByTenantIdAndTypeAndNotDeleted(Long tenantId, MediaFile.MediaType type) {
        return countByTenantIdAndTypeAndDeletedAtIsNull(tenantId, type);
    }
    
    default List<MediaFile> findPendingUploadsOlderThan(LocalDateTime beforeDate) {
        return findByStatusAndCreatedAtBefore(MediaFile.MediaStatus.PENDING, beforeDate);
    }
    
    default Optional<MediaFile> findByKeyAndNotDeleted(String key) {
        return findByKeyAndDeletedAtIsNull(key);
    }
    
    // Calculate total size by tenant and not deleted
    @Query(value = "SELECT COALESCE(SUM(size), 0) FROM media_file WHERE tenant_id = ?1 AND deleted_at IS NULL", nativeQuery = true)
    long calculateTotalSizeByTenantIdAndNotDeleted(Long tenantId);
    
    // Find files older than specified date for cleanup
    List<MediaFile> findByStatusAndCreatedAtBefore(MediaFile.MediaStatus status, LocalDateTime beforeDate);
    
    // Soft delete by setting deletedAt
    @Modifying
    @Query(value = "UPDATE media_file SET deleted_at = ?3 WHERE id = ?1 AND tenant_id = ?2", nativeQuery = true)
    void softDeleteByIdAndTenantId(Long id, Long tenantId, LocalDateTime deletedAt);
    
    // Soft delete by key
    @Modifying
    @Query(value = "UPDATE media_file SET deleted_at = ?3 WHERE key = ?1 AND tenant_id = ?2", nativeQuery = true)
    void softDeleteByKeyAndTenantId(String key, Long tenantId, LocalDateTime deletedAt);
    
    // Update status
    @Modifying
    @Query(value = "UPDATE media_file SET status = ?2, updated_at = ?3 WHERE id = ?1", nativeQuery = true)
    void updateStatus(Long id, MediaFile.MediaStatus status, LocalDateTime updatedAt);
    
    // Update URL after successful upload
    @Modifying
    @Query(value = "UPDATE media_file SET url = ?2, status = ?3, updated_at = ?4 WHERE key = ?1", nativeQuery = true)
    void updateUrlAndStatusByKey(String key, String url, MediaFile.MediaStatus status, LocalDateTime updatedAt);
}
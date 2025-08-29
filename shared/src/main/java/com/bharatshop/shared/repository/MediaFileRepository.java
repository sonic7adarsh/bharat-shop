package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    
    // Find by tenant and not deleted
    @Query("SELECT mf FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.deletedAt IS NULL")
    List<MediaFile> findByTenantIdAndNotDeleted(@Param("tenantId") Long tenantId);
    
    @Query("SELECT mf FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.deletedAt IS NULL")
    Page<MediaFile> findByTenantIdAndNotDeleted(@Param("tenantId") Long tenantId, Pageable pageable);
    
    // Find by tenant, type and not deleted
    @Query("SELECT mf FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.type = :type AND mf.deletedAt IS NULL")
    List<MediaFile> findByTenantIdAndTypeAndNotDeleted(@Param("tenantId") Long tenantId, @Param("type") MediaFile.MediaType type);
    
    @Query("SELECT mf FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.type = :type AND mf.deletedAt IS NULL")
    Page<MediaFile> findByTenantIdAndTypeAndNotDeleted(@Param("tenantId") Long tenantId, @Param("type") MediaFile.MediaType type, Pageable pageable);
    
    // Find by tenant, status and not deleted
    @Query("SELECT mf FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.status = :status AND mf.deletedAt IS NULL")
    List<MediaFile> findByTenantIdAndStatusAndNotDeleted(@Param("tenantId") Long tenantId, @Param("status") MediaFile.MediaStatus status);
    
    // Find by key and not deleted
    @Query("SELECT mf FROM MediaFile mf WHERE mf.key = :key AND mf.deletedAt IS NULL")
    Optional<MediaFile> findByKeyAndNotDeleted(@Param("key") String key);
    
    // Find by tenant and key and not deleted
    @Query("SELECT mf FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.key = :key AND mf.deletedAt IS NULL")
    Optional<MediaFile> findByTenantIdAndKeyAndNotDeleted(@Param("tenantId") Long tenantId, @Param("key") String key);
    
    // Find by id and tenant and not deleted
    @Query("SELECT mf FROM MediaFile mf WHERE mf.id = :id AND mf.tenantId = :tenantId AND mf.deletedAt IS NULL")
    Optional<MediaFile> findByIdAndTenantIdAndNotDeleted(@Param("id") Long id, @Param("tenantId") Long tenantId);
    
    // Count by tenant and not deleted
    @Query("SELECT COUNT(mf) FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.deletedAt IS NULL")
    long countByTenantIdAndNotDeleted(@Param("tenantId") Long tenantId);
    
    // Count by tenant, type and not deleted
    @Query("SELECT COUNT(mf) FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.type = :type AND mf.deletedAt IS NULL")
    long countByTenantIdAndTypeAndNotDeleted(@Param("tenantId") Long tenantId, @Param("type") MediaFile.MediaType type);
    
    // Calculate total size by tenant and not deleted
    @Query("SELECT COALESCE(SUM(mf.size), 0) FROM MediaFile mf WHERE mf.tenantId = :tenantId AND mf.deletedAt IS NULL")
    long calculateTotalSizeByTenantIdAndNotDeleted(@Param("tenantId") Long tenantId);
    
    // Find files older than specified date for cleanup
    @Query("SELECT mf FROM MediaFile mf WHERE mf.status = :status AND mf.createdAt < :beforeDate")
    List<MediaFile> findByStatusAndCreatedAtBefore(@Param("status") MediaFile.MediaStatus status, @Param("beforeDate") LocalDateTime beforeDate);
    
    // Find pending uploads older than specified time for cleanup
    @Query("SELECT mf FROM MediaFile mf WHERE mf.status = 'PENDING' AND mf.createdAt < :beforeDate")
    List<MediaFile> findPendingUploadsOlderThan(@Param("beforeDate") LocalDateTime beforeDate);
    
    // Soft delete by setting deletedAt
    @Query("UPDATE MediaFile mf SET mf.deletedAt = :deletedAt WHERE mf.id = :id AND mf.tenantId = :tenantId")
    void softDeleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("deletedAt") LocalDateTime deletedAt);
    
    // Soft delete by key
    @Query("UPDATE MediaFile mf SET mf.deletedAt = :deletedAt WHERE mf.key = :key AND mf.tenantId = :tenantId")
    void softDeleteByKeyAndTenantId(@Param("key") String key, @Param("tenantId") Long tenantId, @Param("deletedAt") LocalDateTime deletedAt);
    
    // Update status
    @Query("UPDATE MediaFile mf SET mf.status = :status, mf.updatedAt = :updatedAt WHERE mf.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") MediaFile.MediaStatus status, @Param("updatedAt") LocalDateTime updatedAt);
    
    // Update URL after successful upload
    @Query("UPDATE MediaFile mf SET mf.url = :url, mf.status = :status, mf.updatedAt = :updatedAt WHERE mf.key = :key")
    void updateUrlAndStatusByKey(@Param("key") String key, @Param("url") String url, @Param("status") MediaFile.MediaStatus status, @Param("updatedAt") LocalDateTime updatedAt);
}
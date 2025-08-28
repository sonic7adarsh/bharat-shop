package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface TenantAwareRepository<T extends BaseEntity> extends JpaRepository<T, UUID> {
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL")
    List<T> findAllByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL")
    Page<T> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId AND e.deletedAt IS NULL")
    Optional<T> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    List<T> findAllActive();
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    Page<T> findAllActive(Pageable pageable);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL")
    Page<T> findActiveByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
    
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL")
    long countByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    long countActive();
}
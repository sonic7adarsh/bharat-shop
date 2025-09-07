package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
@NoRepositoryBean
public interface TenantAwareRepository<T extends BaseEntity> extends JpaRepository<T, Long> {
    
    // Using Spring Data JPA method naming conventions instead of HQL
    List<T> findByTenantIdAndDeletedAtIsNull(Long tenantId);
    
    Optional<T> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);
    
    List<T> findByDeletedAtIsNull();
    
    Page<T> findByDeletedAtIsNull(Pageable pageable);
    
    Page<T> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);
    
    long countByTenantIdAndDeletedAtIsNull(Long tenantId);
    
    long countByDeletedAtIsNull();
    
    // Alias methods for backward compatibility
    default List<T> findAllByTenantId(Long tenantId) {
        return findByTenantIdAndDeletedAtIsNull(tenantId);
    }
    
    default Page<T> findAllByTenantId(Long tenantId, Pageable pageable) {
        return findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }
    
    default Optional<T> findByIdAndTenantId(Long id, Long tenantId) {
        return findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId);
    }
    
    default List<T> findAllActive() {
        return findByDeletedAtIsNull();
    }
    
    default Page<T> findAllActive(Pageable pageable) {
        return findByDeletedAtIsNull(pageable);
    }
    
    default Page<T> findActiveByTenantId(Long tenantId, Pageable pageable) {
        return findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }
    
    default long countByTenantId(Long tenantId) {
        return countByTenantIdAndDeletedAtIsNull(tenantId);
    }
    
    default long countActive() {
        return countByDeletedAtIsNull();
    }
}
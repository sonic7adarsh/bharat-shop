package com.bharatshop.platform.repository;

import com.bharatshop.platform.model.Tenant;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends TenantAwareRepository<Tenant> {
    
    Optional<Tenant> findByCode(String code);
    
    Optional<Tenant> findByName(String name);
    
    boolean existsByName(String name);
    
    boolean existsByCode(String code);
    
    List<Tenant> findByActiveTrue();
    
    Page<Tenant> findByActiveTrue(Pageable pageable);
    
    @Query(value = "SELECT * FROM tenants WHERE active = true AND (name LIKE CONCAT('%', :search, '%') OR code LIKE CONCAT('%', :search, '%'))", nativeQuery = true)
    Page<Tenant> findActiveTenantsWithSearch(@Param("search") String search, Pageable pageable);
    
    @Query(value = "SELECT COUNT(*) FROM tenants WHERE active = true", nativeQuery = true)
    long countActiveTenants();
}
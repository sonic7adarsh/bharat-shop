package com.bharatshop.platform.service;

import com.bharatshop.platform.dto.TenantCreateDto;
import com.bharatshop.platform.dto.TenantResponseDto;
import com.bharatshop.platform.model.Tenant;
import com.bharatshop.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {
    
    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    
    private final TenantRepository tenantRepository;
    
    @Cacheable(value = "tenants", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<TenantResponseDto> getAllTenants(Pageable pageable) {
        log.debug("Fetching all tenants with pagination: {}", pageable);
        return tenantRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "tenant", key = "#id")
    public TenantResponseDto getTenantById(UUID id) {
        log.debug("Fetching tenant by id: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));
        return mapToResponseDto(tenant);
    }
    
    @Transactional
    @CacheEvict(value = {"tenants", "tenant"}, allEntries = true)
    public TenantResponseDto createTenant(TenantCreateDto createDto) {
        log.info("Creating new tenant: {}", createDto.getName());
        
        // Check if tenant with same name or code already exists
        if (tenantRepository.existsByName(createDto.getName())) {
            throw new RuntimeException("Tenant with name already exists: " + createDto.getName());
        }
        
        if (tenantRepository.existsByCode(createDto.getCode())) {
            throw new RuntimeException("Tenant with code already exists: " + createDto.getCode());
        }
        
        Tenant tenant = Tenant.builder()
                .name(createDto.getName())
                .code(createDto.getCode())
                .description(createDto.getDescription())
                .active(true)
                .build();
        
        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Tenant created successfully with id: {}", savedTenant.getId());
        
        return mapToResponseDto(savedTenant);
    }
    
    @Transactional
    @CacheEvict(value = {"tenants", "tenant"}, allEntries = true)
    public TenantResponseDto updateTenant(UUID id, TenantCreateDto updateDto) {
        log.info("Updating tenant with id: {}", id);
        
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found with id: " + id));
        
        // Check if another tenant with same name or code exists
        if (!tenant.getName().equals(updateDto.getName()) && 
            tenantRepository.existsByName(updateDto.getName())) {
            throw new RuntimeException("Tenant with name already exists: " + updateDto.getName());
        }
        
        if (!tenant.getCode().equals(updateDto.getCode()) && 
            tenantRepository.existsByCode(updateDto.getCode())) {
            throw new RuntimeException("Tenant with code already exists: " + updateDto.getCode());
        }
        
        tenant.setName(updateDto.getName());
        tenant.setCode(updateDto.getCode());
        tenant.setDescription(updateDto.getDescription());
        
        Tenant updatedTenant = tenantRepository.save(tenant);
        log.info("Tenant updated successfully with id: {}", updatedTenant.getId());
        
        return mapToResponseDto(updatedTenant);
    }
    
    @Transactional
    @CacheEvict(value = {"tenants", "tenant"}, allEntries = true)
    public void deleteTenant(UUID id) {
        log.info("Deleting tenant with id: {}", id);
        
        if (!tenantRepository.existsById(id)) {
            throw new RuntimeException("Tenant not found with id: " + id);
        }
        
        tenantRepository.deleteById(id);
        log.info("Tenant deleted successfully with id: {}", id);
    }
    
    private TenantResponseDto mapToResponseDto(Tenant tenant) {
        return TenantResponseDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .code(tenant.getCode())
                .description(tenant.getDescription())
                .active(tenant.isActive())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
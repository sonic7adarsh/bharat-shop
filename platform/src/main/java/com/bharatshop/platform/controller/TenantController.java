package com.bharatshop.platform.controller;

import com.bharatshop.platform.dto.TenantCreateDto;
import com.bharatshop.platform.dto.TenantResponseDto;
import com.bharatshop.platform.service.TenantService;
import com.bharatshop.platform.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// import java.util.UUID; // Replaced with Long

@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "APIs for managing tenants in the platform")
public class TenantController {
    
    private final TenantService tenantService;
    
    @GetMapping
    @Operation(summary = "Get all tenants", description = "Retrieve a paginated list of all tenants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TenantResponseDto>>> getAllTenants(Pageable pageable) {
        Page<TenantResponseDto> tenants = tenantService.getAllTenants(pageable);
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID", description = "Retrieve a specific tenant by its ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponseDto>> getTenantById(@PathVariable Long id) {
        TenantResponseDto tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }
    
    @PostMapping
    @Operation(summary = "Create new tenant", description = "Create a new tenant in the platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponseDto>> createTenant(@Valid @RequestBody TenantCreateDto createDto) {
        TenantResponseDto tenant = tenantService.createTenant(createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tenant, "Tenant created successfully"));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update tenant", description = "Update an existing tenant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponseDto>> updateTenant(
            @PathVariable Long id, 
            @Valid @RequestBody TenantCreateDto updateDto) {
        TenantResponseDto tenant = tenantService.updateTenant(id, updateDto);
        return ResponseEntity.ok(ApiResponse.success(tenant, "Tenant updated successfully"));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tenant", description = "Delete a tenant from the platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant deleted successfully"));
    }
}
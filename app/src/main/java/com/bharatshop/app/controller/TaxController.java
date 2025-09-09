package com.bharatshop.app.controller;

import com.bharatshop.shared.dto.ApiResponse;
import com.bharatshop.shared.dto.TaxRateDto;
import com.bharatshop.shared.dto.TaxCalculationRequest;
import com.bharatshop.shared.dto.TaxCalculationResponse;
import com.bharatshop.shared.entity.TaxRate;
import com.bharatshop.shared.repository.TaxRateRepository;
import com.bharatshop.shared.service.PriceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
@Slf4j
public class TaxController {

    private final TaxRateRepository taxRateRepository;
    private final PriceCalculationService priceCalculationService;

    /**
     * Get all tax rates for a tenant
     */
    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<TaxRateDto>>> getTaxRates(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "hsnCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String hsnCode,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String taxType,
            @RequestParam(required = false) Boolean isActive) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<TaxRate> taxRatesPage;
            
            if (hsnCode != null || stateCode != null || taxType != null || isActive != null) {
                // Apply filters
                taxRatesPage = taxRateRepository.findByTenantIdAndFilters(
                    tenantId, hsnCode, stateCode, taxType, isActive, pageable
                );
            } else {
                taxRatesPage = taxRateRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
            }
            
            List<TaxRateDto> taxRateDtos = taxRatesPage.getContent().stream()
                .map(TaxRateDto::fromEntity)
                .toList();
            
            return ResponseEntity.ok(
                ApiResponse.success(taxRateDtos, "Tax rates retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving tax rates for tenant: {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve tax rates: " + e.getMessage()));
        }
    }

    /**
     * Get tax rate by ID
     */
    @GetMapping("/rates/{taxRateId}")
    public ResponseEntity<ApiResponse<TaxRateDto>> getTaxRate(
            @PathVariable Long taxRateId,
            @RequestParam Long tenantId) {
        try {
            TaxRate taxRate = taxRateRepository.findByIdAndTenantId(taxRateId, tenantId)
                .orElseThrow(() -> new RuntimeException("Tax rate not found"));
            
            TaxRateDto taxRateDto = TaxRateDto.fromEntity(taxRate);
            
            return ResponseEntity.ok(
                ApiResponse.success(taxRateDto, "Tax rate retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving tax rate: {}", taxRateId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Tax rate not found: " + e.getMessage()));
        }
    }

    /**
     * Create new tax rate
     */
    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<TaxRateDto>> createTaxRate(
            @Valid @RequestBody TaxRateDto taxRateDto) {
        try {
            // Check if tax rate already exists
            boolean exists = taxRateRepository.existsByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndDeletedAtIsNull(
                taxRateDto.getTenantId(),
                taxRateDto.getHsnCode(),
                taxRateDto.getStateCode(),
                TaxRate.TaxType.valueOf(taxRateDto.getTaxType())
            );
            
            if (exists) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tax rate already exists for this HSN, state, and tax type combination"));
            }
            
            TaxRate taxRate = TaxRate.builder()
                .tenantId(taxRateDto.getTenantId())
                .hsnCode(taxRateDto.getHsnCode())
                .stateCode(taxRateDto.getStateCode())
                .taxType(TaxRate.TaxType.valueOf(taxRateDto.getTaxType()))
                .cgstRate(taxRateDto.getCgstRate())
                .sgstRate(taxRateDto.getSgstRate())
                .igstRate(taxRateDto.getIgstRate())
                .cessRate(taxRateDto.getCessRate())
                .status(TaxRate.TaxRateStatus.ACTIVE)
                .build();
            
            TaxRate savedTaxRate = taxRateRepository.save(taxRate);
            TaxRateDto savedDto = TaxRateDto.fromEntity(savedTaxRate);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(savedDto, "Tax rate created successfully"));
        } catch (Exception e) {
            log.error("Error creating tax rate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create tax rate: " + e.getMessage()));
        }
    }

    /**
     * Update tax rate
     */
    @PutMapping("/rates/{taxRateId}")
    public ResponseEntity<ApiResponse<TaxRateDto>> updateTaxRate(
            @PathVariable Long taxRateId,
            @RequestParam Long tenantId,
            @Valid @RequestBody TaxRateDto taxRateDto) {
        try {
            TaxRate existingTaxRate = taxRateRepository.findByIdAndTenantId(taxRateId, tenantId)
                .orElseThrow(() -> new RuntimeException("Tax rate not found"));
            
            // Update fields
            existingTaxRate.setHsnCode(taxRateDto.getHsnCode());
            existingTaxRate.setStateCode(taxRateDto.getStateCode());
            existingTaxRate.setTaxType(TaxRate.TaxType.valueOf(taxRateDto.getTaxType()));
            existingTaxRate.setCgstRate(taxRateDto.getCgstRate());
            existingTaxRate.setSgstRate(taxRateDto.getSgstRate());
            existingTaxRate.setIgstRate(taxRateDto.getIgstRate());
            existingTaxRate.setCessRate(taxRateDto.getCessRate());
            
            TaxRate updatedTaxRate = taxRateRepository.save(existingTaxRate);
            TaxRateDto updatedDto = TaxRateDto.fromEntity(updatedTaxRate);
            
            return ResponseEntity.ok(
                ApiResponse.success(updatedDto, "Tax rate updated successfully")
            );
        } catch (Exception e) {
            log.error("Error updating tax rate: {}", taxRateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update tax rate: " + e.getMessage()));
        }
    }

    /**
     * Delete tax rate (soft delete)
     */
    @DeleteMapping("/rates/{taxRateId}")
    public ResponseEntity<ApiResponse<String>> deleteTaxRate(
            @PathVariable Long taxRateId,
            @RequestParam Long tenantId) {
        try {
            TaxRate taxRate = taxRateRepository.findByIdAndTenantId(taxRateId, tenantId)
                .orElseThrow(() -> new RuntimeException("Tax rate not found"));
            
            taxRate.setStatus(TaxRate.TaxRateStatus.INACTIVE);
            taxRateRepository.save(taxRate);
            
            return ResponseEntity.ok(
                ApiResponse.success("Deleted", "Tax rate deleted successfully")
            );
        } catch (Exception e) {
            log.error("Error deleting tax rate: {}", taxRateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete tax rate: " + e.getMessage()));
        }
    }

    /**
     * Calculate tax for given parameters
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<TaxCalculationResponse>> calculateTax(
            @Valid @RequestBody TaxCalculationRequest request) {
        try {
            TaxCalculationResponse response = priceCalculationService.calculateTax(
                request.getTenantId(),
                request.getHsnCode(),
                request.getSellerStateCode(),
                request.getBuyerStateCode(),
                request.getAmount(),
                request.isTaxInclusive()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success(response, "Tax calculated successfully")
            );
        } catch (Exception e) {
            log.error("Error calculating tax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to calculate tax: " + e.getMessage()));
        }
    }

    /**
     * Get applicable tax rate for HSN and state combination
     */
    @GetMapping("/rates/applicable")
    public ResponseEntity<ApiResponse<TaxRateDto>> getApplicableTaxRate(
            @RequestParam Long tenantId,
            @RequestParam String hsnCode,
            @RequestParam String sellerStateCode,
            @RequestParam String buyerStateCode) {
        try {
            boolean isIntraState = sellerStateCode.equals(buyerStateCode);
            TaxRate.TaxType taxType = isIntraState ? TaxRate.TaxType.INTRA_STATE : TaxRate.TaxType.INTER_STATE;
            
            TaxRate taxRate = taxRateRepository.findByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndStatus(
                tenantId, hsnCode, sellerStateCode, taxType, TaxRate.TaxRateStatus.ACTIVE
            ).orElse(null);
            
            if (taxRate == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No applicable tax rate found"));
            }
            
            TaxRateDto taxRateDto = TaxRateDto.fromEntity(taxRate);
            
            return ResponseEntity.ok(
                ApiResponse.success(taxRateDto, "Applicable tax rate retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving applicable tax rate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve tax rate: " + e.getMessage()));
        }
    }

    /**
     * Get unique HSN codes for a tenant
     */
    @GetMapping("/hsn-codes")
    public ResponseEntity<ApiResponse<List<String>>> getHsnCodes(
            @RequestParam Long tenantId) {
        try {
            List<String> hsnCodes = taxRateRepository.findDistinctHsnCodesByTenantIdAndStatusActive(tenantId);
            
            return ResponseEntity.ok(
                ApiResponse.success(hsnCodes, "HSN codes retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving HSN codes for tenant: {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve HSN codes: " + e.getMessage()));
        }
    }

    /**
     * Get unique state codes for a tenant
     */
    @GetMapping("/state-codes")
    public ResponseEntity<ApiResponse<List<String>>> getStateCodes(
            @RequestParam Long tenantId) {
        try {
            List<String> stateCodes = taxRateRepository.findDistinctStateCodesByTenantIdAndStatusActive(tenantId);
            
            return ResponseEntity.ok(
                ApiResponse.success(stateCodes, "State codes retrieved successfully")
            );
        } catch (Exception e) {
            log.error("Error retrieving state codes for tenant: {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve state codes: " + e.getMessage()));
        }
    }

    /**
     * Bulk import tax rates
     */
    @PostMapping("/rates/bulk-import")
    public ResponseEntity<ApiResponse<String>> bulkImportTaxRates(
            @RequestParam Long tenantId,
            @Valid @RequestBody List<TaxRateDto> taxRateDtos) {
        try {
            int imported = 0;
            int skipped = 0;
            
            for (TaxRateDto dto : taxRateDtos) {
                boolean exists = taxRateRepository.existsByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndDeletedAtIsNull(
                dto.getTenantId(), dto.getHsnCode(), dto.getStateCode(), TaxRate.TaxType.valueOf(dto.getTaxType())
            );
                
                if (!exists) {
                    TaxRate taxRate = TaxRate.builder()
                        .tenantId(tenantId)
                        .hsnCode(dto.getHsnCode())
                        .stateCode(dto.getStateCode())
                        .taxType(TaxRate.TaxType.valueOf(dto.getTaxType()))
                        .cgstRate(dto.getCgstRate())
                        .sgstRate(dto.getSgstRate())
                        .igstRate(dto.getIgstRate())
                        .cessRate(dto.getCessRate())
                        .status(TaxRate.TaxRateStatus.ACTIVE)
                        .build();
                    
                    taxRateRepository.save(taxRate);
                    imported++;
                } else {
                    skipped++;
                }
            }
            
            String message = String.format("Bulk import completed. Imported: %d, Skipped: %d", imported, skipped);
            
            return ResponseEntity.ok(
                ApiResponse.success(message, "Bulk import completed successfully")
            );
        } catch (Exception e) {
            log.error("Error during bulk import for tenant: {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to import tax rates: " + e.getMessage()));
        }
    }
}
package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.dto.ProductVariantOptionValueDto;
import com.bharatshop.shared.entity.ProductVariant;
import com.bharatshop.shared.entity.ProductVariantOptionValue;
import com.bharatshop.shared.mapper.ProductVariantMapper;
import com.bharatshop.shared.mapper.ProductVariantOptionValueMapper;
import com.bharatshop.shared.repository.ProductVariantRepository;
import com.bharatshop.shared.repository.ProductVariantOptionValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantOptionValueRepository productVariantOptionValueRepository;
    private final ProductVariantMapper productVariantMapper;
    private final ProductVariantOptionValueMapper productVariantOptionValueMapper;
    private final ProductVariantOptionValueService variantOptionValueService;

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getVariantsByProduct(UUID productId, UUID tenantId) {
        List<ProductVariant> variants = productVariantRepository.findActiveByProductIdAndTenantId(productId, tenantId);
        return variants.stream()
                .map(productVariantMapper::toDtoWithComputedFields)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductVariantDto> getVariantsByProduct(UUID productId, UUID tenantId, Pageable pageable) {
        Page<ProductVariant> variants = productVariantRepository.findActiveByProductIdAndTenantId(productId, tenantId, pageable);
        return variants.map(productVariantMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantById(UUID id, UUID tenantId) {
        return productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .map(productVariantMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getDefaultVariant(UUID productId, UUID tenantId) {
        return productVariantRepository.findDefaultByProductIdAndTenantId(productId, tenantId)
                .map(productVariantMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantBySku(String sku, UUID tenantId) {
        return productVariantRepository.findActiveBySkuAndTenantId(sku, tenantId)
                .map(productVariantMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantByBarcode(String barcode, UUID tenantId) {
        return productVariantRepository.findActiveByBarcodeAndTenantId(barcode, tenantId)
                .map(productVariantMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getVariantsByPriceRange(UUID tenantId, BigDecimal minPrice, BigDecimal maxPrice) {
        List<ProductVariant> variants = productVariantRepository.findActiveByPriceRangeAndTenantId(tenantId, minPrice, maxPrice);
        return variants.stream()
                .map(productVariantMapper::toDtoWithComputedFields)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getLowStockVariants(UUID tenantId, Integer threshold) {
        List<ProductVariant> variants = productVariantRepository.findActiveWithLowStockByTenantId(tenantId, threshold != null ? threshold : 5);
        return variants.stream()
                .map(productVariantMapper::toDtoWithComputedFields)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getOutOfStockVariants(UUID tenantId) {
        List<ProductVariant> variants = productVariantRepository.findActiveWithZeroStockByTenantId(tenantId);
        return variants.stream()
                .map(productVariantMapper::toDtoWithComputedFields)
                .toList();
    }

    public ProductVariantDto createVariant(ProductVariantDto variantDto, UUID tenantId) {
        return createVariant(variantDto, null, tenantId);
    }
    
    public ProductVariantDto createVariant(ProductVariantDto variantDto, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        validateVariant(variantDto);
        
        // Check if SKU already exists
        if (StringUtils.hasText(variantDto.getSku()) && 
            productVariantRepository.existsBySkuAndTenantId(variantDto.getSku(), tenantId)) {
            throw new IllegalArgumentException("SKU '" + variantDto.getSku() + "' already exists");
        }
        
        // Check if barcode already exists
        if (StringUtils.hasText(variantDto.getBarcode()) && 
            productVariantRepository.existsByBarcodeAndTenantId(variantDto.getBarcode(), tenantId)) {
            throw new IllegalArgumentException("Barcode '" + variantDto.getBarcode() + "' already exists");
        }
        
        // Validate option value combination uniqueness if provided
        if (optionValueMap != null && !optionValueMap.isEmpty()) {
            Optional<UUID> existingVariantId = variantOptionValueService.findVariantByOptionValues(variantDto.getProductId(), optionValueMap, tenantId);
            if (existingVariantId.isPresent()) {
                throw new IllegalArgumentException("A variant with this option combination already exists");
            }
        }
        
        ProductVariant variant = productVariantMapper.toEntity(variantDto);
        variant.setTenantId(tenantId);
        variant.setCreatedAt(LocalDateTime.now());
        variant.setUpdatedAt(LocalDateTime.now());
        variant.setDeletedAt(null);
        
        if (variant.getStatus() == null) {
            variant.setStatus(ProductVariant.VariantStatus.ACTIVE);
        }
        
        if (variant.getSortOrder() == null) {
            variant.setSortOrder(0);
        }
        
        // If this is the first variant for the product, make it default
        if (variant.getIsDefault() == null) {
            long variantCount = productVariantRepository.countActiveByProductIdAndTenantId(variant.getProductId(), tenantId);
            variant.setIsDefault(variantCount == 0);
        }
        
        // Ensure only one default variant per product
        if (Boolean.TRUE.equals(variant.getIsDefault())) {
            productVariantRepository.clearDefaultForProduct(variant.getProductId(), tenantId);
        }
        
        log.info("Creating variant with SKU: {} for product: {} and tenant: {}", variant.getSku(), variant.getProductId(), tenantId);
        ProductVariant savedVariant = productVariantRepository.save(variant);
        
        // Set option values if provided
        if (optionValueMap != null && !optionValueMap.isEmpty()) {
            variantOptionValueService.setVariantOptionValues(savedVariant.getId(), optionValueMap, tenantId);
        }
        
        return productVariantMapper.toDtoWithComputedFields(savedVariant);
    }

    public ProductVariantDto updateVariant(UUID id, ProductVariantDto variantDto, UUID tenantId) {
        return updateVariant(id, variantDto, null, tenantId);
    }
    
    public ProductVariantDto updateVariant(UUID id, ProductVariantDto variantDto, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        ProductVariant existingVariant = productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));

        validateVariant(variantDto);
        
        // Check if SKU is being changed and if new SKU already exists
        if (StringUtils.hasText(variantDto.getSku()) && 
            !existingVariant.getSku().equals(variantDto.getSku()) && 
            productVariantRepository.existsBySkuAndTenantId(variantDto.getSku(), tenantId)) {
            throw new IllegalArgumentException("SKU '" + variantDto.getSku() + "' already exists");
        }
        
        // Check if barcode is being changed and if new barcode already exists
        if (StringUtils.hasText(variantDto.getBarcode()) && 
            !existingVariant.getBarcode().equals(variantDto.getBarcode()) && 
            productVariantRepository.existsByBarcodeAndTenantId(variantDto.getBarcode(), tenantId)) {
            throw new IllegalArgumentException("Barcode '" + variantDto.getBarcode() + "' already exists");
        }
        
        // Validate option value combination uniqueness if provided
        if (optionValueMap != null && !optionValueMap.isEmpty()) {
            if (!variantOptionValueService.isVariantCombinationUnique(id, optionValueMap, tenantId)) {
                throw new IllegalArgumentException("A variant with this option combination already exists");
            }
        }
        
        // Handle default variant logic
        if (Boolean.TRUE.equals(variantDto.getIsDefault()) && !Boolean.TRUE.equals(existingVariant.getIsDefault())) {
            productVariantRepository.clearDefaultForProduct(existingVariant.getProductId(), tenantId);
        }
        
        productVariantMapper.updateEntity(variantDto, existingVariant);
        existingVariant.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating variant with SKU: {} for tenant: {}", existingVariant.getSku(), tenantId);
        ProductVariant savedVariant = productVariantRepository.save(existingVariant);
        
        // Update option values if provided
        if (optionValueMap != null && !optionValueMap.isEmpty()) {
            variantOptionValueService.setVariantOptionValues(id, optionValueMap, tenantId);
        }
        
        return productVariantMapper.toDtoWithComputedFields(savedVariant);
    }

    public void deleteVariant(UUID id, UUID tenantId) {
        ProductVariant variant = productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));
        
        // Remove all option values for this variant
        variantOptionValueService.removeAllOptionValuesFromVariant(id, tenantId);
        
        // If this is the default variant, we need to set another variant as default
        if (Boolean.TRUE.equals(variant.getIsDefault())) {
            List<ProductVariant> otherVariants = productVariantRepository.findActiveByProductIdAndTenantId(variant.getProductId(), tenantId)
                    .stream()
                    .filter(v -> !v.getId().equals(id))
                    .toList();
            
            if (!otherVariants.isEmpty()) {
                ProductVariant newDefault = otherVariants.get(0);
                newDefault.setIsDefault(true);
                newDefault.setUpdatedAt(LocalDateTime.now());
                productVariantRepository.save(newDefault);
            }
        }
        
        variant.setDeletedAt(LocalDateTime.now());
        variant.setUpdatedAt(LocalDateTime.now());
        
        log.info("Deleting variant with SKU: {} for tenant: {}", variant.getSku(), tenantId);
        productVariantRepository.save(variant);
    }

    public void deleteVariantsByProduct(UUID productId, UUID tenantId) {
        // Get all variant IDs for this product
        List<UUID> variantIds = productVariantRepository.findActiveByProductIdAndTenantId(productId, tenantId)
                .stream()
                .map(ProductVariant::getId)
                .toList();
        
        // Remove all option values for these variants
        for (UUID variantId : variantIds) {
            variantOptionValueService.removeAllOptionValuesFromVariant(variantId, tenantId);
        }
        
        log.info("Soft deleting all variants for product: {} and tenant: {}", productId, tenantId);
        productVariantRepository.softDeleteByProductIdAndTenantId(productId, tenantId);
    }

    public ProductVariantDto updateVariantStock(UUID id, Integer stock, UUID tenantId) {
        ProductVariant variant = productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));
        
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        
        variant.setStock(stock);
        variant.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating variant stock: {} to {} for tenant: {}", variant.getSku(), stock, tenantId);
        ProductVariant savedVariant = productVariantRepository.save(variant);
        return productVariantMapper.toDtoWithComputedFields(savedVariant);
    }

    public ProductVariantDto incrementStock(UUID id, Integer quantity, UUID tenantId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        int updatedRows = productVariantRepository.incrementStock(id, tenantId, quantity);
        if (updatedRows == 0) {
            throw new RuntimeException("Variant not found with id: " + id);
        }
        
        ProductVariant variant = productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));
        
        log.info("Incremented variant stock: {} by {} for tenant: {}", variant.getSku(), quantity, tenantId);
        return productVariantMapper.toDtoWithComputedFields(variant);
    }

    public ProductVariantDto decrementStock(UUID id, Integer quantity, UUID tenantId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        int updatedRows = productVariantRepository.decrementStock(id, tenantId, quantity);
        if (updatedRows == 0) {
            throw new RuntimeException("Variant not found or insufficient stock for id: " + id);
        }
        
        ProductVariant variant = productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));
        
        log.info("Decremented variant stock: {} by {} for tenant: {}", variant.getSku(), quantity, tenantId);
        return productVariantMapper.toDtoWithComputedFields(variant);
    }

    public ProductVariantDto setAsDefault(UUID id, UUID tenantId) {
        ProductVariant variant = productVariantRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));
        
        // Clear existing default for this product
        productVariantRepository.clearDefaultForProduct(variant.getProductId(), tenantId);
        
        variant.setIsDefault(true);
        variant.setUpdatedAt(LocalDateTime.now());
        
        log.info("Setting variant as default: {} for tenant: {}", variant.getSku(), tenantId);
        ProductVariant savedVariant = productVariantRepository.save(variant);
        return productVariantMapper.toDtoWithComputedFields(savedVariant);
    }

    public ProductVariantDto setDefaultVariant(UUID variantId, UUID tenantId) {
        return setAsDefault(variantId, tenantId);
    }

    public ProductVariantDto updateStock(UUID variantId, Integer stock, UUID tenantId) {
        ProductVariant variant = productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + variantId));
        
        variant.setStock(stock);
        variant.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updated variant stock: {} to {} for tenant: {}", variant.getSku(), stock, tenantId);
        ProductVariant savedVariant = productVariantRepository.save(variant);
        return productVariantMapper.toDtoWithComputedFields(savedVariant);
    }

    @Transactional(readOnly = true)
    public long getVariantCount(UUID productId, UUID tenantId) {
        return productVariantRepository.countActiveByProductIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getTotalVariantCount(UUID tenantId) {
        return productVariantRepository.countActiveByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Integer getTotalStock(UUID productId, UUID tenantId) {
        return productVariantRepository.getTotalStockByProductIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMinPrice(UUID productId, UUID tenantId) {
        return productVariantRepository.getMinPriceByProductIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMaxPrice(UUID productId, UUID tenantId) {
        return productVariantRepository.getMaxPriceByProductIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isSkuUnique(String sku, UUID tenantId) {
        return !productVariantRepository.existsBySkuAndTenantId(sku, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isBarcodeUnique(String barcode, UUID tenantId) {
        return !productVariantRepository.existsByBarcodeAndTenantId(barcode, tenantId);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValues(UUID variantId, UUID tenantId) {
        return variantOptionValueService.getVariantOptionValues(variantId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findVariantByOptionValues(UUID productId, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        return variantOptionValueService.findVariantByOptionValues(productId, optionValueMap, tenantId);
    }

    public List<ProductVariantOptionValueDto> setVariantOptionValues(UUID variantId, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        return variantOptionValueService.setVariantOptionValues(variantId, optionValueMap, tenantId);
    }

    public ProductVariantOptionValueDto addOptionValueToVariant(UUID variantId, UUID optionId, UUID optionValueId, UUID tenantId) {
        return variantOptionValueService.addOptionValueToVariant(variantId, optionId, optionValueId, tenantId);
    }

    public void removeOptionValueFromVariant(UUID variantId, UUID optionId, UUID tenantId) {
        variantOptionValueService.removeOptionValueFromVariant(variantId, optionId, tenantId);
    }

    private void validateVariant(ProductVariantDto variantDto) {
        if (variantDto.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        
        if (variantDto.getPrice() == null || variantDto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        
        if (variantDto.getStock() == null || variantDto.getStock() < 0) {
            throw new IllegalArgumentException("Stock must be non-negative");
        }
        
        if (variantDto.getSalePrice() != null && 
            variantDto.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sale price must be non-negative");
        }
        
        if (variantDto.getSalePrice() != null && 
            variantDto.getSalePrice().compareTo(variantDto.getPrice()) > 0) {
            throw new IllegalArgumentException("Sale price cannot be greater than regular price");
        }
        
        if (variantDto.getWeight() != null && variantDto.getWeight().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight must be non-negative");
        }
        
        if (variantDto.getSortOrder() != null && variantDto.getSortOrder() < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
    }
}
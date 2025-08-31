package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.ProductVariantOptionValueDto;
import com.bharatshop.shared.entity.ProductVariantOptionValue;
import com.bharatshop.shared.mapper.ProductVariantOptionValueMapper;
import com.bharatshop.shared.repository.ProductVariantOptionValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductVariantOptionValueService {

    private final ProductVariantOptionValueRepository variantOptionValueRepository;
    private final ProductVariantOptionValueMapper variantOptionValueMapper;

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValues(UUID variantId, UUID tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdAndTenantId(variantId, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductVariantOptionValueDto> getVariantOptionValues(UUID variantId, UUID tenantId, Pageable pageable) {
        Page<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdAndTenantId(variantId, tenantId, pageable);
        return variantOptionValues.map(variantOptionValueMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantOptionValueDto> getVariantOptionValue(UUID variantId, UUID optionId, UUID tenantId) {
        return variantOptionValueRepository.findByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)
                .map(variantOptionValueMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValuesByOption(UUID optionId, UUID tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByOptionIdAndTenantId(optionId, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValuesByOptionValue(UUID optionValueId, UUID tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByOptionValueIdAndTenantId(optionValueId, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValuesByVariants(List<UUID> variantIds, UUID tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdsAndTenantId(variantIds, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<ProductVariantOptionValueDto>> getVariantOptionValuesGroupedByVariant(List<UUID> variantIds, UUID tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdsAndTenantId(variantIds, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.groupingBy(ProductVariantOptionValueDto::getVariantId));
    }

    public ProductVariantOptionValueDto addOptionValueToVariant(UUID variantId, UUID optionId, UUID optionValueId, UUID tenantId) {
        // Check if this option is already assigned to this variant
        if (variantOptionValueRepository.existsByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)) {
            throw new IllegalArgumentException("Option is already assigned to this variant");
        }
        
        ProductVariantOptionValue variantOptionValue = new ProductVariantOptionValue();
        variantOptionValue.setVariantId(variantId);
        variantOptionValue.setOptionId(optionId);
        variantOptionValue.setOptionValueId(optionValueId);
        variantOptionValue.setTenantId(tenantId);
        variantOptionValue.setCreatedAt(LocalDateTime.now());
        variantOptionValue.setUpdatedAt(LocalDateTime.now());
        
        log.info("Adding option value: {} for option: {} to variant: {} for tenant: {}", optionValueId, optionId, variantId, tenantId);
        ProductVariantOptionValue savedVariantOptionValue = variantOptionValueRepository.save(variantOptionValue);
        return variantOptionValueMapper.toDtoWithComputedFields(savedVariantOptionValue);
    }

    public ProductVariantOptionValueDto updateVariantOptionValue(UUID variantId, UUID optionId, UUID optionValueId, UUID tenantId) {
        ProductVariantOptionValue existingVariantOptionValue = variantOptionValueRepository.findByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant option value not found"));

        existingVariantOptionValue.setOptionValueId(optionValueId);
        existingVariantOptionValue.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating variant option value for variant: {} and option: {} to option value: {} for tenant: {}", variantId, optionId, optionValueId, tenantId);
        ProductVariantOptionValue savedVariantOptionValue = variantOptionValueRepository.save(existingVariantOptionValue);
        return variantOptionValueMapper.toDtoWithComputedFields(savedVariantOptionValue);
    }

    public void removeOptionValueFromVariant(UUID variantId, UUID optionId, UUID tenantId) {
        ProductVariantOptionValue variantOptionValue = variantOptionValueRepository.findByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant option value not found"));
        
        log.info("Removing option: {} from variant: {} for tenant: {}", optionId, variantId, tenantId);
        variantOptionValueRepository.delete(variantOptionValue);
    }

    public void removeAllOptionValuesFromVariant(UUID variantId, UUID tenantId) {
        log.info("Removing all option values from variant: {} for tenant: {}", variantId, tenantId);
        variantOptionValueRepository.deleteByVariantIdAndTenantId(variantId, tenantId);
    }

    public void removeVariantsFromOptionValue(UUID optionValueId, UUID tenantId) {
        log.info("Removing all variants from option value: {} for tenant: {}", optionValueId, tenantId);
        variantOptionValueRepository.deleteByOptionValueIdAndTenantId(optionValueId, tenantId);
    }

    public void removeVariantsFromOption(UUID optionId, UUID tenantId) {
        log.info("Removing all variants from option: {} for tenant: {}", optionId, tenantId);
        variantOptionValueRepository.deleteByOptionIdAndTenantId(optionId, tenantId);
    }

    public List<ProductVariantOptionValueDto> setVariantOptionValues(UUID variantId, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        // Remove existing option values for this variant
        removeAllOptionValuesFromVariant(variantId, tenantId);
        
        // Add new option values
        List<ProductVariantOptionValue> variantOptionValues = optionValueMap.entrySet().stream()
                .map(entry -> {
                    ProductVariantOptionValue variantOptionValue = new ProductVariantOptionValue();
                    variantOptionValue.setVariantId(variantId);
                    variantOptionValue.setOptionId(entry.getKey());
                    variantOptionValue.setOptionValueId(entry.getValue());
                    variantOptionValue.setTenantId(tenantId);
                    variantOptionValue.setCreatedAt(LocalDateTime.now());
                    variantOptionValue.setUpdatedAt(LocalDateTime.now());
                    return variantOptionValue;
                })
                .collect(Collectors.toList());
        
        log.info("Setting {} option values for variant: {} for tenant: {}", variantOptionValues.size(), variantId, tenantId);
        List<ProductVariantOptionValue> savedVariantOptionValues = variantOptionValueRepository.saveAll(variantOptionValues);
        return savedVariantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getVariantOptionValueCount(UUID variantId, UUID tenantId) {
        return variantOptionValueRepository.countByVariantIdAndTenantId(variantId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionVariantCount(UUID optionId, UUID tenantId) {
        return variantOptionValueRepository.countByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionValueVariantCount(UUID optionValueId, UUID tenantId) {
        return variantOptionValueRepository.countByOptionValueIdAndTenantId(optionValueId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getVariantIdsByOption(UUID optionId, UUID tenantId) {
        return variantOptionValueRepository.findVariantIdsByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getVariantIdsByOptionValue(UUID optionValueId, UUID tenantId) {
        return variantOptionValueRepository.findVariantIdsByOptionValueIdAndTenantId(optionValueId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean hasOptionValue(UUID variantId, UUID optionId, UUID tenantId) {
        return variantOptionValueRepository.existsByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findVariantByOptionValues(UUID productId, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        if (optionValueMap.isEmpty()) {
            return Optional.empty();
        }
        
        return variantOptionValueRepository.findVariantByOptionValues(productId, optionValueMap, optionValueMap.size(), tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isVariantCombinationUnique(UUID variantId, Map<UUID, UUID> optionValueMap, UUID tenantId) {
        if (optionValueMap.isEmpty()) {
            return true;
        }
        
        Optional<UUID> existingVariantId = variantOptionValueRepository.findVariantByOptionValues(null, optionValueMap, optionValueMap.size(), tenantId);
        return existingVariantId.isEmpty() || existingVariantId.get().equals(variantId);
    }
}
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductVariantOptionValueService {

    private final ProductVariantOptionValueRepository variantOptionValueRepository;
    private final ProductVariantOptionValueMapper variantOptionValueMapper;

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValues(Long variantId, Long tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdAndTenantId(variantId, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductVariantOptionValueDto> getVariantOptionValues(Long variantId, Long tenantId, Pageable pageable) {
        Page<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdAndTenantId(variantId, tenantId, pageable);
        return variantOptionValues.map(variantOptionValueMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantOptionValueDto> getVariantOptionValue(Long variantId, Long optionId, Long tenantId) {
        return variantOptionValueRepository.findByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)
                .map(variantOptionValueMapper::toDtoWithComputedFields);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValuesByOption(Long optionId, Long tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByOptionIdAndTenantId(optionId, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValuesByOptionValue(Long optionValueId, Long tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByOptionValueIdAndTenantId(optionValueId, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductVariantOptionValueDto> getVariantOptionValuesByVariants(List<Long> variantIds, Long tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdsAndTenantId(variantIds, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ProductVariantOptionValueDto>> getVariantOptionValuesGroupedByVariant(List<Long> variantIds, Long tenantId) {
        List<ProductVariantOptionValue> variantOptionValues = variantOptionValueRepository.findByVariantIdsAndTenantId(variantIds, tenantId);
        return variantOptionValues.stream()
                .map(variantOptionValueMapper::toDtoWithComputedFields)
                .collect(Collectors.groupingBy(ProductVariantOptionValueDto::getVariantId));
    }

    public ProductVariantOptionValueDto addOptionValueToVariant(Long variantId, Long optionId, Long optionValueId, Long tenantId) {
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

    public ProductVariantOptionValueDto updateVariantOptionValue(Long variantId, Long optionId, Long optionValueId, Long tenantId) {
        ProductVariantOptionValue existingVariantOptionValue = variantOptionValueRepository.findByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant option value not found"));

        existingVariantOptionValue.setOptionValueId(optionValueId);
        existingVariantOptionValue.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating variant option value for variant: {} and option: {} to option value: {} for tenant: {}", variantId, optionId, optionValueId, tenantId);
        ProductVariantOptionValue savedVariantOptionValue = variantOptionValueRepository.save(existingVariantOptionValue);
        return variantOptionValueMapper.toDtoWithComputedFields(savedVariantOptionValue);
    }

    public void removeOptionValueFromVariant(Long variantId, Long optionId, Long tenantId) {
        ProductVariantOptionValue variantOptionValue = variantOptionValueRepository.findByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Variant option value not found"));
        
        log.info("Removing option: {} from variant: {} for tenant: {}", optionId, variantId, tenantId);
        variantOptionValueRepository.delete(variantOptionValue);
    }

    public void removeAllOptionValuesFromVariant(Long variantId, Long tenantId) {
        log.info("Removing all option values from variant: {} for tenant: {}", variantId, tenantId);
        variantOptionValueRepository.deleteByVariantIdAndTenantId(variantId, tenantId);
    }

    public void removeVariantsFromOptionValue(Long optionValueId, Long tenantId) {
        log.info("Removing all variants from option value: {} for tenant: {}", optionValueId, tenantId);
        variantOptionValueRepository.deleteByOptionValueIdAndTenantId(optionValueId, tenantId);
    }

    public void removeVariantsFromOption(Long optionId, Long tenantId) {
        log.info("Removing all variants from option: {} for tenant: {}", optionId, tenantId);
        variantOptionValueRepository.deleteByOptionIdAndTenantId(optionId, tenantId);
    }

    public List<ProductVariantOptionValueDto> setVariantOptionValues(Long variantId, Map<Long, Long> optionValueMap, Long tenantId) {
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
    public long getVariantOptionValueCount(Long variantId, Long tenantId) {
        return variantOptionValueRepository.countByVariantIdAndTenantId(variantId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionVariantCount(Long optionId, Long tenantId) {
        return variantOptionValueRepository.countByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionValueVariantCount(Long optionValueId, Long tenantId) {
        return variantOptionValueRepository.countByOptionValueIdAndTenantId(optionValueId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Long> getVariantIdsByOption(Long optionId, Long tenantId) {
        return variantOptionValueRepository.findVariantIdsByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Long> getVariantIdsByOptionValue(Long optionValueId, Long tenantId) {
        return variantOptionValueRepository.findVariantIdsByOptionValueIdAndTenantId(optionValueId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean hasOptionValue(Long variantId, Long optionId, Long tenantId) {
        return variantOptionValueRepository.existsByVariantIdAndOptionIdAndTenantId(variantId, optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findVariantByOptionValues(Long productId, Map<Long, Long> optionValueMap, Long tenantId) {
        if (optionValueMap.isEmpty()) {
            return Optional.empty();
        }
        
        return variantOptionValueRepository.findVariantByOptionValues(productId, optionValueMap.values(), optionValueMap.size(), tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isVariantCombinationUnique(Long variantId, Map<Long, Long> optionValueMap, Long tenantId) {
        if (optionValueMap.isEmpty()) {
            return true;
        }
        
        Optional<Long> existingVariantId = variantOptionValueRepository.findVariantByOptionValues(null, optionValueMap.values(), optionValueMap.size(), tenantId);
        return existingVariantId.isEmpty() || existingVariantId.get().equals(variantId);
    }
}
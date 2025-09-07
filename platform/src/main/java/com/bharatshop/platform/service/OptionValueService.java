package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.OptionValueDto;
import com.bharatshop.shared.entity.OptionValue;
import com.bharatshop.shared.mapper.OptionValueMapper;
import com.bharatshop.shared.repository.OptionValueRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OptionValueService {

    private static final Logger log = LoggerFactory.getLogger(OptionValueService.class);

    private final OptionValueRepository optionValueRepository;
    private final OptionValueMapper optionValueMapper;

    @Transactional(readOnly = true)
    public List<OptionValueDto> getOptionValuesByOption(Long optionId, Long tenantId) {
        List<OptionValue> optionValues = optionValueRepository.findActiveByOptionIdAndTenantId(optionId, tenantId);
        return optionValueMapper.toDtoList(optionValues);
    }

    @Transactional(readOnly = true)
    public Page<OptionValueDto> getOptionValuesByOption(Long optionId, Long tenantId, Pageable pageable) {
        Page<OptionValue> optionValues = optionValueRepository.findActiveByOptionIdAndTenantId(optionId, tenantId, pageable);
        return optionValues.map(optionValueMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OptionValueDto> getOptionValueById(Long id, Long tenantId) {
        return optionValueRepository.findActiveByIdAndTenantId(id, tenantId)
                .map(optionValueMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OptionValueDto> getOptionValueByValue(Long optionId, String value, Long tenantId) {
        return optionValueRepository.findActiveByOptionIdAndValueAndTenantId(optionId, value, tenantId)
                .map(optionValueMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<OptionValueDto> getOptionValuesByIds(List<Long> ids, Long tenantId) {
        List<OptionValue> optionValues = optionValueRepository.findActiveByIdsAndTenantId(ids, tenantId);
        return optionValueMapper.toDtoList(optionValues);
    }

    @Transactional(readOnly = true)
    public Page<OptionValueDto> searchOptionValues(Long optionId, String keyword, Long tenantId, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return getOptionValuesByOption(optionId, tenantId, pageable);
        }
        Page<OptionValue> optionValues = optionValueRepository.searchActiveByOptionIdAndKeyword(optionId, tenantId, keyword.trim(), pageable);
        return optionValues.map(optionValueMapper::toDto);
    }

    public OptionValueDto createOptionValue(OptionValueDto optionValueDto, Long tenantId) {
        validateOptionValue(optionValueDto);
        
        // Check if option value already exists for this option
        if (optionValueRepository.existsByOptionIdAndValueAndTenantId(optionValueDto.getOptionId(), optionValueDto.getValue(), tenantId)) {
            throw new IllegalArgumentException("Option value '" + optionValueDto.getValue() + "' already exists for this option");
        }
        
        OptionValue optionValue = optionValueMapper.toEntity(optionValueDto);
        optionValue.setTenantId(tenantId);
        optionValue.setCreatedAt(LocalDateTime.now());
        optionValue.setUpdatedAt(LocalDateTime.now());
        optionValue.setDeletedAt(null);
        
        if (optionValue.getIsActive() == null) {
            optionValue.setIsActive(true);
        }
        
        if (optionValue.getSortOrder() == null) {
            optionValue.setSortOrder(0);
        }
        
        log.info("Creating option value: {} for option: {} and tenant: {}", optionValue.getValue(), optionValue.getOptionId(), tenantId);
        OptionValue savedOptionValue = optionValueRepository.save(optionValue);
        return optionValueMapper.toDto(savedOptionValue);
    }

    public OptionValueDto updateOptionValue(Long id, OptionValueDto optionValueDto, Long tenantId) {
        OptionValue existingOptionValue = optionValueRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Option value not found with id: " + id));

        validateOptionValue(optionValueDto);
        
        // Check if value is being changed and if new value already exists for this option
        if (!existingOptionValue.getValue().equals(optionValueDto.getValue()) && 
            optionValueRepository.existsByOptionIdAndValueAndTenantId(existingOptionValue.getOptionId(), optionValueDto.getValue(), tenantId)) {
            throw new IllegalArgumentException("Option value '" + optionValueDto.getValue() + "' already exists for this option");
        }
        
        optionValueMapper.updateEntity(optionValueDto, existingOptionValue);
        existingOptionValue.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating option value: {} for tenant: {}", existingOptionValue.getValue(), tenantId);
        OptionValue savedOptionValue = optionValueRepository.save(existingOptionValue);
        return optionValueMapper.toDto(savedOptionValue);
    }

    public void deleteOptionValue(Long id, Long tenantId) {
        OptionValue optionValue = optionValueRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Option value not found with id: " + id));
        
        optionValue.setDeletedAt(LocalDateTime.now());
        optionValue.setUpdatedAt(LocalDateTime.now());
        
        log.info("Deleting option value: {} for tenant: {}", optionValue.getValue(), tenantId);
        optionValueRepository.save(optionValue);
    }

    public void deleteOptionValuesByOption(Long optionId, Long tenantId) {
        log.info("Soft deleting all option values for option: {} and tenant: {}", optionId, tenantId);
        optionValueRepository.softDeleteByOptionIdAndTenantId(optionId, tenantId);
    }

    public OptionValueDto updateOptionValueStatus(Long id, boolean isActive, Long tenantId) {
        OptionValue optionValue = optionValueRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Option value not found with id: " + id));
        
        optionValue.setIsActive(isActive);
        optionValue.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating option value status: {} to {} for tenant: {}", optionValue.getValue(), isActive, tenantId);
        OptionValue savedOptionValue = optionValueRepository.save(optionValue);
        return optionValueMapper.toDto(savedOptionValue);
    }

    @Transactional(readOnly = true)
    public long getOptionValueCount(Long optionId, Long tenantId) {
        return optionValueRepository.countActiveByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getTotalOptionValueCount(Long tenantId) {
        return optionValueRepository.countActiveByTenantId(tenantId);
    }

    private void validateOptionValue(OptionValueDto optionValueDto) {
        if (optionValueDto.getOptionId() == null) {
            throw new IllegalArgumentException("Option ID is required");
        }
        
        if (!StringUtils.hasText(optionValueDto.getValue())) {
            throw new IllegalArgumentException("Option value is required");
        }
        
        if (optionValueDto.getSortOrder() != null && optionValueDto.getSortOrder() < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
    }
}
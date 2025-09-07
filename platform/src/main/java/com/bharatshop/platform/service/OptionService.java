package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.OptionDto;
import com.bharatshop.shared.entity.Option;
import com.bharatshop.shared.mapper.OptionMapper;
import com.bharatshop.shared.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OptionService {

    private final OptionRepository optionRepository;
    private final OptionMapper optionMapper;

    @Transactional(readOnly = true)
    public List<OptionDto> getAllOptionsByTenant(Long tenantId) {
        List<Option> options = optionRepository.findAllActiveByTenantId(tenantId);
        return optionMapper.toDtoList(options);
    }

    @Transactional(readOnly = true)
    public Page<OptionDto> getAllOptionsByTenant(Long tenantId, Pageable pageable) {
        Page<Option> options = optionRepository.findAllActiveByTenantId(tenantId, pageable);
        return options.map(optionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OptionDto> getAllOptions(Long tenantId, Pageable pageable) {
        return getAllOptionsByTenant(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<OptionDto> getOptionById(Long id, Long tenantId) {
        return optionRepository.findActiveByIdAndTenantId(id, tenantId)
                .map(optionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OptionDto> getOptionByName(String name, Long tenantId) {
        return optionRepository.findActiveByNameAndTenantId(name, tenantId)
                .map(optionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<OptionDto> getOptionsByType(Long tenantId, Option.OptionType type) {
        List<Option> options = optionRepository.findActiveByTenantIdAndType(tenantId, type.name());
        return optionMapper.toDtoList(options);
    }

    @Transactional(readOnly = true)
    public Page<OptionDto> searchOptions(Long tenantId, String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return getAllOptionsByTenant(tenantId, pageable);
        }
        Page<Option> options = optionRepository.searchActiveByTenantIdAndKeywordWithPagination(tenantId, keyword.trim(), pageable);
        return options.map(optionMapper::toDto);
    }

    public OptionDto createOption(OptionDto optionDto, Long tenantId) {
        validateOption(optionDto);
        
        // Check if name already exists for this tenant
        if (optionRepository.existsByNameAndTenantId(optionDto.getName(), tenantId)) {
            throw new IllegalArgumentException("Option with name '" + optionDto.getName() + "' already exists");
        }
        
        Option option = optionMapper.toEntity(optionDto);
        option.setTenantId(tenantId);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        option.setDeletedAt(null);
        
        if (option.getIsActive() == null) {
            option.setIsActive(true);
        }
        
        if (option.getSortOrder() == null) {
            option.setSortOrder(0);
        }
        
        log.info("Creating option: {} for tenant: {}", option.getName(), tenantId);
        Option savedOption = optionRepository.save(option);
        return optionMapper.toDto(savedOption);
    }

    public OptionDto updateOption(Long id, OptionDto optionDto, Long tenantId) {
        Option existingOption = optionRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Option not found with id: " + id));

        validateOption(optionDto);
        
        // Check if name is being changed and if new name already exists
        if (!existingOption.getName().equals(optionDto.getName()) && 
            optionRepository.existsByNameAndTenantId(optionDto.getName(), tenantId)) {
            throw new IllegalArgumentException("Option with name '" + optionDto.getName() + "' already exists");
        }
        
        optionMapper.updateEntity(optionDto, existingOption);
        existingOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating option: {} for tenant: {}", existingOption.getName(), tenantId);
        Option savedOption = optionRepository.save(existingOption);
        return optionMapper.toDto(savedOption);
    }

    public void deleteOption(Long id, Long tenantId) {
        Option option = optionRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Option not found with id: " + id));
        
        option.setDeletedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        
        log.info("Deleting option: {} for tenant: {}", option.getName(), tenantId);
        optionRepository.save(option);
    }

    public OptionDto updateOptionStatus(Long id, boolean isActive, Long tenantId) {
        Option option = optionRepository.findActiveByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Option not found with id: " + id));
        
        option.setIsActive(isActive);
        option.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating option status: {} to {} for tenant: {}", option.getName(), isActive, tenantId);
        Option savedOption = optionRepository.save(option);
        return optionMapper.toDto(savedOption);
    }

    @Transactional(readOnly = true)
    public long getOptionCount(Long tenantId) {
        return optionRepository.countActiveByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionCountByType(Long tenantId, Option.OptionType type) {
        return optionRepository.countActiveByTenantIdAndType(tenantId, type.name());
    }

    private void validateOption(OptionDto optionDto) {
        if (!StringUtils.hasText(optionDto.getName())) {
            throw new IllegalArgumentException("Option name is required");
        }
        
        if (optionDto.getType() == null) {
            throw new IllegalArgumentException("Option type is required");
        }
        
        if (optionDto.getSortOrder() != null && optionDto.getSortOrder() < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
    }
}
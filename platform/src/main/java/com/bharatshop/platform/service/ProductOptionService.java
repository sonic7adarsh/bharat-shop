package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.ProductOptionDto;
import com.bharatshop.shared.entity.ProductOption;
import com.bharatshop.shared.mapper.ProductOptionMapper;
import com.bharatshop.shared.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductOptionService {

    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionMapper productOptionMapper;

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptions(UUID productId, UUID tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveByProductIdAndTenantId(productId, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    @Transactional(readOnly = true)
    public Page<ProductOptionDto> getProductOptions(UUID productId, UUID tenantId, Pageable pageable) {
        Page<ProductOption> productOptions = productOptionRepository.findActiveByProductIdAndTenantId(productId, tenantId, pageable);
        return productOptions.map(productOptionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductOptionDto> getProductOption(UUID productId, UUID optionId, UUID tenantId) {
        return productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .map(productOptionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getRequiredProductOptions(UUID productId, UUID tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveRequiredByProductIdAndTenantId(productId, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptionsByOption(UUID optionId, UUID tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveByOptionIdAndTenantId(optionId, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptionsByProducts(List<UUID> productIds, UUID tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveByProductIdsAndTenantId(productIds, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    public ProductOptionDto addOptionToProduct(UUID productId, UUID optionId, boolean isRequired, Integer sortOrder, UUID tenantId) {
        // Check if option is already assigned to this product
        if (productOptionRepository.existsByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)) {
            throw new IllegalArgumentException("Option is already assigned to this product");
        }
        
        ProductOption productOption = new ProductOption();
        productOption.setProductId(productId);
        productOption.setOptionId(optionId);
        productOption.setIsRequired(isRequired);
        productOption.setSortOrder(sortOrder != null ? sortOrder : 0);
        productOption.setTenantId(tenantId);
        productOption.setCreatedAt(LocalDateTime.now());
        productOption.setUpdatedAt(LocalDateTime.now());
        productOption.setDeletedAt(null);
        
        log.info("Adding option: {} to product: {} for tenant: {}", optionId, productId, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(productOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    public ProductOptionDto updateProductOption(UUID productId, UUID optionId, ProductOptionDto productOptionDto, UUID tenantId) {
        ProductOption existingProductOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));

        productOptionMapper.updateEntity(productOptionDto, existingProductOption);
        existingProductOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating product option for product: {} and option: {} for tenant: {}", productId, optionId, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(existingProductOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    public void removeOptionFromProduct(UUID productId, UUID optionId, UUID tenantId) {
        ProductOption productOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        
        productOption.setDeletedAt(LocalDateTime.now());
        productOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Removing option: {} from product: {} for tenant: {}", optionId, productId, tenantId);
        productOptionRepository.save(productOption);
    }

    public void removeAllOptionsFromProduct(UUID productId, UUID tenantId) {
        log.info("Soft deleting all options for product: {} and tenant: {}", productId, tenantId);
        productOptionRepository.softDeleteByProductIdAndTenantId(productId, tenantId);
    }

    public void removeProductsFromOption(UUID optionId, UUID tenantId) {
        log.info("Soft deleting all products for option: {} and tenant: {}", optionId, tenantId);
        productOptionRepository.softDeleteByOptionIdAndTenantId(optionId, tenantId);
    }

    public ProductOptionDto updateRequiredStatus(UUID productId, UUID optionId, boolean isRequired, UUID tenantId) {
        ProductOption productOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        
        productOption.setIsRequired(isRequired);
        productOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating required status for product: {} and option: {} to {} for tenant: {}", productId, optionId, isRequired, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(productOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    public ProductOptionDto updateSortOrder(UUID productId, UUID optionId, Integer sortOrder, UUID tenantId) {
        ProductOption productOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        
        productOption.setSortOrder(sortOrder);
        productOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating sort order for product: {} and option: {} to {} for tenant: {}", productId, optionId, sortOrder, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(productOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    @Transactional(readOnly = true)
    public long getProductOptionCount(UUID productId, UUID tenantId) {
        return productOptionRepository.countActiveByProductIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionProductCount(UUID optionId, UUID tenantId) {
        return productOptionRepository.countActiveByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getProductIdsByOption(UUID optionId, UUID tenantId) {
        return productOptionRepository.findProductIdsByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredOptions(UUID productId, UUID tenantId) {
        return productOptionRepository.countActiveRequiredByProductIdAndTenantId(productId, tenantId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isOptionAssignedToProduct(UUID productId, UUID optionId, UUID tenantId) {
        return productOptionRepository.existsByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId);
    }
}
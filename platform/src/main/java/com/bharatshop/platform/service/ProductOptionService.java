package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.ProductOptionDto;
import com.bharatshop.shared.entity.ProductOption;
import com.bharatshop.shared.mapper.ProductOptionMapper;
import com.bharatshop.shared.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductOptionService {

    private static final Logger log = LoggerFactory.getLogger(ProductOptionService.class);

    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionMapper productOptionMapper;

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptions(Long productId, Long tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveByProductIdAndTenantId(productId, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    @Transactional(readOnly = true)
    public Page<ProductOptionDto> getProductOptions(Long productId, Long tenantId, Pageable pageable) {
        Page<ProductOption> productOptions = productOptionRepository.findActiveByProductIdAndTenantId(productId, tenantId, pageable);
        return productOptions.map(productOptionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductOptionDto> getProductOption(Long productId, Long optionId, Long tenantId) {
        return productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .map(productOptionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getRequiredProductOptions(Long productId, Long tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveRequiredByProductIdAndTenantId(productId, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptionsByOption(Long optionId, Long tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveByOptionIdAndTenantId(optionId, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptionsByProducts(List<Long> productIds, Long tenantId) {
        List<ProductOption> productOptions = productOptionRepository.findActiveByProductIdsAndTenantId(productIds, tenantId);
        return productOptionMapper.toDtoList(productOptions);
    }

    public ProductOptionDto addOptionToProduct(Long productId, Long optionId, boolean isRequired, Integer sortOrder, Long tenantId) {
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

    public ProductOptionDto updateProductOption(Long productId, Long optionId, ProductOptionDto productOptionDto, Long tenantId) {
        ProductOption existingProductOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));

        productOptionMapper.updateEntity(productOptionDto, existingProductOption);
        existingProductOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating product option for product: {} and option: {} for tenant: {}", productId, optionId, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(existingProductOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    public void removeOptionFromProduct(Long productId, Long optionId, Long tenantId) {
        ProductOption productOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        
        productOption.setDeletedAt(LocalDateTime.now());
        productOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Removing option: {} from product: {} for tenant: {}", optionId, productId, tenantId);
        productOptionRepository.save(productOption);
    }

    public void removeAllOptionsFromProduct(Long productId, Long tenantId) {
        log.info("Soft deleting all options for product: {} and tenant: {}", productId, tenantId);
        productOptionRepository.softDeleteByProductIdAndTenantId(productId, tenantId);
    }

    public void removeProductsFromOption(Long optionId, Long tenantId) {
        log.info("Soft deleting all products for option: {} and tenant: {}", optionId, tenantId);
        productOptionRepository.softDeleteByOptionIdAndTenantId(optionId, tenantId);
    }

    public ProductOptionDto updateRequiredStatus(Long productId, Long optionId, boolean isRequired, Long tenantId) {
        ProductOption productOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        
        productOption.setIsRequired(isRequired);
        productOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating required status for product: {} and option: {} to {} for tenant: {}", productId, optionId, isRequired, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(productOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    public ProductOptionDto updateSortOrder(Long productId, Long optionId, Integer sortOrder, Long tenantId) {
        ProductOption productOption = productOptionRepository.findActiveByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        
        productOption.setSortOrder(sortOrder);
        productOption.setUpdatedAt(LocalDateTime.now());
        
        log.info("Updating sort order for product: {} and option: {} to {} for tenant: {}", productId, optionId, sortOrder, tenantId);
        ProductOption savedProductOption = productOptionRepository.save(productOption);
        return productOptionMapper.toDto(savedProductOption);
    }

    @Transactional(readOnly = true)
    public long getProductOptionCount(Long productId, Long tenantId) {
        return productOptionRepository.countActiveByProductIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public long getOptionProductCount(Long optionId, Long tenantId) {
        return productOptionRepository.countActiveByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Long> getProductIdsByOption(Long optionId, Long tenantId) {
        return productOptionRepository.findProductIdsByOptionIdAndTenantId(optionId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredOptions(Long productId, Long tenantId) {
        return productOptionRepository.countActiveRequiredByProductIdAndTenantId(productId, tenantId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isOptionAssignedToProduct(Long productId, Long optionId, Long tenantId) {
        return productOptionRepository.existsByProductIdAndOptionIdAndTenantId(productId, optionId, tenantId);
    }
}
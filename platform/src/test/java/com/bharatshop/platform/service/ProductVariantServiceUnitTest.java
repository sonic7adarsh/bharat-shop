package com.bharatshop.platform.service;

import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.entity.*;
import com.bharatshop.shared.mapper.ProductVariantMapper;
import com.bharatshop.shared.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductVariantService - Isolated test without Spring Boot context
 */
@ExtendWith(MockitoExtension.class)
class ProductVariantServiceUnitTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductVariantOptionValueRepository productVariantOptionValueRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OptionValueRepository optionValueRepository;

    @Mock
    private ProductVariantMapper productVariantMapper;

    @Mock
    private ProductVariantOptionValueService variantOptionValueService;

    @InjectMocks
    private ProductVariantService productVariantService;

    private Long tenantId;
    private Long productId;
    private Long variantId;
    private Long optionId1;
    private Long optionId2;
    private Long optionValueId1;
    private Long optionValueId2;
    private Product product;
    private ProductVariant productVariant;
    private ProductVariantDto productVariantDto;
    private OptionValue optionValue1;
    private OptionValue optionValue2;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        productId = 2L;
        variantId = 3L;
        optionId1 = 4L;
        optionId2 = 5L;
        optionValueId1 = 6L;
        optionValueId2 = 7L;

        // Create test entities
        product = new Product();
        product.setId(productId);
        product.setTenantId(tenantId);
        product.setName("Test Product");
        product.setStatus(Product.ProductStatus.ACTIVE);

        productVariant = new ProductVariant();
        productVariant.setId(variantId);
        productVariant.setTenantId(tenantId);
        productVariant.setProduct(product);
        productVariant.setSku("TEST-SKU-001");
        productVariant.setPrice(new BigDecimal("99.99"));
        productVariant.setStock(100);
        productVariant.setStatus(ProductVariant.VariantStatus.ACTIVE);
        productVariant.setIsDefault(false);

        optionValue1 = new OptionValue();
        optionValue1.setId(optionValueId1);
        optionValue1.setTenantId(tenantId);
        optionValue1.setValue("Red");

        optionValue2 = new OptionValue();
        optionValue2.setId(optionValueId2);
        optionValue2.setTenantId(tenantId);
        optionValue2.setValue("Large");

        productVariantDto = new ProductVariantDto();
        productVariantDto.setId(variantId);
        productVariantDto.setProductId(productId);
        productVariantDto.setSku("TEST-SKU-001");
        productVariantDto.setPrice(new BigDecimal("99.99"));
        productVariantDto.setStock(100);
        productVariantDto.setStatus(ProductVariant.VariantStatus.ACTIVE);
        productVariantDto.setIsDefault(false);
    }

    @Test
    @DisplayName("Should create variant successfully")
    void shouldCreateVariantSuccessfully() {
        // Given
        Map<Long, Long> optionValues = Map.of(optionId1, optionValueId1, optionId2, optionValueId2);
        

        when(variantOptionValueService.findVariantByOptionValues(productId, optionValues, tenantId))
            .thenReturn(Optional.empty());
        when(productVariantMapper.toEntity(any(ProductVariantDto.class)))
            .thenReturn(productVariant);
        when(productVariantRepository.save(any(ProductVariant.class)))
            .thenReturn(productVariant);
        when(productVariantMapper.toDtoWithComputedFields(any(ProductVariant.class)))
            .thenReturn(productVariantDto);

        // When
        ProductVariantDto result = productVariantService.createVariant(productVariantDto, optionValues, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(variantId);
        assertThat(result.getSku()).isEqualTo("TEST-SKU-001");
        
        verify(variantOptionValueService).findVariantByOptionValues(productId, optionValues, tenantId);
        verify(productVariantRepository).save(any(ProductVariant.class));
        verify(variantOptionValueService).setVariantOptionValues(eq(variantId), eq(optionValues), eq(tenantId));
    }

    @Test
    @DisplayName("Should get variant by id")
    void shouldGetVariantById() {
        // Given
        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
            .thenReturn(Optional.of(productVariant));
        when(productVariantMapper.toDtoWithComputedFields(any(ProductVariant.class)))
            .thenReturn(productVariantDto);

        // When
        Optional<ProductVariantDto> result = productVariantService.getVariantById(variantId, tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(variantId);
        
        verify(productVariantRepository).findActiveByIdAndTenantId(variantId, tenantId);
        verify(productVariantMapper).toDtoWithComputedFields(any(ProductVariant.class));
    }
}
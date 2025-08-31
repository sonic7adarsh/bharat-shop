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
 * Unit tests for ProductVariantService
 * Tests business logic in isolation with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

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

    @InjectMocks
    private ProductVariantService productVariantService;

    private UUID tenantId;
    private UUID productId;
    private UUID variantId;
    private UUID optionId1;
    private UUID optionId2;
    private UUID optionValueId1;
    private UUID optionValueId2;
    private Product product;
    private ProductVariant productVariant;
    private ProductVariantDto productVariantDto;
    private OptionValue optionValue1;
    private OptionValue optionValue2;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        optionId1 = UUID.randomUUID();
        optionId2 = UUID.randomUUID();
        optionValueId1 = UUID.randomUUID();
        optionValueId2 = UUID.randomUUID();

        product = Product.builder()
                .id(productId)
                .tenantId(tenantId)
                .name("Test Product")
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(10)
                .sku("TEST-001")
                .build();

        productVariant = ProductVariant.builder()
                .id(variantId)
                .product(product)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .stockQuantity(5)
                .isDefault(true)
                .build();

        productVariantDto = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .stockQuantity(5)
                .isDefault(true)
                .build();

        optionValue1 = OptionValue.builder()
                .id(optionValueId1)
                .value("Small")
                .build();

        optionValue2 = OptionValue.builder()
                .id(optionValueId2)
                .value("Red")
                .build();
    }

    @Test
    @DisplayName("Should create variant successfully")
    void shouldCreateVariantSuccessfully() {
        // Given
        Map<UUID, UUID> optionValues = Map.of(
                optionId1, optionValueId1,
                optionId2, optionValueId2
        );

        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.existsByProductAndOptionValueCombination(eq(product), any()))
                .thenReturn(false);
        when(productVariantMapper.toEntity(productVariantDto))
                .thenReturn(productVariant);
        when(productVariantRepository.save(productVariant))
                .thenReturn(productVariant);
        when(optionValueRepository.findAllById(optionValues.values()))
                .thenReturn(List.of(optionValue1, optionValue2));
        when(productVariantMapper.toDto(productVariant))
                .thenReturn(productVariantDto);

        // When
        ProductVariantDto result = productVariantService.createVariant(productVariantDto, optionValues, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("TEST-001-SM-RED");
        verify(productVariantRepository).save(productVariant);
        verify(productVariantOptionValueRepository, times(2)).save(any(ProductVariantOptionValue.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        Map<UUID, UUID> optionValues = Map.of(optionId1, optionValueId1);
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.createVariant(productVariantDto, optionValues, tenantId)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("Should throw exception when variant combination already exists")
    void shouldThrowExceptionWhenVariantCombinationExists() {
        // Given
        Map<UUID, UUID> optionValues = Map.of(optionId1, optionValueId1);
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.existsByProductAndOptionValueCombination(eq(product), any()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.createVariant(productVariantDto, optionValues, tenantId)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("variant combination already exists");
    }

    @Test
    @DisplayName("Should get variants by product")
    void shouldGetVariantsByProduct() {
        // Given
        List<ProductVariant> variants = List.of(productVariant);
        when(productVariantRepository.findByProductIdAndTenantId(productId, tenantId))
                .thenReturn(variants);
        when(productVariantMapper.toDto(productVariant))
                .thenReturn(productVariantDto);

        // When
        List<ProductVariantDto> result = productVariantService.getVariantsByProduct(productId, tenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("TEST-001-SM-RED");
    }

    @Test
    @DisplayName("Should get variant by id")
    void shouldGetVariantById() {
        // Given
        when(productVariantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantMapper.toDto(productVariant))
                .thenReturn(productVariantDto);

        // When
        Optional<ProductVariantDto> result = productVariantService.getVariantById(variantId, tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo("TEST-001-SM-RED");
    }

    @Test
    @DisplayName("Should update variant successfully")
    void shouldUpdateVariantSuccessfully() {
        // Given
        Map<UUID, UUID> newOptionValues = Map.of(optionId1, optionValueId2);
        ProductVariant updatedVariant = ProductVariant.builder()
                .id(variantId)
                .product(product)
                .sku("TEST-001-LG-BLUE")
                .price(BigDecimal.valueOf(110.00))
                .stockQuantity(8)
                .isDefault(true)
                .build();
        ProductVariantDto updatedDto = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-LG-BLUE")
                .price(BigDecimal.valueOf(110.00))
                .stockQuantity(8)
                .isDefault(true)
                .build();

        when(productVariantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantRepository.existsByProductAndOptionValueCombinationExcludingVariant(
                eq(product), any(), eq(variantId)))
                .thenReturn(false);
        when(productVariantMapper.updateEntityFromDto(updatedDto, productVariant))
                .thenReturn(updatedVariant);
        when(productVariantRepository.save(updatedVariant))
                .thenReturn(updatedVariant);
        when(optionValueRepository.findAllById(newOptionValues.values()))
                .thenReturn(List.of(optionValue2));
        when(productVariantMapper.toDto(updatedVariant))
                .thenReturn(updatedDto);

        // When
        ProductVariantDto result = productVariantService.updateVariant(updatedDto, newOptionValues, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("TEST-001-LG-BLUE");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(110.00));
        verify(productVariantOptionValueRepository).deleteByVariantId(variantId);
        verify(productVariantRepository).save(updatedVariant);
    }

    @Test
    @DisplayName("Should delete variant successfully")
    void shouldDeleteVariantSuccessfully() {
        // Given
        when(productVariantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));

        // When
        productVariantService.deleteVariant(variantId, tenantId);

        // Then
        verify(productVariantOptionValueRepository).deleteByVariantId(variantId);
        verify(productVariantRepository).delete(productVariant);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent variant")
    void shouldThrowExceptionWhenDeletingNonExistentVariant() {
        // Given
        when(productVariantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.deleteVariant(variantId, tenantId)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Variant not found");
    }

    @Test
    @DisplayName("Should set default variant")
    void shouldSetDefaultVariant() {
        // Given
        when(productVariantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));

        // When
        productVariantService.setDefaultVariant(variantId, tenantId);

        // Then
        verify(productVariantRepository).clearDefaultForProduct(productId);
        verify(productVariantRepository).setAsDefault(variantId);
    }

    @Test
    @DisplayName("Should update variant stock")
    void shouldUpdateVariantStock() {
        // Given
        int newStock = 15;
        when(productVariantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantMapper.toDto(any(ProductVariant.class)))
                .thenReturn(productVariantDto.toBuilder().stockQuantity(newStock).build());

        // When
        ProductVariantDto result = productVariantService.updateVariantStock(variantId, newStock, tenantId);

        // Then
        assertThat(result.getStockQuantity()).isEqualTo(newStock);
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Should find variant by option values")
    void shouldFindVariantByOptionValues() {
        // Given
        Map<UUID, UUID> searchOptionValues = Map.of(optionId1, optionValueId1);
        when(productVariantRepository.findByProductIdAndOptionValues(productId, searchOptionValues, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantMapper.toDto(productVariant))
                .thenReturn(productVariantDto);

        // When
        Optional<ProductVariantDto> result = productVariantService.findVariantByOptionValues(
                productId, searchOptionValues, tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo("TEST-001-SM-RED");
    }

    @Test
    @DisplayName("Should count variants by product")
    void shouldCountVariantsByProduct() {
        // Given
        long expectedCount = 3L;
        when(productVariantRepository.countByProductIdAndTenantId(productId, tenantId))
                .thenReturn(expectedCount);

        // When
        long result = productVariantService.countVariantsByProduct(productId, tenantId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("Should get default variant")
    void shouldGetDefaultVariant() {
        // Given
        when(productVariantRepository.findDefaultByProductIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantMapper.toDto(productVariant))
                .thenReturn(productVariantDto);

        // When
        Optional<ProductVariantDto> result = productVariantService.getDefaultVariant(productId, tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("Should validate option values exist")
    void shouldValidateOptionValuesExist() {
        // Given
        Map<UUID, UUID> optionValues = Map.of(optionId1, optionValueId1, optionId2, optionValueId2);
        when(optionValueRepository.findAllById(optionValues.values()))
                .thenReturn(List.of(optionValue1)); // Missing one option value

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.validateOptionValues(optionValues)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Some option values not found");
    }

    @Test
    @DisplayName("Should handle empty variant list")
    void shouldHandleEmptyVariantList() {
        // Given
        when(productVariantRepository.findByProductIdAndTenantId(productId, tenantId))
                .thenReturn(Collections.emptyList());

        // When
        List<ProductVariantDto> result = productVariantService.getVariantsByProduct(productId, tenantId);

        // Then
        assertThat(result).isEmpty();
    }
}
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

    @Mock
    private ProductVariantOptionValueService variantOptionValueService;

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
                .stock(10)
                .build();

        productVariant = ProductVariant.builder()
                .id(variantId)
                .productId(productId)
                .product(product)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .stock(5)
                .isDefault(true)
                .build();

        productVariantDto = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .stock(5)
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

        when(variantOptionValueService.findVariantByOptionValues(productId, optionValues, tenantId))
                .thenReturn(Optional.empty());
        when(productVariantMapper.toEntity(productVariantDto))
                .thenReturn(productVariant);
        when(productVariantRepository.save(productVariant))
                .thenReturn(productVariant);
        when(optionValueRepository.findAllById(optionValues.values()))
                .thenReturn(List.of(optionValue1, optionValue2));
        when(productVariantMapper.toDtoWithComputedFields(productVariant))
                .thenReturn(productVariantDto);

        // When
        ProductVariantDto result = productVariantService.createVariant(productVariantDto, optionValues, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("TEST-001-SM-RED");
        verify(productVariantRepository).save(productVariant);
        verify(variantOptionValueService).setVariantOptionValues(any(UUID.class), eq(optionValues), eq(tenantId));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        Map<UUID, UUID> optionValues = Map.of(optionId1, optionValueId1);
        when(productVariantRepository.existsBySkuAndTenantId(productVariantDto.getSku(), tenantId))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.createVariant(productVariantDto, optionValues, tenantId)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("SKU");
    }

    @Test
    @DisplayName("Should throw exception when variant combination already exists")
    void shouldThrowExceptionWhenVariantCombinationExists() {
        // Given
        Map<UUID, UUID> optionValues = Map.of(optionId1, optionValueId1);
        when(variantOptionValueService.findVariantByOptionValues(productId, optionValues, tenantId))
                .thenReturn(Optional.of(UUID.randomUUID()));

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.createVariant(productVariantDto, optionValues, tenantId)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("A variant with this option combination already exists");
    }

    @Test
    @DisplayName("Should get variants by product")
    void shouldGetVariantsByProduct() {
        // Given
        List<ProductVariant> variants = List.of(productVariant);
        when(productVariantRepository.findActiveByProductIdAndTenantId(productId, tenantId))
                .thenReturn(variants);
        when(productVariantMapper.toDtoWithComputedFields(productVariant))
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
        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantMapper.toDtoWithComputedFields(productVariant))
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
                .stock(8)
                .isDefault(true)
                .build();
        ProductVariantDto updatedDto = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-LG-BLUE")
                .price(BigDecimal.valueOf(110.00))
                .stock(8)
                .isDefault(true)
                .build();

        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(variantOptionValueService.isVariantCombinationUnique(variantId, newOptionValues, tenantId))
                .thenReturn(true);
        doNothing().when(productVariantMapper).updateEntity(updatedDto, productVariant);
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(updatedVariant);
        when(optionValueRepository.findAllById(newOptionValues.values()))
                .thenReturn(List.of(optionValue2));
        when(productVariantMapper.toDtoWithComputedFields(any(ProductVariant.class)))
                .thenReturn(updatedDto);

        // When
        ProductVariantDto result = productVariantService.updateVariant(variantId, updatedDto, newOptionValues, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("TEST-001-LG-BLUE");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(110.00));
        verify(variantOptionValueService).setVariantOptionValues(eq(variantId), eq(newOptionValues), eq(tenantId));
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Should delete variant successfully")
    void shouldDeleteVariantSuccessfully() {
        // Given
        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));

        // When
        productVariantService.deleteVariant(variantId, tenantId);

        // Then
        verify(variantOptionValueService).removeAllOptionValuesFromVariant(variantId, tenantId);
        verify(productVariantRepository).delete(productVariant);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent variant")
    void shouldThrowExceptionWhenDeletingNonExistentVariant() {
        // Given
        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            productVariantService.deleteVariant(variantId, tenantId)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Variant not found with id:");
    }

    @Test
    @DisplayName("Should set default variant")
    void shouldSetDefaultVariant() {
        // Given
        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantMapper.toDtoWithComputedFields(any(ProductVariant.class)))
                .thenReturn(productVariantDto);

        // When
        ProductVariantDto result = productVariantService.setDefaultVariant(variantId, tenantId);

        // Then
        assertThat(result).isNotNull();
        verify(productVariantRepository).clearDefaultForProduct(productId, tenantId);
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Should update variant stock")
    void shouldUpdateVariantStock() {
        // Given
        int newStock = 15;
        when(productVariantRepository.findActiveByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantMapper.toDtoWithComputedFields(any(ProductVariant.class)))
                .thenReturn(productVariantDto);

        // When
        ProductVariantDto result = productVariantService.updateVariantStock(variantId, newStock, tenantId);

        // Then
        assertThat(result.getStock()).isEqualTo(productVariantDto.getStock());
        verify(productVariantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Should find variant by option values")
    void shouldFindVariantByOptionValues() {
        // Given
        Map<UUID, UUID> searchOptionValues = Map.of(optionId1, optionValueId1);
        when(variantOptionValueService.findVariantByOptionValues(productId, searchOptionValues, tenantId))
                .thenReturn(Optional.of(variantId));

        // When
        Optional<UUID> result = productVariantService.findVariantByOptionValues(
                productId, searchOptionValues, tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(variantId);
    }

    @Test
    @DisplayName("Should get variant count")
    void shouldGetVariantCount() {
        // Given
        long expectedCount = 3L;
        when(productVariantRepository.countActiveByProductIdAndTenantId(productId, tenantId))
                .thenReturn(expectedCount);

        // When
        long result = productVariantService.getVariantCount(productId, tenantId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("Should get default variant")
    void shouldGetDefaultVariant() {
        // Given
        when(productVariantRepository.findDefaultByProductIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(productVariant));
        when(productVariantMapper.toDtoWithComputedFields(productVariant))
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
        when(variantOptionValueService.findVariantByOptionValues(productId, optionValues, tenantId))
                .thenReturn(Optional.empty());
        when(productVariantMapper.toEntity(productVariantDto))
                .thenReturn(productVariant);
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(productVariant);
        when(productVariantMapper.toDtoWithComputedFields(any(ProductVariant.class)))
                .thenReturn(productVariantDto);

        // When
        ProductVariantDto result = productVariantService.createVariant(productVariantDto, optionValues, tenantId);

        // Then
        assertThat(result).isNotNull();
        verify(variantOptionValueService).setVariantOptionValues(any(UUID.class), eq(optionValues), eq(tenantId));
    }

    @Test
    @DisplayName("Should handle empty variant list")
    void shouldHandleEmptyVariantList() {
        // Given
        when(productVariantRepository.findActiveByProductIdAndTenantId(productId, tenantId))
                .thenReturn(Collections.emptyList());

        // When
        List<ProductVariantDto> result = productVariantService.getVariantsByProduct(productId, tenantId);

        // Then
        assertThat(result).isEmpty();
    }
}
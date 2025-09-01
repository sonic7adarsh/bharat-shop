package com.bharatshop.platform.integration;

import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.ProductService;
import com.bharatshop.platform.service.OptionService;
import com.bharatshop.platform.service.OptionValueService;
import com.bharatshop.platform.service.ProductOptionService;
import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.dto.OptionDto;
import com.bharatshop.shared.dto.OptionValueDto;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.Option;
import com.bharatshop.shared.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ProductVariantService
 * Tests the complete variant functionality including CRUD operations,
 * unique variant combinations, and option value management
 */
@Transactional
class ProductVariantServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductVariantService productVariantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OptionService optionService;

    @Autowired
    private OptionValueService optionValueService;

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductRepository productRepository;

    private UUID tenantId;
    private UUID productId;
    private UUID sizeOptionId;
    private UUID colorOptionId;
    private UUID smallSizeValueId;
    private UUID largeSizeValueId;
    private UUID redColorValueId;
    private UUID blueColorValueId;

    @BeforeEach
    void setUpTestData() {
        tenantId = UUID.randomUUID();
        
        // Create a test product
        Product product = Product.builder()
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .status(Product.ProductStatus.ACTIVE)
                .tenantId(tenantId)
                .build();
        product = productRepository.save(product);
        productId = product.getId();

        // Create options
        OptionDto sizeOption = OptionDto.builder()
                .name("Size")
                .displayName("Size")
                .type(Option.OptionType.SIZE)
                .build();
        sizeOptionId = optionService.createOption(sizeOption, tenantId).getId();

        OptionDto colorOption = OptionDto.builder()
                .name("Color")
                .displayName("Color")
                .type(Option.OptionType.COLOR)
                .build();
        colorOptionId = optionService.createOption(colorOption, tenantId).getId();

        // Create option values
        OptionValueDto smallSize = OptionValueDto.builder()
                .optionId(sizeOptionId)
                .value("Small")
                .sortOrder(1)
                .build();
        smallSizeValueId = optionValueService.createOptionValue(smallSize, tenantId).getId();

        OptionValueDto largeSize = OptionValueDto.builder()
                .optionId(sizeOptionId)
                .value("Large")
                .sortOrder(2)
                .build();
        largeSizeValueId = optionValueService.createOptionValue(largeSize, tenantId).getId();

        OptionValueDto redColor = OptionValueDto.builder()
                .optionId(colorOptionId)
                .value("Red")
                .sortOrder(1)
                .build();
        redColorValueId = optionValueService.createOptionValue(redColor, tenantId).getId();

        OptionValueDto blueColor = OptionValueDto.builder()
                .optionId(colorOptionId)
                .value("Blue")
                .sortOrder(2)
                .build();
        blueColorValueId = optionValueService.createOptionValue(blueColor, tenantId).getId();

        // Add options to product
        productOptionService.addOptionToProduct(productId, sizeOptionId, true, 1, tenantId);
        productOptionService.addOptionToProduct(productId, colorOptionId, true, 2, tenantId);
    }

    @Test
    @DisplayName("Should create product variant successfully")
    void shouldCreateProductVariantSuccessfully() {
        // Given
        ProductVariantDto variantDto = ProductVariantDto.builder()
                .productId(productId)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .salePrice(BigDecimal.valueOf(85.00))
                .stock(5)
                .isDefault(true)
                .build();

        Map<UUID, UUID> optionValues = new HashMap<>();
        optionValues.put(sizeOptionId, smallSizeValueId);
        optionValues.put(colorOptionId, redColorValueId);

        // When
        ProductVariantDto createdVariant = productVariantService.createVariant(variantDto, optionValues, tenantId);

        // Then
        assertThat(createdVariant).isNotNull();
        assertThat(createdVariant.getId()).isNotNull();
        assertThat(createdVariant.getProductId()).isEqualTo(productId);
        assertThat(createdVariant.getSku()).isEqualTo("TEST-001-SM-RED");
        assertThat(createdVariant.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(95.00));
        assertThat(createdVariant.getSalePrice()).isEqualByComparingTo(BigDecimal.valueOf(85.00));
        assertThat(createdVariant.getStock()).isEqualTo(5);
        assertThat(createdVariant.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("Should prevent duplicate variant combinations")
    void shouldPreventDuplicateVariantCombinations() {
        // Given - Create first variant
        ProductVariantDto firstVariant = ProductVariantDto.builder()
                .productId(productId)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .stock(5)
                .build();

        Map<UUID, UUID> optionValues = new HashMap<>();
        optionValues.put(sizeOptionId, smallSizeValueId);
        optionValues.put(colorOptionId, redColorValueId);

        productVariantService.createVariant(firstVariant, optionValues, tenantId);

        // When - Try to create duplicate variant
        ProductVariantDto duplicateVariant = ProductVariantDto.builder()
                .productId(productId)
                .sku("TEST-001-SM-RED-DUP")
                .price(BigDecimal.valueOf(100.00))
                .stock(3)
                .build();

        // Then
        assertThatThrownBy(() -> 
            productVariantService.createVariant(duplicateVariant, optionValues, tenantId)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("variant combination already exists");
    }

    @Test
    @DisplayName("Should retrieve variants by product")
    void shouldRetrieveVariantsByProduct() {
        // Given - Create multiple variants
        createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);
        createTestVariant("TEST-001-SM-BLUE", smallSizeValueId, blueColorValueId, false);
        createTestVariant("TEST-001-LG-RED", largeSizeValueId, redColorValueId, false);

        // When
        List<ProductVariantDto> variants = productVariantService.getVariantsByProduct(productId, tenantId);

        // Then
        assertThat(variants).hasSize(3);
        assertThat(variants.stream().map(ProductVariantDto::getSku))
                .containsExactlyInAnyOrder("TEST-001-SM-RED", "TEST-001-SM-BLUE", "TEST-001-LG-RED");
        
        // Check default variant
        Optional<ProductVariantDto> defaultVariant = variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst();
        assertThat(defaultVariant).isPresent();
        assertThat(defaultVariant.get().getSku()).isEqualTo("TEST-001-SM-RED");
    }

    @Test
    @DisplayName("Should update variant successfully")
    void shouldUpdateVariantSuccessfully() {
        // Given
        UUID variantId = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        ProductVariantDto updateDto = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-SM-RED-UPDATED")
                .price(BigDecimal.valueOf(110.00))
                .stock(8)
                .isDefault(true)
                .build();

        Map<UUID, UUID> newOptionValues = new HashMap<>();
        newOptionValues.put(sizeOptionId, largeSizeValueId); // Change size to Large
        newOptionValues.put(colorOptionId, redColorValueId);

        // When
        ProductVariantDto updatedVariant = productVariantService.updateVariant(variantId, updateDto, newOptionValues, tenantId);

        // Then
        assertThat(updatedVariant.getSku()).isEqualTo("TEST-001-SM-RED-UPDATED");
        assertThat(updatedVariant.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(110.00));
        assertThat(updatedVariant.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should delete variant successfully")
    void shouldDeleteVariantSuccessfully() {
        // Given
        UUID variantId = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        // When
        productVariantService.deleteVariant(variantId, tenantId);

        // Then
        Optional<ProductVariantDto> deletedVariant = productVariantService.getVariantById(variantId, tenantId);
        assertThat(deletedVariant).isEmpty();
    }

    @Test
    @DisplayName("Should set default variant")
    void shouldSetDefaultVariant() {
        // Given
        UUID variant1Id = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);
        UUID variant2Id = createTestVariant("TEST-001-SM-BLUE", smallSizeValueId, blueColorValueId, false);

        // When
        productVariantService.setDefaultVariant(variant2Id, tenantId);

        // Then
        Optional<ProductVariantDto> newDefault = productVariantService.getVariantById(variant2Id, tenantId);
        Optional<ProductVariantDto> oldDefault = productVariantService.getVariantById(variant1Id, tenantId);

        assertThat(newDefault).isPresent();
        assertThat(newDefault.get().getIsDefault()).isTrue();
        assertThat(oldDefault).isPresent();
        assertThat(oldDefault.get().getIsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should update variant stock")
    void shouldUpdateVariantStock() {
        // Given
        UUID variantId = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        // When
        productVariantService.updateVariantStock(variantId, 15, tenantId);

        // Then
        Optional<ProductVariantDto> updatedVariant = productVariantService.getVariantById(variantId, tenantId);
        assertThat(updatedVariant).isPresent();
        assertThat(updatedVariant.get().getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should find variant by option values")
    void shouldFindVariantByOptionValues() {
        // Given
        createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);
        createTestVariant("TEST-001-LG-BLUE", largeSizeValueId, blueColorValueId, false);

        Map<UUID, UUID> searchOptionValues = new HashMap<>();
        searchOptionValues.put(sizeOptionId, largeSizeValueId);
        searchOptionValues.put(colorOptionId, blueColorValueId);

        // When
        Optional<UUID> variantIdResult = productVariantService.findVariantByOptionValues(
                productId, searchOptionValues, tenantId);
        
        // Then
        assertThat(variantIdResult).isPresent();
        
        // Get the full variant details
        Optional<ProductVariantDto> result = productVariantService.getVariantById(variantIdResult.get(), tenantId);
        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo("TEST-001-LG-BLUE");
    }

    @Test
    @DisplayName("Should count variants by product")
    void shouldCountVariantsByProduct() {
        // Given
        createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);
        createTestVariant("TEST-001-SM-BLUE", smallSizeValueId, blueColorValueId, false);
        createTestVariant("TEST-001-LG-RED", largeSizeValueId, redColorValueId, false);

        // When
        long count = productVariantService.getVariantCount(productId, tenantId);

        // Then
        assertThat(count).isEqualTo(3);
    }

    private UUID createTestVariant(String sku, UUID sizeValueId, UUID colorValueId, boolean isDefault) {
        ProductVariantDto variantDto = ProductVariantDto.builder()
                .productId(productId)
                .sku(sku)
                .price(BigDecimal.valueOf(95.00))
                .stock(5)
                .isDefault(isDefault)
                .build();

        Map<UUID, UUID> optionValues = new HashMap<>();
        optionValues.put(sizeOptionId, sizeValueId);
        optionValues.put(colorOptionId, colorValueId);

        return productVariantService.createVariant(variantDto, optionValues, tenantId).getId();
    }
}
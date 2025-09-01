package com.bharatshop.platform.integration;

import com.bharatshop.platform.service.ProductService;
import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.OptionService;
import com.bharatshop.platform.service.OptionValueService;
import com.bharatshop.platform.service.ProductOptionService;
import com.bharatshop.shared.dto.*;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.Option;
import com.bharatshop.shared.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ProductController variant endpoints
 * Tests the REST API endpoints for managing product variants and options
 */
@AutoConfigureWebMvc
@Transactional
class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductVariantService productVariantService;

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
                .tenantId(tenantId)
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .status(Product.ProductStatus.ACTIVE)
                .build();
        product = productRepository.save(product);
        productId = product.getId();

        // Create options and option values
        setupOptionsAndValues();
    }

    private void setupOptionsAndValues() {
        // Create Size option
        OptionDto sizeOption = OptionDto.builder()
                .name("Size")
                .type(Option.OptionType.SIZE)
                .isRequired(true)
                .build();
        sizeOptionId = optionService.createOption(sizeOption, tenantId).getId();

        // Create Color option
        OptionDto colorOption = OptionDto.builder()
                .name("Color")
                .type(Option.OptionType.COLOR)
                .isRequired(true)
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
    @DisplayName("Should create product variant via API")
    void shouldCreateProductVariantViaAPI() throws Exception {
        // Given
        Map<String, Object> variantRequest = new HashMap<>();
        variantRequest.put("productId", productId.toString());
        variantRequest.put("sku", "TEST-001-SM-RED");
        variantRequest.put("price", 95.00);
        variantRequest.put("discountPrice", 85.00);
        variantRequest.put("stock", 5);
        variantRequest.put("isDefault", true);
        
        Map<String, String> optionValues = new HashMap<>();
        optionValues.put(sizeOptionId.toString(), smallSizeValueId.toString());
        optionValues.put(colorOptionId.toString(), redColorValueId.toString());
        variantRequest.put("optionValues", optionValues);

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.sku").value("TEST-001-SM-RED"))
                .andExpect(jsonPath("$.price").value(95.00))
                .andExpect(jsonPath("$.discountPrice").value(85.00))
                .andExpect(jsonPath("$.stock").value(5))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Should get product variants via API")
    void shouldGetProductVariantsViaAPI() throws Exception {
        // Given - Create test variants
        createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);
        createTestVariant("TEST-001-LG-BLUE", largeSizeValueId, blueColorValueId, false);

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].sku", containsInAnyOrder("TEST-001-SM-RED", "TEST-001-LG-BLUE")));
    }

    @Test
    @DisplayName("Should update product variant via API")
    void shouldUpdateProductVariantViaAPI() throws Exception {
        // Given
        UUID variantId = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("sku", "TEST-001-SM-RED-UPDATED");
        updateRequest.put("price", 110.00);
        updateRequest.put("stock", 8);
        updateRequest.put("isDefault", true);
        
        Map<String, String> optionValues = new HashMap<>();
        optionValues.put(sizeOptionId.toString(), largeSizeValueId.toString());
        optionValues.put(colorOptionId.toString(), redColorValueId.toString());
        updateRequest.put("optionValues", optionValues);

        // When & Then
        mockMvc.perform(put("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TEST-001-SM-RED-UPDATED"))
                .andExpect(jsonPath("$.price").value(110.00))
                .andExpect(jsonPath("$.stock").value(8));
    }

    @Test
    @DisplayName("Should delete product variant via API")
    void shouldDeleteProductVariantViaAPI() throws Exception {
        // Given
        UUID variantId = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        // When & Then
        mockMvc.perform(delete("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should set default variant via API")
    void shouldSetDefaultVariantViaAPI() throws Exception {
        // Given
        UUID variant1Id = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);
        UUID variant2Id = createTestVariant("TEST-001-LG-BLUE", largeSizeValueId, blueColorValueId, false);

        // When & Then
        mockMvc.perform(patch("/api/products/{productId}/variants/{variantId}/default", productId, variant2Id)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk());

        // Verify new default
        mockMvc.perform(get("/api/products/{productId}/variants/{variantId}", productId, variant2Id)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Should update variant stock via API")
    void shouldUpdateVariantStockViaAPI() throws Exception {
        // Given
        UUID variantId = createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        Map<String, Object> stockRequest = new HashMap<>();
        stockRequest.put("stock", 15);

        // When & Then
        mockMvc.perform(patch("/api/products/{productId}/variants/{variantId}/stock", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));
    }

    @Test
    @DisplayName("Should add option to product via API")
    void shouldAddOptionToProductViaAPI() throws Exception {
        // Given
        OptionDto materialOption = OptionDto.builder()
                .name("Material")
                .type(Option.OptionType.MATERIAL)
                .isRequired(false)
                .build();
        UUID materialOptionId = optionService.createOption(materialOption, tenantId).getId();

        Map<String, Object> optionRequest = new HashMap<>();
        optionRequest.put("optionId", materialOptionId.toString());
        optionRequest.put("required", false);
        optionRequest.put("sortOrder", 3);

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/options", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(optionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.optionId").value(materialOptionId.toString()))
                .andExpect(jsonPath("$.required").value(false))
                .andExpect(jsonPath("$.sortOrder").value(3));
    }

    @Test
    @DisplayName("Should get product options via API")
    void shouldGetProductOptionsViaAPI() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/{productId}/options", productId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].option.name", containsInAnyOrder("Size", "Color")));
    }

    @Test
    @DisplayName("Should remove option from product via API")
    void shouldRemoveOptionFromProductViaAPI() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}", productId, sizeOptionId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNoContent());

        // Verify removal
        mockMvc.perform(get("/api/products/{productId}/options", productId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].option.name").value("Color"));
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors() throws Exception {
        // Given - Invalid variant request (missing required fields)
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("productId", productId.toString());
        // Missing sku, price, stock

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle duplicate variant combinations")
    void shouldHandleDuplicateVariantCombinations() throws Exception {
        // Given - Create first variant
        createTestVariant("TEST-001-SM-RED", smallSizeValueId, redColorValueId, true);

        // Try to create duplicate
        Map<String, Object> duplicateRequest = new HashMap<>();
        duplicateRequest.put("productId", productId.toString());
        duplicateRequest.put("sku", "TEST-001-SM-RED-DUP");
        duplicateRequest.put("price", 100.00);
        duplicateRequest.put("stock", 3);
        
        Map<String, String> optionValues = new HashMap<>();
        optionValues.put(sizeOptionId.toString(), smallSizeValueId.toString());
        optionValues.put(colorOptionId.toString(), redColorValueId.toString());
        duplicateRequest.put("optionValues", optionValues);

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
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
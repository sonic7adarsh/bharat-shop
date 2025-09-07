package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.ProductOptionService;
import com.bharatshop.shared.dto.*;
import com.bharatshop.shared.entity.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for ProductController variant endpoints
 * Tests REST API endpoints with mocked services
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductVariantService productVariantService;

    @MockBean
    private ProductOptionService productOptionService;

    private Long tenantId;
    private Long productId;
    private Long variantId;
    private Long optionId;
    private ProductVariantDto productVariantDto;
    private ProductOptionDto productOptionDto;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        productId = 1L;
        variantId = 1L;
        optionId = 1L;

        productVariantDto = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-SM-RED")
                .price(BigDecimal.valueOf(95.00))
                .salePrice(BigDecimal.valueOf(85.00))
                .stock(5)
                .isDefault(true)
                .build();

        OptionDto optionDto = OptionDto.builder()
                .id(optionId)
                .name("Size")
                .type(Option.OptionType.SIZE)
                .isRequired(true)
                .build();

        productOptionDto = ProductOptionDto.builder()
                .id(2L)
                .productId(productId)
                .optionId(optionId)
                .option(optionDto)
                .isRequired(true)
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("Should create product variant")
    void shouldCreateProductVariant() throws Exception {
        // Given
        Map<String, Object> variantRequest = new HashMap<>();
        variantRequest.put("productId", productId.toString());
        variantRequest.put("sku", "TEST-001-SM-RED");
        variantRequest.put("price", 95.00);
        variantRequest.put("discountPrice", 85.00);
        variantRequest.put("stockQuantity", 5);
        variantRequest.put("isDefault", true);
        
        Map<String, String> optionValues = new HashMap<>();
        optionValues.put(optionId.toString(), "2");
        variantRequest.put("optionValues", optionValues);

        when(productVariantService.createVariant(org.mockito.ArgumentMatchers.any(ProductVariantDto.class), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId)))
                .thenReturn(productVariantDto);

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(variantId.toString()))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.sku").value("TEST-001-SM-RED"))
                .andExpect(jsonPath("$.price").value(95.00))
                .andExpect(jsonPath("$.discountPrice").value(85.00))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(productVariantService).createVariant(org.mockito.ArgumentMatchers.any(ProductVariantDto.class), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId));
    }

    @Test
    @DisplayName("Should get product variants")
    void shouldGetProductVariants() throws Exception {
        // Given
        List<ProductVariantDto> variants = List.of(productVariantDto);
        when(productVariantService.getVariantsByProduct(productId, tenantId))
                .thenReturn(variants);

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(variantId.toString()))
                .andExpect(jsonPath("$[0].sku").value("TEST-001-SM-RED"));

        verify(productVariantService).getVariantsByProduct(productId, tenantId);
    }

    @Test
    @DisplayName("Should get product variant by id")
    void shouldGetProductVariantById() throws Exception {
        // Given
        when(productVariantService.getVariantById(variantId, tenantId))
                .thenReturn(Optional.of(productVariantDto));

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(variantId.toString()))
                .andExpect(jsonPath("$.sku").value("TEST-001-SM-RED"));

        verify(productVariantService).getVariantById(variantId, tenantId);
    }

    @Test
    @DisplayName("Should return 404 when variant not found")
    void shouldReturn404WhenVariantNotFound() throws Exception {
        // Given
        when(productVariantService.getVariantById(variantId, tenantId))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update product variant")
    void shouldUpdateProductVariant() throws Exception {
        // Given
        ProductVariantDto updatedVariant = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-SM-RED-UPDATED")
                .price(BigDecimal.valueOf(110.00))
                .stock(5)
                .build();

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("sku", "TEST-001-SM-RED-UPDATED");
        updateRequest.put("price", 110.00);
        updateRequest.put("stockQuantity", 5);
        updateRequest.put("isDefault", true);
        
        Map<String, String> optionValues = new HashMap<>();
        optionValues.put(optionId.toString(), "3");
        updateRequest.put("optionValues", optionValues);

        when(productVariantService.updateVariant(eq(variantId), org.mockito.ArgumentMatchers.any(ProductVariantDto.class), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId)))
                .thenReturn(updatedVariant);

        // When & Then
        mockMvc.perform(put("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TEST-001-SM-RED-UPDATED"))
                .andExpect(jsonPath("$.price").value(110.00));

        verify(productVariantService).updateVariant(eq(variantId), org.mockito.ArgumentMatchers.any(ProductVariantDto.class), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId));
    }

    @Test
    @DisplayName("Should delete product variant")
    void shouldDeleteProductVariant() throws Exception {
        // Given
        doNothing().when(productVariantService).deleteVariant(variantId, tenantId);

        // When & Then
        mockMvc.perform(delete("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNoContent());

        verify(productVariantService).deleteVariant(variantId, tenantId);
    }

    @Test
    @DisplayName("Should set default variant")
    void shouldSetDefaultVariant() throws Exception {
        // Given
        doNothing().when(productVariantService).setDefaultVariant(variantId, tenantId);

        // When & Then
        mockMvc.perform(patch("/api/products/{productId}/variants/{variantId}/default", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk());

        verify(productVariantService).setDefaultVariant(variantId, tenantId);
    }

    @Test
    @DisplayName("Should update variant stock")
    void shouldUpdateVariantStock() throws Exception {
        // Given
        ProductVariantDto updatedVariant = ProductVariantDto.builder()
                .id(variantId)
                .productId(productId)
                .sku("TEST-001-SM-RED")
                .price(new BigDecimal("100.00"))
                .stock(15)
                .build();

        Map<String, Object> stockRequest = new HashMap<>();
        stockRequest.put("stock", 15);

        when(productVariantService.updateVariantStock(variantId, 15, tenantId))
                .thenReturn(updatedVariant);

        // When & Then
        mockMvc.perform(patch("/api/products/{productId}/variants/{variantId}/stock", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15));

        verify(productVariantService).updateVariantStock(variantId, 15, tenantId);
    }

    @Test
    @DisplayName("Should add option to product")
    void shouldAddOptionToProduct() throws Exception {
        // Given
        Map<String, Object> optionRequest = new HashMap<>();
        optionRequest.put("optionId", optionId.toString());
        optionRequest.put("required", true);
        optionRequest.put("sortOrder", 1);

        when(productOptionService.addOptionToProduct(productId, optionId, true, 1, tenantId))
                .thenReturn(productOptionDto);

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/options", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(optionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.optionId").value(optionId.toString()))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.sortOrder").value(1));

        verify(productOptionService).addOptionToProduct(productId, optionId, true, 1, tenantId);
    }

    @Test
    @DisplayName("Should get product options")
    void shouldGetProductOptions() throws Exception {
        // Given
        List<ProductOptionDto> options = List.of(productOptionDto);
        when(productOptionService.getProductOptions(productId, tenantId))
                .thenReturn(options);

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/options", productId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].optionId").value(optionId.toString()))
                .andExpect(jsonPath("$[0].option.name").value("Size"));

        verify(productOptionService).getProductOptions(productId, tenantId);
    }

    @Test
    @DisplayName("Should remove option from product")
    void shouldRemoveOptionFromProduct() throws Exception {
        // Given
        doNothing().when(productOptionService).removeOptionFromProduct(productId, optionId, tenantId);

        // When & Then
        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}", productId, optionId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isNoContent());

        verify(productOptionService).removeOptionFromProduct(productId, optionId, tenantId);
    }

    @Test
    @DisplayName("Should handle missing tenant header")
    void shouldHandleMissingTenantHeader() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants", productId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle invalid UUID in path")
    void shouldHandleInvalidUUIDInPath() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants", "invalid-uuid")
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle validation errors in request body")
    void shouldHandleValidationErrorsInRequestBody() throws Exception {
        // Given - Invalid request (missing required fields)
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("productId", productId.toString());
        // Missing sku, price, stockQuantity

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle service exceptions")
    void shouldHandleServiceExceptions() throws Exception {
        // Given
        when(productVariantService.getVariantById(variantId, tenantId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants/{variantId}", productId, variantId)
                .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle duplicate variant combination")
    void shouldHandleDuplicateVariantCombination() throws Exception {
        // Given
        Map<String, Object> variantRequest = new HashMap<>();
        variantRequest.put("productId", productId.toString());
        variantRequest.put("sku", "TEST-001-SM-RED");
        variantRequest.put("price", 95.00);
        variantRequest.put("stockQuantity", 5);
        
        Map<String, String> optionValues = new HashMap<>();
        optionValues.put(optionId.toString(), "4");
        variantRequest.put("optionValues", optionValues);

        when(productVariantService.createVariant(org.mockito.ArgumentMatchers.any(ProductVariantDto.class), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId)))
                .thenThrow(new RuntimeException("variant combination already exists"));

        // When & Then
        mockMvc.perform(post("/api/products/{productId}/variants", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(variantRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should find variant by option values")
    void shouldFindVariantByOptionValues() throws Exception {
        // Given
        Map<String, String> optionValues = Map.of(
                optionId.toString(), "5"
        );
        
        when(productVariantService.findVariantByOptionValues(eq(productId), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId)))
                .thenReturn(Optional.of(productVariantDto));

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants/find", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .param("optionValues", objectMapper.writeValueAsString(optionValues)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(variantId.toString()))
                .andExpect(jsonPath("$.sku").value("TEST-001-SM-RED"));
    }

    @Test
    @DisplayName("Should return 404 when variant not found by option values")
    void shouldReturn404WhenVariantNotFoundByOptionValues() throws Exception {
        // Given
        Map<String, String> optionValues = Map.of(
                optionId.toString(), "6"
        );
        
        when(productVariantService.findVariantByOptionValues(eq(productId), org.mockito.ArgumentMatchers.any(Map.class), eq(tenantId)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/products/{productId}/variants/find", productId)
                .header("X-Tenant-ID", tenantId.toString())
                .param("optionValues", objectMapper.writeValueAsString(optionValues)))
                .andExpect(status().isNotFound());
    }
}
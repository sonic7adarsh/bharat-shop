package com.bharatshop.app.integration;

import com.bharatshop.app.controller.ProductController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration tests for ProductController
 * Tests the complete flow from HTTP request to database operations
 */
@AutoConfigureWebMvc
@Transactional
class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return empty list when no products exist")
    @WithMockUser(roles = "USER")
    void shouldReturnEmptyListWhenNoProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Should create a new product successfully")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateProductSuccessfully() throws Exception {
        // Prepare test data
        Map<String, Object> productRequest = Map.of(
                "name", "Test Product",
                "description", "A test product for integration testing",
                "price", 99.99,
                "categoryId", 1L,
                "stockQuantity", 100,
                "sku", "TEST-001"
        );

        mockMvc.perform(post("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description").value("A test product for integration testing"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.sku").value("TEST-001"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should return 400 when creating product with invalid data")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenCreatingProductWithInvalidData() throws Exception {
        // Prepare invalid test data (missing required fields)
        Map<String, Object> invalidProductRequest = Map.of(
                "description", "A product without name",
                "price", -10.0  // Invalid negative price
        );

        mockMvc.perform(post("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProductRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("Should return 404 when getting non-existent product")
    @WithMockUser(roles = "USER")
    void shouldReturn404WhenGettingNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/products/999")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return 401 when accessing without authentication")
    void shouldReturn401WhenAccessingWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 when missing tenant header")
    @WithMockUser(roles = "USER")
    void shouldReturn400WhenMissingTenantHeader() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should update product successfully")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateProductSuccessfully() throws Exception {
        // First create a product
        Map<String, Object> createRequest = Map.of(
                "name", "Original Product",
                "description", "Original description",
                "price", 50.0,
                "categoryId", 1L,
                "stockQuantity", 50,
                "sku", "ORIG-001"
        );

        String createResponse = mockMvc.perform(post("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the created product ID
        Map<String, Object> createdProduct = objectMapper.readValue(createResponse, Map.class);
        Long productId = Long.valueOf(createdProduct.get("id").toString());

        // Now update the product
        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Product",
                "description", "Updated description",
                "price", 75.0,
                "categoryId", 1L,
                "stockQuantity", 75,
                "sku", "UPD-001"
        );

        mockMvc.perform(put("/api/products/" + productId)
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.price").value(75.0))
                .andExpect(jsonPath("$.sku").value("UPD-001"));
    }

    @Test
    @DisplayName("Should delete product successfully")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteProductSuccessfully() throws Exception {
        // First create a product
        Map<String, Object> createRequest = Map.of(
                "name", "Product to Delete",
                "description", "This product will be deleted",
                "price", 25.0,
                "categoryId", 1L,
                "stockQuantity", 10,
                "sku", "DEL-001"
        );

        String createResponse = mockMvc.perform(post("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the created product ID
        Map<String, Object> createdProduct = objectMapper.readValue(createResponse, Map.class);
        Long productId = Long.valueOf(createdProduct.get("id").toString());

        // Delete the product
        mockMvc.perform(delete("/api/products/" + productId)
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify the product is deleted
        mockMvc.perform(get("/api/products/" + productId)
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
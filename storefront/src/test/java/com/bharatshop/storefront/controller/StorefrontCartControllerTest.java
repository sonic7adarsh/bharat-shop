package com.bharatshop.storefront.controller;

import com.bharatshop.shared.dto.ApplyCouponRequest;
import com.bharatshop.shared.dto.CartResponse;
import com.bharatshop.shared.dto.CouponResponse;
import com.bharatshop.shared.entity.Cart;
import com.bharatshop.shared.entity.Coupon;
import com.bharatshop.shared.service.CartService;
import com.bharatshop.shared.service.CouponService;
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
import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for StorefrontCartController coupon endpoints
 * Tests REST API endpoints with mocked services
 */
@WebMvcTest(StorefrontCartController.class)
class StorefrontCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private CouponService couponService;

    private Long tenantId;
    private Long customerId;
    private String couponCode;
    private ApplyCouponRequest applyCouponRequest;
    private CartResponse cartResponse;
    private CouponResponse couponResponse;
    private Cart cart;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        customerId = 100L;
        couponCode = "SAVE20";

        // Create test request
        applyCouponRequest = new ApplyCouponRequest();
        applyCouponRequest.setCouponCode(couponCode);

        // Create test coupon
        coupon = Coupon.builder()
                .id(1L)
                .tenantId(tenantId)
                .code(couponCode)
                .type(Coupon.CouponType.PERCENT)
                .value(new BigDecimal("20"))
                .minCartAmount(new BigDecimal("100"))
                .maxDiscountAmount(new BigDecimal("50"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .usageCount(10)
                .isActive(true)
                .build();

        // Create test cart
        cart = Cart.builder()
                .id(1L)
                .tenantId(tenantId)
                .customerId(customerId)
                .appliedCoupon(coupon)
                .discountAmount(new BigDecimal("30.00"))
                .build();

        // Create test responses
        couponResponse = CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType().name())
                .value(coupon.getValue())
                .minCartAmount(coupon.getMinCartAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .isActive(coupon.getIsActive())
                .build();

        cartResponse = CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .appliedCoupon(couponResponse)
                .discountAmount(cart.getDiscountAmount())
                .build();
    }

    @Test
    @DisplayName("Should apply coupon successfully")
    void shouldApplyCouponSuccessfully() throws Exception {
        // Given
        when(cartService.applyCouponToCart(eq(tenantId), eq(customerId), eq(couponCode)))
                .thenReturn(cartResponse);

        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyCouponRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(cart.getId().intValue())))
                .andExpect(jsonPath("$.customerId", is(customerId.intValue())))
                .andExpect(jsonPath("$.appliedCoupon.code", is(couponCode)))
                .andExpect(jsonPath("$.appliedCoupon.type", is("PERCENT")))
                .andExpect(jsonPath("$.appliedCoupon.value", is(20)))
                .andExpect(jsonPath("$.discountAmount", is(30.00)));

        verify(cartService).applyCouponToCart(tenantId, customerId, couponCode);
    }

    @Test
    @DisplayName("Should return bad request when coupon code is missing")
    void shouldReturnBadRequestWhenCouponCodeMissing() throws Exception {
        // Given
        ApplyCouponRequest invalidRequest = new ApplyCouponRequest();
        // couponCode is null

        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when coupon code is empty")
    void shouldReturnBadRequestWhenCouponCodeEmpty() throws Exception {
        // Given
        ApplyCouponRequest invalidRequest = new ApplyCouponRequest();
        invalidRequest.setCouponCode("");

        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when tenant ID header is missing")
    void shouldReturnBadRequestWhenTenantIdMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyCouponRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when customer ID header is missing")
    void shouldReturnBadRequestWhenCustomerIdMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyCouponRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle service exception when applying coupon")
    void shouldHandleServiceExceptionWhenApplyingCoupon() throws Exception {
        // Given
        when(cartService.applyCouponToCart(eq(tenantId), eq(customerId), eq(couponCode)))
                .thenThrow(new RuntimeException("Coupon not found"));

        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyCouponRequest)))
                .andExpect(status().isInternalServerError());

        verify(cartService).applyCouponToCart(tenantId, customerId, couponCode);
    }

    @Test
    @DisplayName("Should remove coupon successfully")
    void shouldRemoveCouponSuccessfully() throws Exception {
        // Given
        CartResponse cartWithoutCoupon = CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .appliedCoupon(null)
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(cartService.removeCouponFromCart(eq(tenantId), eq(customerId)))
                .thenReturn(cartWithoutCoupon);

        // When & Then
        mockMvc.perform(delete("/store/cart/remove-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(cart.getId().intValue())))
                .andExpect(jsonPath("$.customerId", is(customerId.intValue())))
                .andExpect(jsonPath("$.appliedCoupon").doesNotExist())
                .andExpect(jsonPath("$.discountAmount", is(0)));

        verify(cartService).removeCouponFromCart(tenantId, customerId);
    }

    @Test
    @DisplayName("Should return bad request when removing coupon without tenant ID")
    void shouldReturnBadRequestWhenRemovingCouponWithoutTenantId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/store/cart/remove-coupon")
                        .header("X-Customer-ID", customerId))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).removeCouponFromCart(any(), any());
    }

    @Test
    @DisplayName("Should return bad request when removing coupon without customer ID")
    void shouldReturnBadRequestWhenRemovingCouponWithoutCustomerId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/store/cart/remove-coupon")
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).removeCouponFromCart(any(), any());
    }

    @Test
    @DisplayName("Should handle service exception when removing coupon")
    void shouldHandleServiceExceptionWhenRemovingCoupon() throws Exception {
        // Given
        when(cartService.removeCouponFromCart(eq(tenantId), eq(customerId)))
                .thenThrow(new RuntimeException("Cart not found"));

        // When & Then
        mockMvc.perform(delete("/store/cart/remove-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId))
                .andExpect(status().isInternalServerError());

        verify(cartService).removeCouponFromCart(tenantId, customerId);
    }

    @Test
    @DisplayName("Should handle invalid JSON in request body")
    void shouldHandleInvalidJsonInRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle missing request body")
    void shouldHandleMissingRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle unsupported media type")
    void shouldHandleUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text content"))
                .andExpect(status().isUnsupportedMediaType());

        verify(cartService, never()).applyCouponToCart(any(), any(), any());
    }

    @Test
    @DisplayName("Should apply coupon with special characters in code")
    void shouldApplyCouponWithSpecialCharactersInCode() throws Exception {
        // Given
        String specialCouponCode = "SAVE-20%";
        ApplyCouponRequest specialRequest = new ApplyCouponRequest();
        specialRequest.setCouponCode(specialCouponCode);

        CartResponse specialCartResponse = CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomerId())
                .appliedCoupon(CouponResponse.builder()
                        .code(specialCouponCode)
                        .type("PERCENT")
                        .value(new BigDecimal("20"))
                        .build())
                .discountAmount(new BigDecimal("25.00"))
                .build();

        when(cartService.applyCouponToCart(eq(tenantId), eq(customerId), eq(specialCouponCode)))
                .thenReturn(specialCartResponse);

        // When & Then
        mockMvc.perform(post("/store/cart/apply-coupon")
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Customer-ID", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(specialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCoupon.code", is(specialCouponCode)));

        verify(cartService).applyCouponToCart(tenantId, customerId, specialCouponCode);
    }
}
package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.Coupon;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for coupon operations
 */
@Data
@Builder
public class CouponResponse {
    
    private Long id;
    private String code;
    private String type;
    private BigDecimal value;
    private BigDecimal discountAmount;
    private BigDecimal minCartAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description;
    private Boolean isActive;
    
    /**
     * Create CouponResponse from Coupon entity
     */
    public static CouponResponse fromEntity(Coupon coupon, BigDecimal discountAmount) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType().name())
                .value(coupon.getValue())
                .discountAmount(discountAmount)
                .minCartAmount(coupon.getMinCartAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .description(coupon.getDescription())
                .isActive(coupon.getIsActive())
                .build();
    }
    
    /**
     * Create CouponResponse from Coupon entity without discount amount
     */
    public static CouponResponse fromEntity(Coupon coupon) {
        return fromEntity(coupon, BigDecimal.ZERO);
    }
}
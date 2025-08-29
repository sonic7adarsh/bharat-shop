package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponseDto {

    private String orderId;
    private String currency;
    private BigDecimal amount;
    private String key;
    private String name;
    private String description;
    private String image;
    private UUID subscriptionId;
    private String prefillName;
    private String prefillEmail;
    private String prefillContact;
    private String theme;
    private String callbackUrl;
    private String cancelUrl;
    
    // Plan details for display
    private String planName;
    private String planDescription;
    private Integer planDurationDays;
}
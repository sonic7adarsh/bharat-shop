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
    
    // Manual builder method for compilation compatibility
    public static RazorpayOrderResponseDtoBuilder builder() {
        return new RazorpayOrderResponseDtoBuilder();
    }
    
    public static class RazorpayOrderResponseDtoBuilder {
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
        private String planName;
        private String planDescription;
        private Integer planDurationDays;
        
        public RazorpayOrderResponseDtoBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder key(String key) {
            this.key = key;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder image(String image) {
            this.image = image;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder subscriptionId(UUID subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder prefillName(String prefillName) {
            this.prefillName = prefillName;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder prefillEmail(String prefillEmail) {
            this.prefillEmail = prefillEmail;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder prefillContact(String prefillContact) {
            this.prefillContact = prefillContact;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder theme(String theme) {
            this.theme = theme;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder cancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder planName(String planName) {
            this.planName = planName;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder planDescription(String planDescription) {
            this.planDescription = planDescription;
            return this;
        }
        
        public RazorpayOrderResponseDtoBuilder planDurationDays(Integer planDurationDays) {
            this.planDurationDays = planDurationDays;
            return this;
        }
        
        public RazorpayOrderResponseDto build() {
            RazorpayOrderResponseDto dto = new RazorpayOrderResponseDto();
            dto.orderId = this.orderId;
            dto.currency = this.currency;
            dto.amount = this.amount;
            dto.key = this.key;
            dto.name = this.name;
            dto.description = this.description;
            dto.image = this.image;
            dto.subscriptionId = this.subscriptionId;
            dto.prefillName = this.prefillName;
            dto.prefillEmail = this.prefillEmail;
            dto.prefillContact = this.prefillContact;
            dto.theme = this.theme;
            dto.callbackUrl = this.callbackUrl;
            dto.cancelUrl = this.cancelUrl;
            dto.planName = this.planName;
            dto.planDescription = this.planDescription;
            dto.planDurationDays = this.planDurationDays;
            return dto;
        }
    }
}
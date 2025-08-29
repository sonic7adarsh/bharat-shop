package com.bharatshop.storefront.dto;

import lombok.Data;

import java.util.Map;

@Data
public class WebhookRequest {
    
    private String entity;
    private String account_id;
    private String event;
    private Map<String, Object> payload;
    private Long created_at;
    
    @Data
    public static class PaymentEntity {
        private String id;
        private String entity;
        private Long amount;
        private String currency;
        private String status;
        private String order_id;
        private String invoice_id;
        private Boolean international;
        private String method;
        private Long amount_refunded;
        private String refund_status;
        private Boolean captured;
        private String description;
        private String card_id;
        private String bank;
        private String wallet;
        private String vpa;
        private String email;
        private String contact;
        private Map<String, Object> notes;
        private Long fee;
        private Long tax;
        private String error_code;
        private String error_description;
        private String error_source;
        private String error_step;
        private String error_reason;
        private String acquirer_data;
        private Long created_at;
    }
}
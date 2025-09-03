package com.bharatshop.storefront.dto;


import java.util.Map;

public class WebhookRequest {
    
    private String entity;
    private String account_id;
    private String event;
    private Map<String, Object> payload;
    private Long created_at;
    
    // Manual getter methods
    public String getEntity() {
        return entity;
    }
    
    public String getAccount_id() {
        return account_id;
    }
    
    public String getEvent() {
        return event;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public Long getCreated_at() {
        return created_at;
    }
    
    // Manual setter methods
    public void setEntity(String entity) {
        this.entity = entity;
    }
    
    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }
    
    public void setEvent(String event) {
        this.event = event;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public void setCreated_at(Long created_at) {
        this.created_at = created_at;
    }
    
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
        
        // Manual getter methods for PaymentEntity
        public String getId() { return id; }
        public String getEntity() { return entity; }
        public Long getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getStatus() { return status; }
        public String getOrder_id() { return order_id; }
        public String getInvoice_id() { return invoice_id; }
        public Boolean getInternational() { return international; }
        public String getMethod() { return method; }
        public Long getAmount_refunded() { return amount_refunded; }
        public String getRefund_status() { return refund_status; }
        public Boolean getCaptured() { return captured; }
        public String getDescription() { return description; }
        public String getCard_id() { return card_id; }
        public String getBank() { return bank; }
        public String getWallet() { return wallet; }
        public String getVpa() { return vpa; }
        public String getEmail() { return email; }
        public String getContact() { return contact; }
        public Map<String, Object> getNotes() { return notes; }
        public Long getFee() { return fee; }
        public Long getTax() { return tax; }
        public String getError_code() { return error_code; }
        public String getError_description() { return error_description; }
        public String getError_source() { return error_source; }
        public String getError_step() { return error_step; }
        public String getError_reason() { return error_reason; }
        public String getAcquirer_data() { return acquirer_data; }
        public Long getCreated_at() { return created_at; }
    }
}
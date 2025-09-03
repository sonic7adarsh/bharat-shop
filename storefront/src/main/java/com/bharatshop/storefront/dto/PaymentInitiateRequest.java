package com.bharatshop.storefront.dto;

import jakarta.validation.constraints.NotNull;

public class PaymentInitiateRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    private String notes;
    
    // Manual getter methods
    public Long getOrderId() {
        return orderId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    // Manual setter methods
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
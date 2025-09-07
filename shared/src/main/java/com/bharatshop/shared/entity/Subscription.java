package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.BaseEntity;
import com.bharatshop.shared.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
// import java.util.UUID; // Replaced with Long

@Entity
@Table(name = "subscriptions")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Subscription extends BaseEntity {

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "razorpay_subscription_id")
    private String razorpaySubscriptionId;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "auto_renew")
    private Boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_reason")
    private String cancelledReason;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    // Helper methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && 
               LocalDateTime.now().isBefore(endDate);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    public boolean isExpiringSoon(int days) {
        return LocalDateTime.now().plusDays(days).isAfter(endDate);
    }

    public long getDaysRemaining() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), endDate).toDays();
    }

    // Manual getters for fields that Lombok might not be generating
    public Long getId() {
        return this.id;
    }
    
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    
    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }
    
    public Long getTenantId() {
        return this.tenantId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public Long getPlanId() {
        return planId;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getRazorpaySubscriptionId() {
        return razorpaySubscriptionId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public LocalDateTime getNextBillingDate() {
        return nextBillingDate;
    }
    
    // Setter methods
    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }
    
    public void setPlanId(Long planId) {
        this.planId = planId;
    }
    
    public void setPlan(Plan plan) {
        this.plan = plan;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }
    
    public void setRazorpaySubscriptionId(String razorpaySubscriptionId) {
        this.razorpaySubscriptionId = razorpaySubscriptionId;
    }
    
    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }
    
    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }
    
    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }
    
    public void setNextBillingDate(LocalDateTime nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
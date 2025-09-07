package com.bharatshop.shared.dto;

import com.bharatshop.shared.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// import java.util.UUID; // Replaced with Long

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {

    private Long id;
    private Long vendorId;
    private Long planId;
    private PlanResponseDto plan;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private SubscriptionStatus status;
    private String razorpaySubscriptionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private Boolean autoRenew;
    private LocalDateTime cancelledAt;
    private String cancelledReason;
    private LocalDateTime nextBillingDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Boolean isActive;
    private Boolean isExpired;
    private Boolean isExpiringSoon;
    private Long daysRemaining;
    private String statusText;
    private String timeRemaining;
    
    // Helper methods for computed fields
    public Boolean getIsActive() {
        return status == SubscriptionStatus.ACTIVE && 
               LocalDateTime.now().isBefore(endDate);
    }
    
    public Boolean getIsExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }
    
    public Boolean getIsExpiringSoon() {
        return LocalDateTime.now().plusDays(7).isAfter(endDate);
    }
    
    public Long getDaysRemaining() {
        if (getIsExpired()) {
            return 0L;
        }
        return java.time.Duration.between(LocalDateTime.now(), endDate).toDays();
    }
    
    public String getStatusText() {
        if (status == null) return "Unknown";
        
        switch (status) {
            case ACTIVE:
                return getIsExpired() ? "Expired" : "Active";
            case PENDING:
                return "Payment Pending";
            case CANCELLED:
                return "Cancelled";
            case EXPIRED:
                return "Expired";
            case SUSPENDED:
                return "Suspended";
            case TRIAL:
                return "Trial";
            case PAYMENT_FAILED:
                return "Payment Failed";
            case RENEWAL_PENDING:
                return "Renewal Pending";
            default:
                return status.name();
        }
    }
    
    public String getTimeRemaining() {
        if (getIsExpired()) {
            return "Expired";
        }
        
        long days = getDaysRemaining();
        if (days > 30) {
            long months = days / 30;
            return months + " month" + (months > 1 ? "s" : "") + " remaining";
        } else if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " remaining";
        } else {
            return "Expires today";
        }
    }
    
    // Manual builder method for compilation compatibility
    public static SubscriptionResponseDtoBuilder builder() {
        return new SubscriptionResponseDtoBuilder();
    }
    
    public static class SubscriptionResponseDtoBuilder {
        private Long id;
        private Long vendorId;
        private Long planId;
        private PlanResponseDto plan;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private SubscriptionStatus status;
        private String razorpaySubscriptionId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private Boolean autoRenew;
        private LocalDateTime cancelledAt;
        private String cancelledReason;
        private LocalDateTime nextBillingDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public SubscriptionResponseDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder vendorId(Long vendorId) {
            this.vendorId = vendorId;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder planId(Long planId) {
            this.planId = planId;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder plan(PlanResponseDto plan) {
            this.plan = plan;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder status(SubscriptionStatus status) {
            this.status = status;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder razorpaySubscriptionId(String razorpaySubscriptionId) {
            this.razorpaySubscriptionId = razorpaySubscriptionId;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder autoRenew(Boolean autoRenew) {
            this.autoRenew = autoRenew;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder cancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder cancelledReason(String cancelledReason) {
            this.cancelledReason = cancelledReason;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder nextBillingDate(LocalDateTime nextBillingDate) {
            this.nextBillingDate = nextBillingDate;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public SubscriptionResponseDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public SubscriptionResponseDto build() {
            SubscriptionResponseDto dto = new SubscriptionResponseDto();
            dto.id = this.id;
            dto.vendorId = this.vendorId;
            dto.planId = this.planId;
            dto.plan = this.plan;
            dto.startDate = this.startDate;
            dto.endDate = this.endDate;
            dto.status = this.status;
            dto.razorpaySubscriptionId = this.razorpaySubscriptionId;
            dto.razorpayOrderId = this.razorpayOrderId;
            dto.razorpayPaymentId = this.razorpayPaymentId;
            dto.autoRenew = this.autoRenew;
            dto.cancelledAt = this.cancelledAt;
            dto.cancelledReason = this.cancelledReason;
            dto.nextBillingDate = this.nextBillingDate;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            return dto;
        }
    }
}
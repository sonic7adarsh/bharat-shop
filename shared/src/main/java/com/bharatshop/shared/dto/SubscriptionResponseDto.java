package com.bharatshop.shared.dto;

import com.bharatshop.shared.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {

    private UUID id;
    private UUID vendorId;
    private UUID planId;
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
}
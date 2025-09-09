package com.bharatshop.shared.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a return merchandise authorization (RMA) request.
 * Handles return requests for orders with items, reasons, and supporting images.
 */
@Entity
@Table(name = "return_requests")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE return_requests SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ReturnRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Orders order;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private User customer;

    @Column(name = "return_number", unique = true, nullable = false, length = 50)
    private String returnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatus status = ReturnStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false)
    private ReturnType returnType;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "customer_comments", length = 2000)
    private String customerComments;

    @Column(name = "admin_comments", length = 2000)
    private String adminComments;

    @Column(name = "total_return_amount", precision = 10, scale = 2)
    private BigDecimal totalReturnAmount;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "pickup_scheduled_at")
    private LocalDateTime pickupScheduledAt;

    @Column(name = "pickup_completed_at")
    private LocalDateTime pickupCompletedAt;

    @Column(name = "quality_check_completed_at")
    private LocalDateTime qualityCheckCompletedAt;

    @Column(name = "refund_processed_at")
    private LocalDateTime refundProcessedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnRequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnRequestImage> images = new ArrayList<>();

    // Enums

    public enum ReturnStatus {
        PENDING("Return request submitted, awaiting approval"),
        APPROVED("Return request approved, pickup scheduled"),
        REJECTED("Return request rejected"),
        PICKUP_SCHEDULED("Pickup scheduled with logistics partner"),
        PICKED_UP("Items picked up from customer"),
        IN_TRANSIT("Items in transit to warehouse"),
        RECEIVED("Items received at warehouse"),
        QUALITY_CHECK("Items under quality inspection"),
        QUALITY_APPROVED("Quality check passed, refund processing"),
        QUALITY_REJECTED("Quality check failed, items being returned"),
        REFUND_PROCESSED("Refund processed successfully"),
        COMPLETED("Return process completed"),
        CANCELLED("Return request cancelled");

        private final String description;

        ReturnStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isTerminal() {
            return this == COMPLETED || this == CANCELLED || this == QUALITY_REJECTED;
        }

        public boolean canTransitionTo(ReturnStatus newStatus) {
            if (isTerminal()) {
                return false;
            }

            return switch (this) {
                case PENDING -> newStatus == APPROVED || newStatus == REJECTED || newStatus == CANCELLED;
                case APPROVED -> newStatus == PICKUP_SCHEDULED || newStatus == CANCELLED;
                case PICKUP_SCHEDULED -> newStatus == PICKED_UP || newStatus == CANCELLED;
                case PICKED_UP -> newStatus == IN_TRANSIT;
                case IN_TRANSIT -> newStatus == RECEIVED;
                case RECEIVED -> newStatus == QUALITY_CHECK;
                case QUALITY_CHECK -> newStatus == QUALITY_APPROVED || newStatus == QUALITY_REJECTED;
                case QUALITY_APPROVED -> newStatus == REFUND_PROCESSED;
                case REFUND_PROCESSED -> newStatus == COMPLETED;
                default -> false;
            };
        }
    }

    public enum ReturnType {
        DEFECTIVE("Item is defective or damaged"),
        WRONG_ITEM("Wrong item received"),
        SIZE_ISSUE("Size doesn't fit"),
        NOT_AS_DESCRIBED("Item not as described"),
        QUALITY_ISSUE("Quality not satisfactory"),
        CHANGED_MIND("Customer changed mind"),
        DUPLICATE_ORDER("Duplicate order placed"),
        OTHER("Other reason");

        private final String description;

        ReturnType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEligibleForFullRefund() {
            return this == DEFECTIVE || this == WRONG_ITEM || this == NOT_AS_DESCRIBED;
        }
    }

    // Business methods

    public boolean canBeApproved() {
        return status == ReturnStatus.PENDING;
    }

    public boolean canBeRejected() {
        return status == ReturnStatus.PENDING;
    }

    public boolean canBeCancelled() {
        return !status.isTerminal() && status != ReturnStatus.REFUND_PROCESSED;
    }

    public boolean isRefundEligible() {
        return status == ReturnStatus.QUALITY_APPROVED;
    }

    public boolean isCompleted() {
        return status == ReturnStatus.COMPLETED;
    }

    public void addItem(ReturnRequestItem item) {
        items.add(item);
        item.setReturnRequest(this);
    }

    public void addImage(ReturnRequestImage image) {
        images.add(image);
        image.setReturnRequest(this);
    }

    public void calculateTotalReturnAmount() {
        this.totalReturnAmount = items.stream()
                .map(ReturnRequestItem::getReturnAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (returnNumber == null) {
            generateReturnNumber();
        }
    }

    private void generateReturnNumber() {
        // Generate return number: RET-YYYYMMDD-HHMMSS-XXX
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%04d%02d%02d-%02d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond());
        
        // Add random suffix to ensure uniqueness
        String suffix = String.format("%03d", (int) (Math.random() * 1000));
        this.returnNumber = "RET-" + timestamp + "-" + suffix;
    }
}
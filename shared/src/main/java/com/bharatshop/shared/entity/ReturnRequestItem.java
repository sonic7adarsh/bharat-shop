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

/**
 * Entity representing individual items in a return request.
 * Links to order items and tracks return quantities and amounts.
 */
@Entity
@Table(name = "return_request_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE return_request_items SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ReturnRequestItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "return_request_id", nullable = false)
    private Long returnRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", insertable = false, updatable = false)
    private ReturnRequest returnRequest;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", insertable = false, updatable = false)
    private OrderItem orderItem;

    @Column(name = "product_variant_id", nullable = false)
    private Long productVariantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;

    @Column(name = "return_quantity", nullable = false)
    private Integer returnQuantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "return_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal returnAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_received")
    private ItemCondition conditionReceived;

    @Column(name = "quality_check_notes", length = 1000)
    private String qualityCheckNotes;

    @Column(name = "approved_return_quantity")
    private Integer approvedReturnQuantity;

    @Column(name = "approved_return_amount", precision = 10, scale = 2)
    private BigDecimal approvedReturnAmount;

    @Column(name = "reason", length = 500)
    private String reason;

    // Enums

    public enum ItemCondition {
        EXCELLENT("Item in excellent condition"),
        GOOD("Item in good condition with minor wear"),
        FAIR("Item shows signs of use but functional"),
        POOR("Item damaged or heavily used"),
        DEFECTIVE("Item is defective or broken"),
        UNOPENED("Item unopened in original packaging");

        private final String description;

        ItemCondition(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEligibleForFullRefund() {
            return this == EXCELLENT || this == GOOD || this == DEFECTIVE || this == UNOPENED;
        }

        public BigDecimal getRefundPercentage() {
            return switch (this) {
                case EXCELLENT, UNOPENED, DEFECTIVE -> BigDecimal.valueOf(1.00); // 100%
                case GOOD -> BigDecimal.valueOf(0.90); // 90%
                case FAIR -> BigDecimal.valueOf(0.70); // 70%
                case POOR -> BigDecimal.valueOf(0.50); // 50%
            };
        }
    }

    // Business methods

    public void calculateReturnAmount() {
        if (returnQuantity != null && unitPrice != null) {
            this.returnAmount = unitPrice.multiply(BigDecimal.valueOf(returnQuantity));
        }
    }

    public void calculateApprovedReturnAmount() {
        if (approvedReturnQuantity != null && unitPrice != null) {
            BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(approvedReturnQuantity));
            
            if (conditionReceived != null) {
                BigDecimal refundPercentage = conditionReceived.getRefundPercentage();
                this.approvedReturnAmount = baseAmount.multiply(refundPercentage);
            } else {
                this.approvedReturnAmount = baseAmount;
            }
        }
    }

    public boolean isFullQuantityReturn() {
        if (orderItem == null || returnQuantity == null) {
            return false;
        }
        return returnQuantity.equals(orderItem.getQuantity());
    }

    public boolean isPartialQuantityReturn() {
        return !isFullQuantityReturn() && returnQuantity != null && returnQuantity > 0;
    }

    public Integer getMaxReturnableQuantity() {
        return orderItem != null ? orderItem.getQuantity() : 0;
    }

    public boolean isReturnQuantityValid() {
        if (returnQuantity == null || returnQuantity <= 0) {
            return false;
        }
        return returnQuantity <= getMaxReturnableQuantity();
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        super.onCreate();
        calculateReturnAmount();
        if (approvedReturnQuantity != null) {
            calculateApprovedReturnAmount();
        }
    }
    
    // Manual getter and setter methods
    public Long getId() {
        return id;
    }
    
    public void setConditionReceived(ItemCondition conditionReceived) {
        this.conditionReceived = conditionReceived;
    }
    
    public void setQualityCheckNotes(String qualityCheckNotes) {
        this.qualityCheckNotes = qualityCheckNotes;
    }
    
    public void setApprovedReturnQuantity(Integer approvedReturnQuantity) {
        this.approvedReturnQuantity = approvedReturnQuantity;
    }
    
    public BigDecimal getApprovedReturnAmount() {
        return approvedReturnAmount;
    }
    
    public ItemCondition getConditionReceived() {
        return conditionReceived;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }
    
    public void setReturnQuantity(Integer returnQuantity) {
        this.returnQuantity = returnQuantity;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setProductVariantId(Long productVariantId) {
        this.productVariantId = productVariantId;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public void setReturnRequest(ReturnRequest returnRequest) {
        this.returnRequest = returnRequest;
    }
    
    public BigDecimal getReturnAmount() {
        if (unitPrice != null && returnQuantity != null) {
            return unitPrice.multiply(BigDecimal.valueOf(returnQuantity));
        }
        return BigDecimal.ZERO;
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.ReturnRequest;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Event classes for return request lifecycle events.
 * These events are published when return requests change state.
 */
public class ReturnRequestEvents {

    /**
     * Event published when a new return request is created
     */
    public record ReturnRequestCreatedEvent(
            ReturnRequest returnRequest,
            String createdBy,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestCreatedEvent of(ReturnRequest returnRequest, String createdBy) {
            return new ReturnRequestCreatedEvent(returnRequest, createdBy, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private String createdBy;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder createdBy(String createdBy) {
                this.createdBy = createdBy;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestCreatedEvent build() {
                return new ReturnRequestCreatedEvent(returnRequest, createdBy, timestamp);
            }
        }
    }

    /**
     * Event published when a return request is approved
     */
    public record ReturnRequestApprovedEvent(
            ReturnRequest returnRequest,
            String approvedBy,
            String approvalNotes,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestApprovedEvent of(ReturnRequest returnRequest, String approvedBy, String notes) {
            return new ReturnRequestApprovedEvent(returnRequest, approvedBy, notes, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private String approvedBy;
            private String approvalNotes;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder approvedBy(String approvedBy) {
                this.approvedBy = approvedBy;
                return this;
            }
            
            public Builder approvalNotes(String approvalNotes) {
                this.approvalNotes = approvalNotes;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestApprovedEvent build() {
                return new ReturnRequestApprovedEvent(returnRequest, approvedBy, approvalNotes, timestamp);
            }
        }
    }

    /**
     * Event published when a return request is rejected
     */
    public record ReturnRequestRejectedEvent(
            ReturnRequest returnRequest,
            String rejectedBy,
            String rejectionReason,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestRejectedEvent of(ReturnRequest returnRequest, String rejectedBy, String reason) {
            return new ReturnRequestRejectedEvent(returnRequest, rejectedBy, reason, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private String rejectedBy;
            private String rejectionReason;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder rejectedBy(String rejectedBy) {
                this.rejectedBy = rejectedBy;
                return this;
            }
            
            public Builder rejectionReason(String rejectionReason) {
                this.rejectionReason = rejectionReason;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestRejectedEvent build() {
                return new ReturnRequestRejectedEvent(returnRequest, rejectedBy, rejectionReason, timestamp);
            }
        }
    }

    /**
     * Event published when a return request is completed (refund processed)
     */
    public record ReturnRequestCompletedEvent(
            ReturnRequest returnRequest,
            String completedBy,
            String refundTransactionId,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestCompletedEvent of(ReturnRequest returnRequest, String completedBy, String refundId) {
            return new ReturnRequestCompletedEvent(returnRequest, completedBy, refundId, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private String completedBy;
            private String refundTransactionId;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder completedBy(String completedBy) {
                this.completedBy = completedBy;
                return this;
            }
            
            public Builder refundTransactionId(String refundTransactionId) {
                this.refundTransactionId = refundTransactionId;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestCompletedEvent build() {
                return new ReturnRequestCompletedEvent(returnRequest, completedBy, refundTransactionId, timestamp);
            }
        }
    }

    /**
     * Event published when return request items are updated
     */
    public record ReturnRequestItemsUpdatedEvent(
            ReturnRequest returnRequest,
            String updatedBy,
            String updateReason,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestItemsUpdatedEvent of(ReturnRequest returnRequest, String updatedBy, String reason) {
            return new ReturnRequestItemsUpdatedEvent(returnRequest, updatedBy, reason, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private String updatedBy;
            private String updateReason;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder updatedBy(String updatedBy) {
                this.updatedBy = updatedBy;
                return this;
            }
            
            public Builder updateReason(String updateReason) {
                this.updateReason = updateReason;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestItemsUpdatedEvent build() {
                return new ReturnRequestItemsUpdatedEvent(returnRequest, updatedBy, updateReason, timestamp);
            }
        }
    }

    /**
     * Event published when return request images are added
     */
    public record ReturnRequestImagesAddedEvent(
            ReturnRequest returnRequest,
            int imageCount,
            String addedBy,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestImagesAddedEvent of(ReturnRequest returnRequest, int count, String addedBy) {
            return new ReturnRequestImagesAddedEvent(returnRequest, count, addedBy, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private int imageCount;
            private String addedBy;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder imageCount(int imageCount) {
                this.imageCount = imageCount;
                return this;
            }
            
            public Builder addedBy(String addedBy) {
                this.addedBy = addedBy;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestImagesAddedEvent build() {
                return new ReturnRequestImagesAddedEvent(returnRequest, imageCount, addedBy, timestamp);
            }
        }
    }

    /**
     * Event published when return request status changes (generic)
     */
    public record ReturnRequestStatusChangedEvent(
            ReturnRequest returnRequest,
            ReturnRequest.ReturnStatus previousStatus,
            ReturnRequest.ReturnStatus newStatus,
            String changedBy,
            String reason,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestStatusChangedEvent of(
                ReturnRequest returnRequest, 
                ReturnRequest.ReturnStatus previousStatus,
                String changedBy, 
                String reason) {
            return new ReturnRequestStatusChangedEvent(returnRequest, previousStatus, returnRequest.getStatus(), changedBy, reason, LocalDateTime.now());
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private ReturnRequest returnRequest;
            private ReturnRequest.ReturnStatus previousStatus;
            private ReturnRequest.ReturnStatus newStatus;
            private String changedBy;
            private String reason;
            private LocalDateTime timestamp;
            
            public Builder returnRequest(ReturnRequest returnRequest) {
                this.returnRequest = returnRequest;
                return this;
            }
            
            public Builder previousStatus(ReturnRequest.ReturnStatus previousStatus) {
                this.previousStatus = previousStatus;
                return this;
            }
            
            public Builder newStatus(ReturnRequest.ReturnStatus newStatus) {
                this.newStatus = newStatus;
                return this;
            }
            
            public Builder changedBy(String changedBy) {
                this.changedBy = changedBy;
                return this;
            }
            
            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public ReturnRequestStatusChangedEvent build() {
                return new ReturnRequestStatusChangedEvent(returnRequest, previousStatus, newStatus, changedBy, reason, timestamp);
            }
        }
        
        public boolean isStatusTransition(ReturnRequest.ReturnStatus from, ReturnRequest.ReturnStatus to) {
            return previousStatus == from && newStatus == to;
        }
        
        public boolean isApproval() {
            return isStatusTransition(ReturnRequest.ReturnStatus.PENDING, ReturnRequest.ReturnStatus.APPROVED);
        }
        
        public boolean isRejection() {
            return isStatusTransition(ReturnRequest.ReturnStatus.PENDING, ReturnRequest.ReturnStatus.REJECTED);
        }
        
        public boolean isCompletion() {
            return newStatus == ReturnRequest.ReturnStatus.COMPLETED;
        }
    }
}
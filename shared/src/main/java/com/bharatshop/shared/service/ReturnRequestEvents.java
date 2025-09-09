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
    @Builder
    public record ReturnRequestCreatedEvent(
            ReturnRequest returnRequest,
            String createdBy,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestCreatedEvent of(ReturnRequest returnRequest, String createdBy) {
            return ReturnRequestCreatedEvent.builder()
                    .returnRequest(returnRequest)
                    .createdBy(createdBy)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Event published when a return request is approved
     */
    @Builder
    public record ReturnRequestApprovedEvent(
            ReturnRequest returnRequest,
            String approvedBy,
            String approvalNotes,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestApprovedEvent of(ReturnRequest returnRequest, String approvedBy, String notes) {
            return ReturnRequestApprovedEvent.builder()
                    .returnRequest(returnRequest)
                    .approvedBy(approvedBy)
                    .approvalNotes(notes)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Event published when a return request is rejected
     */
    @Builder
    public record ReturnRequestRejectedEvent(
            ReturnRequest returnRequest,
            String rejectedBy,
            String rejectionReason,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestRejectedEvent of(ReturnRequest returnRequest, String rejectedBy, String reason) {
            return ReturnRequestRejectedEvent.builder()
                    .returnRequest(returnRequest)
                    .rejectedBy(rejectedBy)
                    .rejectionReason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Event published when a return request is completed (refund processed)
     */
    @Builder
    public record ReturnRequestCompletedEvent(
            ReturnRequest returnRequest,
            String completedBy,
            String refundTransactionId,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestCompletedEvent of(ReturnRequest returnRequest, String completedBy, String refundId) {
            return ReturnRequestCompletedEvent.builder()
                    .returnRequest(returnRequest)
                    .completedBy(completedBy)
                    .refundTransactionId(refundId)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Event published when return request items are updated
     */
    @Builder
    public record ReturnRequestItemsUpdatedEvent(
            ReturnRequest returnRequest,
            String updatedBy,
            String updateReason,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestItemsUpdatedEvent of(ReturnRequest returnRequest, String updatedBy, String reason) {
            return ReturnRequestItemsUpdatedEvent.builder()
                    .returnRequest(returnRequest)
                    .updatedBy(updatedBy)
                    .updateReason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Event published when return request images are added
     */
    @Builder
    public record ReturnRequestImagesAddedEvent(
            ReturnRequest returnRequest,
            int imageCount,
            String addedBy,
            LocalDateTime timestamp
    ) {
        public static ReturnRequestImagesAddedEvent of(ReturnRequest returnRequest, int count, String addedBy) {
            return ReturnRequestImagesAddedEvent.builder()
                    .returnRequest(returnRequest)
                    .imageCount(count)
                    .addedBy(addedBy)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Event published when return request status changes (generic)
     */
    @Builder
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
            return ReturnRequestStatusChangedEvent.builder()
                    .returnRequest(returnRequest)
                    .previousStatus(previousStatus)
                    .newStatus(returnRequest.getStatus())
                    .changedBy(changedBy)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();
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
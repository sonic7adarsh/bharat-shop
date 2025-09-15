package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.*;
import com.bharatshop.shared.exception.BusinessException;
import com.bharatshop.shared.repository.OrderRepository;
import com.bharatshop.shared.repository.ReturnRequestRepository;
import com.bharatshop.shared.service.ReturnRequestEvents.*;
import com.bharatshop.shared.service.MediaService;
import com.bharatshop.shared.entity.MediaFile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Return Merchandise Authorization (RMA) requests.
 * Handles the complete return lifecycle from request to completion.
 */
@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private static final Logger log = LoggerFactory.getLogger(ReturnRequestService.class);
    
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderStateMachineService orderStateMachineService;
    private final RefundService refundService;
    private final MediaService mediaService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new return request
     */
    @Transactional
    public ReturnRequest createReturnRequest(CreateReturnRequestDto dto) {
        // Validate order eligibility
        Orders order = validateOrderForReturn(dto.getOrderId(), dto.getTenantId());
        
        // Create return request
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setTenantId(dto.getTenantId());
        returnRequest.setOrderId(dto.getOrderId());
        returnRequest.setCustomerId(order.getCustomerId());
        returnRequest.setReturnType(dto.getReturnType());
        returnRequest.setReason(dto.getReason());
        returnRequest.setCustomerComments(dto.getCustomerComments());
        returnRequest.setStatus(ReturnRequest.ReturnStatus.PENDING);
        
        // Add items
        for (CreateReturnItemDto itemDto : dto.getItems()) {
            ReturnRequestItem item = createReturnRequestItem(itemDto, order);
            returnRequest.addItem(item);
        }
        
        // Calculate total return amount
        returnRequest.calculateTotalReturnAmount();
        
        // Save return request
        returnRequest = returnRequestRepository.save(returnRequest);
        
        // Update order status to RETURN_REQUESTED
        orderStateMachineService.requestReturn(order.getId(), dto.getTenantId(), 
                "Return request created: " + returnRequest.getReturnNumber());
        
        // Publish event
        eventPublisher.publishEvent(ReturnRequestCreatedEvent.of(returnRequest, "CUSTOMER"));
        
        log.info("Return request created: {} for order {}", 
                returnRequest.getReturnNumber(), order.getId());
        
        return returnRequest;
    }

    /**
     * Add images to return request
     */
    @Transactional
    public ReturnRequest addImages(Long returnRequestId, Long tenantId, 
                                  List<MultipartFile> images, List<ReturnRequestImage.ImageType> imageTypes) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (returnRequest.getStatus().isTerminal()) {
            throw BusinessException.invalidState("ReturnRequest", "Cannot add images to completed return request");
        }
        
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            ReturnRequestImage.ImageType imageType = i < imageTypes.size() ? 
                    imageTypes.get(i) : ReturnRequestImage.ImageType.GENERAL;
            
            ReturnRequestImage returnImage = uploadReturnImage(returnRequest, image, imageType);
            returnRequest.addImage(returnImage);
        }
        
        return returnRequestRepository.save(returnRequest);
    }

    /**
     * Approve return request
     */
    @Transactional
    public ReturnRequest approveReturnRequest(Long returnRequestId, Long tenantId, 
                                            Long approvedBy, String adminComments) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (!returnRequest.canBeApproved()) {
            throw BusinessException.invalidState("ReturnRequest", 
                    "Return request cannot be approved in current status: " + returnRequest.getStatus());
        }
        
        returnRequest.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        returnRequest.setApprovedBy(approvedBy);
        returnRequest.setApprovedAt(LocalDateTime.now());
        returnRequest.setAdminComments(adminComments);
        
        returnRequest = returnRequestRepository.save(returnRequest);
        
        // Publish event
        eventPublisher.publishEvent(ReturnRequestApprovedEvent.of(returnRequest, approvedBy.toString(), adminComments));
        
        log.info("Return request approved: {} by user {}", 
                returnRequest.getReturnNumber(), approvedBy);
        
        return returnRequest;
    }

    /**
     * Reject return request
     */
    @Transactional
    public ReturnRequest rejectReturnRequest(Long returnRequestId, Long tenantId, 
                                           Long rejectedBy, String rejectionReason) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (!returnRequest.canBeRejected()) {
            throw BusinessException.invalidState("ReturnRequest", 
                    "Return request cannot be rejected in current status: " + returnRequest.getStatus());
        }
        
        returnRequest.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        returnRequest.setRejectedBy(rejectedBy);
        returnRequest.setRejectedAt(LocalDateTime.now());
        returnRequest.setRejectionReason(rejectionReason);
        
        returnRequest = returnRequestRepository.save(returnRequest);
        
        // Revert order status if needed
        Orders order = orderRepository.findById(returnRequest.getOrderId()).orElse(null);
        if (order != null && order.getStatus() == Orders.OrderStatus.RETURN_REQUESTED) {
            // Revert to previous status (assuming it was DELIVERED)
            orderStateMachineService.deliverOrder(order.getId(), tenantId);
        }
        
        // Publish event
        eventPublisher.publishEvent(ReturnRequestRejectedEvent.of(returnRequest, rejectedBy.toString(), rejectionReason));
        
        log.info("Return request rejected: {} by user {} - reason: {}", 
                returnRequest.getReturnNumber(), rejectedBy, rejectionReason);
        
        return returnRequest;
    }

    /**
     * Update return status to picked up
     */
    @Transactional
    public ReturnRequest markAsPickedUp(Long returnRequestId, Long tenantId) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (!returnRequest.getStatus().canTransitionTo(ReturnRequest.ReturnStatus.PICKED_UP)) {
            throw BusinessException.invalidState("ReturnRequest", 
                    "Cannot mark as picked up from current status: " + returnRequest.getStatus());
        }
        
        returnRequest.setStatus(ReturnRequest.ReturnStatus.PICKED_UP);
        returnRequest.setPickupCompletedAt(LocalDateTime.now());
        
        return returnRequestRepository.save(returnRequest);
    }

    /**
     * Complete quality check
     */
    @Transactional
    public ReturnRequest completeQualityCheck(Long returnRequestId, Long tenantId, 
                                            QualityCheckDto qualityCheckDto) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.QUALITY_CHECK) {
            throw BusinessException.invalidState("ReturnRequest", 
                    "Quality check can only be performed when status is QUALITY_CHECK");
        }
        
        // Update item conditions and approved quantities
        for (QualityCheckItemDto itemDto : qualityCheckDto.getItems()) {
            ReturnRequestItem item = returnRequest.getItems().stream()
                    .filter(i -> i.getId().equals(itemDto.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> BusinessException.notFound("ReturnRequestItem", itemDto.getItemId()));
            
            item.setConditionReceived(itemDto.getCondition());
            item.setQualityCheckNotes(itemDto.getNotes());
            item.setApprovedReturnQuantity(itemDto.getApprovedQuantity());
            item.calculateApprovedReturnAmount();
        }
        
        // Calculate approved refund amount
        BigDecimal approvedRefundAmount = returnRequest.getItems().stream()
                .map(ReturnRequestItem::getApprovedReturnAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        returnRequest.setRefundAmount(approvedRefundAmount);
        returnRequest.setQualityCheckCompletedAt(LocalDateTime.now());
        
        // Determine next status based on quality check results
        boolean allItemsApproved = returnRequest.getItems().stream()
                .allMatch(item -> item.getConditionReceived() != null && 
                         item.getConditionReceived().isEligibleForFullRefund());
        
        if (allItemsApproved && approvedRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
            returnRequest.setStatus(ReturnRequest.ReturnStatus.QUALITY_APPROVED);
        } else {
            returnRequest.setStatus(ReturnRequest.ReturnStatus.QUALITY_REJECTED);
        }
        
        returnRequest = returnRequestRepository.save(returnRequest);
        
        // Publish event
        eventPublisher.publishEvent(ReturnRequestStatusChangedEvent.of(returnRequest, 
                ReturnRequest.ReturnStatus.QUALITY_CHECK, "SYSTEM", "Quality check completed"));
        
        return returnRequest;
    }

    /**
     * Process refund for approved return
     */
    @Transactional
    public ReturnRequest processRefund(Long returnRequestId, Long tenantId) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (!returnRequest.isRefundEligible()) {
            throw BusinessException.invalidState("ReturnRequest", 
                    "Return request is not eligible for refund");
        }
        
        // Process refund through RefundService
        RefundService.RefundResult refundResult = refundService.processPartialRefund(
                returnRequest.getOrderId(), tenantId, returnRequest.getRefundAmount(), 
                "Return refund for " + returnRequest.getReturnNumber());
        
        if (refundResult.isSuccess()) {
            returnRequest.setStatus(ReturnRequest.ReturnStatus.REFUND_PROCESSED);
            returnRequest.setRefundProcessedAt(LocalDateTime.now());
            
            // Update order status to REFUNDED if full refund
            Orders order = orderRepository.findById(returnRequest.getOrderId()).orElse(null);
            if (order != null && returnRequest.getRefundAmount().compareTo(order.getTotalAmount()) >= 0) {
                orderStateMachineService.refundOrder(order.getId(), tenantId);
            }
        } else {
            throw new BusinessException("Refund processing failed: " + refundResult.getMessage(), "REFUND_PROCESSING_ERROR");
        }
        
        return returnRequestRepository.save(returnRequest);
    }

    /**
     * Complete return request
     */
    @Transactional
    public ReturnRequest completeReturnRequest(Long returnRequestId, Long tenantId) {
        ReturnRequest returnRequest = getReturnRequestWithValidation(returnRequestId, tenantId);
        
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.REFUND_PROCESSED) {
            throw BusinessException.invalidState("ReturnRequest", 
                    "Return can only be completed after refund is processed");
        }
        
        returnRequest.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
        returnRequest.setCompletedAt(LocalDateTime.now());
        
        // Update order status to RETURNED
        orderStateMachineService.markAsReturned(returnRequest.getOrderId(), tenantId);
        
        returnRequest = returnRequestRepository.save(returnRequest);
        
        // Publish event
        eventPublisher.publishEvent(ReturnRequestCompletedEvent.of(returnRequest, "SYSTEM", "return_completed"));
        
        return returnRequest;
    }

    // Query methods

    public Page<ReturnRequest> getReturnRequests(Long tenantId, Pageable pageable) {
        return returnRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public Page<ReturnRequest> getReturnRequestsByStatus(Long tenantId, 
                                                       ReturnRequest.ReturnStatus status, 
                                                       Pageable pageable) {
        return returnRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }

    public Optional<ReturnRequest> getReturnRequestByNumber(String returnNumber, Long tenantId) {
        return returnRequestRepository.findByReturnNumberAndTenantId(returnNumber, tenantId);
    }

    // Private helper methods

    private Orders validateOrderForReturn(Long orderId, Long tenantId) {
        Optional<Orders> orderOpt = orderRepository.findByIdAndTenantId(orderId, tenantId);
        if (orderOpt.isEmpty()) {
            throw BusinessException.notFound("Order", orderId);
        }
        
        Orders order = orderOpt.get();
        if (!order.getStatus().canBeReturned()) {
            throw BusinessException.invalidState("Order", 
                    "Order cannot be returned in current status: " + order.getStatus());
        }
        
        return order;
    }

    private ReturnRequest getReturnRequestWithValidation(Long returnRequestId, Long tenantId) {
        Optional<ReturnRequest> returnRequestOpt = returnRequestRepository
                .findByIdAndTenantId(returnRequestId, tenantId);
        
        if (returnRequestOpt.isEmpty()) {
            throw BusinessException.notFound("ReturnRequest", returnRequestId);
        }
        
        return returnRequestOpt.get();
    }

    private ReturnRequestItem createReturnRequestItem(CreateReturnItemDto itemDto, Orders order) {
        // Find order item
        OrderItem orderItem = order.getItems().stream()
                .filter(oi -> oi.getId().equals(itemDto.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("OrderItem", itemDto.getOrderItemId()));
        
        // Validate return quantity
        if (itemDto.getReturnQuantity() <= 0 || itemDto.getReturnQuantity() > orderItem.getQuantity()) {
            throw BusinessException.invalidInput(
                    String.format("Invalid return quantity %d for order item %d (max: %d)", 
                            itemDto.getReturnQuantity(), orderItem.getId(), orderItem.getQuantity()));
        }
        
        ReturnRequestItem item = new ReturnRequestItem();
        item.setTenantId(order.getTenantId());
        item.setOrderItemId(itemDto.getOrderItemId());
        item.setProductVariantId(orderItem.getVariantId());
        item.setReturnQuantity(itemDto.getReturnQuantity());
        item.setUnitPrice(orderItem.getPrice());
        item.setReason(itemDto.getReason());
        
        return item;
    }

    private ReturnRequestImage uploadReturnImage(ReturnRequest returnRequest, 
                                               MultipartFile image, 
                                               ReturnRequestImage.ImageType imageType) {
        try {
            // Generate presigned upload URL using MediaService
            MediaService.PresignedUploadResponse uploadResponse = mediaService.generatePresignedUploadUrl(
                    returnRequest.getTenantId(), 
                    image.getOriginalFilename(), 
                    image.getContentType(), 
                    image.getSize());
            
            // For now, we'll create the return image with the key as filePath
            // In a real implementation, you would upload to the presigned URL first
            ReturnRequestImage returnImage = new ReturnRequestImage();
            returnImage.setTenantId(returnRequest.getTenantId());
            returnImage.setOriginalFileName(image.getOriginalFilename());
            returnImage.setFilePath(uploadResponse.getKey()); // Use key as file path
            returnImage.setFileSize(image.getSize());
            returnImage.setMimeType(image.getContentType());
            returnImage.setImageType(imageType);
            
            return returnImage;
        } catch (Exception e) {
            log.error("Failed to upload return image for request {}: {}", 
                    returnRequest.getId(), e.getMessage(), e);
            throw BusinessException.invalidInput("Failed to upload return image: " + e.getMessage());
        }
    }

    // DTOs and Events (inner classes for brevity)

    public static class CreateReturnRequestDto {
        private Long tenantId;
        private Long orderId;
        private ReturnRequest.ReturnType returnType;
        private String reason;
        private String customerComments;
        private List<CreateReturnItemDto> items;
        
        public Long getTenantId() {
            return tenantId;
        }
        
        public Long getOrderId() {
            return orderId;
        }
        
        public ReturnRequest.ReturnType getReturnType() {
            return returnType;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getCustomerComments() {
            return customerComments;
        }
        
        public List<CreateReturnItemDto> getItems() {
            return items;
        }
    }

    public static class CreateReturnItemDto {
        private Long orderItemId;
        private Integer returnQuantity;
        private String reason;
        
        public Long getOrderItemId() {
            return orderItemId;
        }
        
        public Integer getReturnQuantity() {
            return returnQuantity;
        }
        
        public String getReason() {
            return reason;
        }
    }

    public static class QualityCheckDto {
        private List<QualityCheckItemDto> items;
        
        public List<QualityCheckItemDto> getItems() {
            return items;
        }
    }

    public static class QualityCheckItemDto {
        private Long itemId;
        private ReturnRequestItem.ItemCondition condition;
        private Integer approvedQuantity;
        private String notes;
        
        public Long getItemId() {
            return itemId;
        }
        
        public ReturnRequestItem.ItemCondition getCondition() {
            return condition;
        }
        
        public Integer getApprovedQuantity() {
            return approvedQuantity;
        }
        
        public String getNotes() {
            return notes;
        }
    }

    // Event classes are now in ReturnRequestEvents.java
}
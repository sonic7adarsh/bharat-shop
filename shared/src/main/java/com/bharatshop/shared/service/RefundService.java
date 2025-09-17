package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.entity.Payment;
import com.bharatshop.shared.exception.BusinessException;
import com.bharatshop.shared.repository.OrderRepository;
import com.bharatshop.shared.repository.PaymentRepository;
import com.bharatshop.shared.tenant.TenantContext;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling refunds through Razorpay integration.
 * Supports both live and sandbox environments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;
    
    @Value("${razorpay.sandbox:true}")
    private boolean isSandbox;
    
    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    /**
     * Process full refund for an order
     */
    @Transactional
    public RefundResult processFullRefund(Long orderId, Long tenantId, String reason) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        Payment payment = getPaymentForOrder(order);
        
        validateRefundEligibility(order, payment);
        
        BigDecimal refundAmount = order.getTotalAmount();
        return processRefund(payment, refundAmount, reason, RefundType.FULL);
    }

    /**
     * Process partial refund for an order
     */
    @Transactional
    public RefundResult processPartialRefund(Long orderId, Long tenantId, BigDecimal refundAmount, String reason) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        Payment payment = getPaymentForOrder(order);
        
        validateRefundEligibility(order, payment);
        validatePartialRefundAmount(order, payment, refundAmount);
        
        return processRefund(payment, refundAmount, reason, RefundType.PARTIAL);
    }

    /**
     * Capture payment (for orders that were authorized but not captured)
     */
    @Transactional
    public CaptureResult capturePayment(Long orderId, Long tenantId, BigDecimal captureAmount) {
        Orders order = getOrderWithValidation(orderId, tenantId);
        Payment payment = getPaymentForOrder(order);
        
        validateCaptureEligibility(payment);
        
        if (isSandbox) {
            return processSandboxCapture(payment, captureAmount);
        } else {
            return processLiveCapture(payment, captureAmount);
        }
    }

    // Private methods

    private RefundResult processRefund(Payment payment, BigDecimal refundAmount, String reason, RefundType type) {
        if (isSandbox) {
            return processSandboxRefund(payment, refundAmount, reason, type);
        } else {
            return processLiveRefund(payment, refundAmount, reason, type);
        }
    }

    private RefundResult processLiveRefund(Payment payment, BigDecimal refundAmount, String reason, RefundType type) {
        try {
            // Convert amount to paise (Razorpay uses paise)
            int amountInPaise = refundAmount.multiply(BigDecimal.valueOf(100)).intValue();
            
            Map<String, Object> refundRequest = new HashMap<>();
            refundRequest.put("amount", amountInPaise);
            refundRequest.put("speed", "normal");
            refundRequest.put("notes", Map.of(
                "reason", reason,
                "type", type.name(),
                "order_id", payment.getOrderId().toString()
            ));
            
            // Create refund using Razorpay Java SDK
            JSONObject refundRequestJson = new JSONObject(refundRequest);
            com.razorpay.Refund refund = razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundRequestJson);
            
            // Update payment record
            updatePaymentAfterRefund(payment, refundAmount, refund.get("id"), type);
            
            System.out.println("Live refund processed: " + refund.get("id") + " for payment " + payment.getId() + " amount " + refundAmount);
            
            return RefundResult.builder()
                    .success(true)
                    .refundId(refund.get("id"))
                    .amount(refundAmount)
                    .status("processing")
                    .message("Refund initiated successfully")
                    .build();
            
        } catch (RazorpayException e) {
            System.out.println("Razorpay refund failed for payment " + payment.getId() + ": " + e.getMessage());
            
            return RefundResult.builder()
                    .success(false)
                    .amount(refundAmount)
                    .status("failed")
                    .message("Refund failed: " + e.getMessage())
                    .error(e.getMessage())
                    .build();
        }
    }

    private RefundResult processSandboxRefund(Payment payment, BigDecimal refundAmount, String reason, RefundType type) {
        // Simulate sandbox refund
        String mockRefundId = "rfnd_sandbox_" + System.currentTimeMillis();
        
        // Update payment record
        updatePaymentAfterRefund(payment, refundAmount, mockRefundId, type);
        
        System.out.println("Sandbox refund processed: " + mockRefundId + " for payment " + payment.getId() + " amount " + refundAmount);
        
        return RefundResult.builder()
                .success(true)
                .refundId(mockRefundId)
                .amount(refundAmount)
                .status("processed")
                .message("Sandbox refund processed successfully")
                .build();
    }

    private CaptureResult processLiveCapture(Payment payment, BigDecimal captureAmount) {
        try {
            int amountInPaise = captureAmount.multiply(BigDecimal.valueOf(100)).intValue();
            
            Map<String, Object> captureRequest = new HashMap<>();
            captureRequest.put("amount", amountInPaise);
            captureRequest.put("currency", "INR");
            
            // Capture payment using Razorpay Java SDK
            JSONObject captureRequestJson = new JSONObject(captureRequest);
            com.razorpay.Payment capturedPayment = razorpayClient.payments.capture(payment.getRazorpayPaymentId(), captureRequestJson);
            
            // Update payment record - Note: Payment entity lacks setCapturedAmount and setCapturedAt methods
            payment.setStatus(Payment.PaymentStatus.CAPTURED);
            paymentRepository.save(payment);
            
            System.out.println("Live capture processed for payment " + payment.getId() + " amount " + captureAmount);
            
            return CaptureResult.builder()
                    .success(true)
                    .capturedAmount(captureAmount)
                    .status("captured")
                    .message("Payment captured successfully")
                    .build();
            
        } catch (RazorpayException e) {
            System.out.println("Razorpay capture failed for payment " + payment.getId() + ": " + e.getMessage());
            
            return CaptureResult.builder()
                    .success(false)
                    .capturedAmount(BigDecimal.ZERO)
                    .status("failed")
                    .message("Capture failed: " + e.getMessage())
                    .error(e.getMessage())
                    .build();
        }
    }

    private CaptureResult processSandboxCapture(Payment payment, BigDecimal captureAmount) {
        // Simulate sandbox capture
        payment.setStatus(Payment.PaymentStatus.CAPTURED);
        // Note: Payment entity doesn't have capturedAmount or capturedAt fields
        paymentRepository.save(payment);
        
        System.out.println("Sandbox capture processed for payment " + payment.getId() + " amount " + captureAmount);
        
        return CaptureResult.builder()
                .success(true)
                .capturedAmount(captureAmount)
                .status("captured")
                .message("Sandbox capture processed successfully")
                .build();
    }

    private void updatePaymentAfterRefund(Payment payment, BigDecimal refundAmount, String refundId, RefundType type) {
        BigDecimal currentRefunded = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal newRefundedAmount = currentRefunded.add(refundAmount);
        
        payment.setRefundAmount(newRefundedAmount);
        payment.setRefundId(refundId);
        // Note: Payment entity doesn't have lastRefundAt field
        
        // Update payment status based on refund type and amount
        if (type == RefundType.FULL || newRefundedAmount.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
        }
        
        paymentRepository.save(payment);
    }

    // Validation methods

    private Orders getOrderWithValidation(Long orderId, Long tenantId) {
        Optional<Orders> orderOpt = orderRepository.findByIdAndTenantId(orderId, tenantId);
        if (orderOpt.isEmpty()) {
            throw BusinessException.notFound("Order", orderId);
        }
        return orderOpt.get();
    }

    private Payment getPaymentForOrder(Orders order) {
        Optional<Payment> paymentOpt = paymentRepository.findByTenantIdAndOrderIdAndStatus(
                order.getTenantId(), order.getId(), Payment.PaymentStatus.CAPTURED)
                .stream().findFirst();
        
        if (paymentOpt.isEmpty()) {
            throw BusinessException.invalidState("Order", "No completed payment found for order");
        }
        
        return paymentOpt.get();
    }

    private void validateRefundEligibility(Orders order, Payment payment) {
        if (payment.getStatus() != Payment.PaymentStatus.CAPTURED && 
            payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw BusinessException.invalidState("Payment", "Payment must be captured to process refund");
        }
        
        if (order.getStatus() == Orders.OrderStatus.REFUNDED) {
            throw BusinessException.invalidState("Order", "Order is already fully refunded");
        }
    }

    private void validatePartialRefundAmount(Orders order, Payment payment, BigDecimal refundAmount) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalidInput("Refund amount must be greater than zero");
        }
        
        BigDecimal currentRefunded = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal maxRefundable = payment.getAmount().subtract(currentRefunded);
        
        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw BusinessException.invalidInput(
                String.format("Refund amount %.2f exceeds maximum refundable amount %.2f", 
                        refundAmount, maxRefundable));
        }
    }

    private void validateCaptureEligibility(Payment payment) {
        if (payment.getStatus() != Payment.PaymentStatus.AUTHORIZED) {
            throw BusinessException.invalidState("Payment", "Payment must be authorized to capture");
        }
    }

    // Result classes

    public enum RefundType {
        FULL, PARTIAL
    }

    @lombok.Data
    @lombok.Builder
    public static class RefundResult {
        private boolean success;
        private String refundId;
        private BigDecimal amount;
        private String status;
        private String message;
        private String error;
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }

    }

    @lombok.Data
    @lombok.Builder
    public static class CaptureResult {
        private boolean success;
        private BigDecimal capturedAmount;
        private String status;
        private String message;
        private String error;
        


    }
}
package com.bharatshop.storefront.controller;

import com.bharatshop.shared.entity.Payment;
import com.bharatshop.storefront.dto.PaymentInitiateRequest;
import com.bharatshop.storefront.dto.PaymentResponse;
import com.bharatshop.storefront.dto.PaymentVerificationRequest;
import com.bharatshop.storefront.dto.WebhookRequest;
import com.bharatshop.storefront.service.OrderService;
import com.bharatshop.storefront.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/store/payments")
@RequiredArgsConstructor
@Slf4j
public class StorefrontPaymentController {
    
    private final PaymentService paymentService;
    private final OrderService orderService;
    
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentInitiateRequest request) {
        try {
            // Get order details to create payment
            var order = orderService.getOrderById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderId()));
            
            // Create Razorpay order
            String receipt = "order_" + order.getOrderNumber();
            Payment payment = paymentService.createRazorpayOrder(
                order.getId(), 
                order.getTotalAmount(), 
                receipt, 
                request.getNotes()
            );
            
            return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
            
        } catch (Exception e) {
            log.error("Failed to initiate payment for order: {}", request.getOrderId(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        try {
            Payment payment = paymentService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
            );
            
            // Update order status to CONFIRMED on successful payment
            if (payment.getStatus() == Payment.PaymentStatus.CAPTURED) {
                orderService.confirmOrder(payment.getOrderId());
            }
            
            return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
            
        } catch (Exception e) {
            log.error("Failed to verify payment: {}", request.getRazorpayPaymentId(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        try {
            return paymentService.getPaymentByOrderId(orderId)
                .map(payment -> ResponseEntity.ok(PaymentResponse.fromEntity(payment)))
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Failed to get payment for order: {}", orderId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/razorpay/{razorpayOrderId}")
    public ResponseEntity<PaymentResponse> getPaymentByRazorpayOrderId(@PathVariable String razorpayOrderId) {
        try {
            return paymentService.getPaymentByRazorpayOrderId(razorpayOrderId)
                .map(payment -> ResponseEntity.ok(PaymentResponse.fromEntity(payment)))
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            log.error("Failed to get payment for Razorpay order: {}", razorpayOrderId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
        @RequestBody WebhookRequest webhookRequest,
        @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        try {
            log.info("Received webhook: {} for entity: {}", webhookRequest.getEvent(), webhookRequest.getEntity());
            
            // Handle different webhook events
            switch (webhookRequest.getEvent()) {
                case "payment.captured":
                    handlePaymentCaptured(webhookRequest);
                    break;
                case "payment.failed":
                    handlePaymentFailed(webhookRequest);
                    break;
                case "order.paid":
                    handleOrderPaid(webhookRequest);
                    break;
                default:
                    log.info("Unhandled webhook event: {}", webhookRequest.getEvent());
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
            
        } catch (Exception e) {
            log.error("Failed to handle webhook", e);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    private void handlePaymentCaptured(WebhookRequest webhookRequest) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) webhookRequest.getPayload().get("payment");
            
            if (paymentData != null) {
                String razorpayPaymentId = (String) paymentData.get("id");
                String razorpayOrderId = (String) paymentData.get("order_id");
                
                // Find and update payment
                paymentService.getPaymentByRazorpayOrderId(razorpayOrderId)
                    .ifPresent(payment -> {
                        if (payment.getStatus() != Payment.PaymentStatus.CAPTURED) {
                            // Update payment status via webhook
                            // Note: In production, you should verify the webhook signature
                            log.info("Payment captured via webhook: {} for order: {}", 
                                razorpayPaymentId, payment.getOrderId());
                            
                            // Confirm order
                            orderService.confirmOrder(payment.getOrderId());
                        }
                    });
            }
        } catch (Exception e) {
            log.error("Failed to handle payment.captured webhook", e);
        }
    }
    
    private void handlePaymentFailed(WebhookRequest webhookRequest) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) webhookRequest.getPayload().get("payment");
            
            if (paymentData != null) {
                String razorpayOrderId = (String) paymentData.get("order_id");
                String errorDescription = (String) paymentData.get("error_description");
                
                // Find and update payment
                paymentService.getPaymentByRazorpayOrderId(razorpayOrderId)
                    .ifPresent(payment -> {
                        log.info("Payment failed via webhook for order: {} - {}", 
                            payment.getOrderId(), errorDescription);
                    });
            }
        } catch (Exception e) {
            log.error("Failed to handle payment.failed webhook", e);
        }
    }
    
    private void handleOrderPaid(WebhookRequest webhookRequest) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = (Map<String, Object>) webhookRequest.getPayload().get("order");
            
            if (orderData != null) {
                String razorpayOrderId = (String) orderData.get("id");
                
                // Find and confirm order
                paymentService.getPaymentByRazorpayOrderId(razorpayOrderId)
                    .ifPresent(payment -> {
                        log.info("Order paid via webhook for order: {}", payment.getOrderId());
                        orderService.confirmOrder(payment.getOrderId());
                    });
            }
        } catch (Exception e) {
            log.error("Failed to handle order.paid webhook", e);
        }
    }
}
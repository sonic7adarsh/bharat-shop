package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.Payment;
import com.bharatshop.shared.entity.PaymentGateway;
import com.bharatshop.shared.repository.PaymentGatewayRepository;
import com.bharatshop.shared.repository.PaymentRepository;
import com.bharatshop.platform.tenant.TenantContext;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRepository paymentGatewayRepository;
    
    @Value("${app.payment.razorpay.key-id}")
    private String defaultKeyId;
    
    @Value("${app.payment.razorpay.key-secret}")
    private String defaultKeySecret;
    
    @Value("${app.payment.razorpay.webhook-secret}")
    private String defaultWebhookSecret;
    
    @Value("${app.payment.razorpay.currency}")
    private String defaultCurrency;
    
    @Transactional
    public Payment createRazorpayOrder(Long orderId, BigDecimal amount, String receipt, String notes) {
        Long tenantId = Long.valueOf(TenantContext.getTenantId());
        
        try {
            // Get payment gateway configuration for tenant
            PaymentGateway gateway = getActiveRazorpayGateway(tenantId);
            
            // Create Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(gateway.getKeyId(), gateway.getKeySecret());
            
            // Create order request
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Amount in paise
            orderRequest.put("currency", gateway.getCurrency());
            orderRequest.put("receipt", receipt);
            orderRequest.put("notes", new JSONObject(notes != null ? notes : "{}"));
            
            // Create order in Razorpay
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            
            // Create payment record
            Payment payment = Payment.builder()
                .tenantId(tenantId)
                .orderId(orderId)
                .paymentGateway(gateway)
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(amount)
                .currency(gateway.getCurrency())
                .status(Payment.PaymentStatus.CREATED)
                .description("Order payment for order: " + orderId)
                .receipt(receipt)
                .notes(notes)
                .webhookVerified(false)
                .build();
            
            payment = paymentRepository.save(payment);
            
            log.info("Created Razorpay order: {} for order: {} in tenant: {}", 
                razorpayOrder.get("id"), orderId, tenantId);
            
            return payment;
            
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for order: {} in tenant: {}", orderId, tenantId, e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public Payment verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Long tenantId = Long.valueOf(TenantContext.getTenantId());
        
        try {
            // Find payment by Razorpay order ID
            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for Razorpay order: " + razorpayOrderId));
            
            // Verify signature
            PaymentGateway gateway = payment.getPaymentGateway();
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            
            boolean isValidSignature = Utils.verifyPaymentSignature(attributes, gateway.getKeySecret());
            
            if (isValidSignature) {
                // Update payment status
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setRazorpaySignature(razorpaySignature);
                payment.setStatus(Payment.PaymentStatus.CAPTURED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setWebhookVerified(true);
                
                payment = paymentRepository.save(payment);
                
                log.info("Payment verified successfully: {} for order: {} in tenant: {}", 
                    razorpayPaymentId, payment.getOrderId(), tenantId);
                
                return payment;
            } else {
                // Mark payment as failed
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailureReason("Invalid payment signature");
                payment.setFailedAt(LocalDateTime.now());
                
                payment = paymentRepository.save(payment);
                
                log.warn("Payment verification failed for order: {} in tenant: {}", 
                    payment.getOrderId(), tenantId);
                
                throw new RuntimeException("Payment verification failed");
            }
            
        } catch (RazorpayException e) {
            log.error("Failed to verify payment: {} in tenant: {}", razorpayPaymentId, tenantId, e);
            throw new RuntimeException("Payment verification failed: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        Long tenantId = Long.valueOf(TenantContext.getTenantId());
        return paymentRepository.findByTenantIdAndOrderIdAndStatus(
            tenantId, orderId, Payment.PaymentStatus.CAPTURED
        ).stream().findFirst();
    }
    
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByRazorpayOrderId(String razorpayOrderId) {
        return paymentRepository.findByRazorpayOrderId(razorpayOrderId);
    }
    
    private PaymentGateway getActiveRazorpayGateway(Long tenantId) {
        // Try to get tenant-specific Razorpay configuration
        Optional<PaymentGateway> tenantGateway = paymentGatewayRepository
            .findActiveByTenantIdAndGatewayType(tenantId, PaymentGateway.GatewayType.RAZORPAY);
        
        if (tenantGateway.isPresent()) {
            return tenantGateway.get();
        }
        
        // Fallback to system default configuration
        return PaymentGateway.builder()
            .tenantId(tenantId)
            .gatewayType(PaymentGateway.GatewayType.RAZORPAY)
            .gatewayName("Razorpay")
            .keyId(defaultKeyId)
            .keySecret(defaultKeySecret)
            .webhookSecret(defaultWebhookSecret)
            .currency(defaultCurrency)
            .isActive(true)
            .isTestMode(true)
            .build();
    }
}
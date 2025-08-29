package com.bharatshop.shared.service;

import com.bharatshop.shared.dto.RazorpayOrderResponseDto;
import com.bharatshop.shared.dto.SubscriptionResponseDto;
import com.bharatshop.shared.entity.Plan;
import com.bharatshop.shared.entity.Subscription;
import com.bharatshop.shared.enums.SubscriptionStatus;
import com.bharatshop.shared.repository.PlanRepository;
import com.bharatshop.shared.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final RazorpayService razorpayService;

    private static final String RAZORPAY_BASE_URL = "https://api.razorpay.com/v1";

    /**
     * Get active subscription for vendor
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscription(UUID vendorId) {
        log.debug("Fetching active subscription for vendor: {}", vendorId);
        return subscriptionRepository.findActiveByVendorId(vendorId, LocalDateTime.now());
    }

    /**
     * Get current subscription for vendor (active or expired)
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getCurrentSubscription(UUID vendorId) {
        log.debug("Fetching current subscription for vendor: {}", vendorId);
        return subscriptionRepository.findCurrentByVendorId(vendorId);
    }

    /**
     * Get all subscriptions for vendor
     */
    @Transactional(readOnly = true)
    public List<Subscription> getVendorSubscriptions(UUID vendorId) {
        log.debug("Fetching all subscriptions for vendor: {}", vendorId);
        return subscriptionRepository.findAllByVendorId(vendorId);
    }

    /**
     * Create Razorpay order for subscription
     */
    public RazorpayOrderResponseDto createSubscriptionOrder(UUID vendorId, UUID planId) {
        log.info("Creating subscription order for vendor: {} and plan: {}", vendorId, planId);
        
        // Get plan details
        Plan plan = planRepository.findActiveById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        
        // Check if vendor already has active subscription
        Optional<Subscription> existingSubscription = getActiveSubscription(vendorId);
        if (existingSubscription.isPresent()) {
            throw new IllegalStateException("Vendor already has an active subscription");
        }
        
        try {
            // Create pending subscription record
            Subscription subscription = Subscription.builder()
                    .vendorId(vendorId)
                    .planId(planId)
                    .status(SubscriptionStatus.PENDING)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(plan.getDurationDays()))
                    .autoRenew(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            subscription = subscriptionRepository.save(subscription);
            
            // Create Razorpay order using RazorpayService
            RazorpayOrderResponseDto orderResponse = razorpayService.createOrder(
                    plan, 
                    subscription.getId(), 
                    "customer@example.com", // TODO: Get from vendor profile
                    "Customer Name", // TODO: Get from vendor profile
                    "9999999999" // TODO: Get from vendor profile
            );
            
            // Update subscription with order ID
            subscription.setRazorpayOrderId(orderResponse.getOrderId());
            subscriptionRepository.save(subscription);
            
            log.info("Subscription order created successfully: {}", orderResponse.getOrderId());
            return orderResponse;
            
        } catch (Exception e) {
            log.error("Error creating subscription order", e);
            throw new RuntimeException("Failed to create subscription order: " + e.getMessage());
        }
    }

    /**
     * Verify Razorpay payment and activate subscription
     */
    public Subscription verifyAndActivateSubscription(String orderId, String paymentId, String signature) {
        log.info("Verifying payment for order: {}", orderId);
        
        // Find subscription by order ID
        Subscription subscription = subscriptionRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found for order: " + orderId));
        
        try {
            // Verify payment signature using RazorpayService
            if (!razorpayService.verifyPaymentSignature(orderId, paymentId, signature)) {
                throw new SecurityException("Invalid payment signature");
            }
            
            // Activate subscription
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setRazorpayPaymentId(paymentId);
            subscription.setStartDate(LocalDateTime.now());
            subscription.setNextBillingDate(LocalDateTime.now().plusDays(subscription.getPlan().getDurationDays()));
            subscription.setUpdatedAt(LocalDateTime.now());
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            log.info("Subscription activated successfully: {}", savedSubscription.getId());
            return savedSubscription;
            
        } catch (Exception e) {
            log.error("Error verifying payment", e);
            subscription.setStatus(SubscriptionStatus.PAYMENT_FAILED);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }

    /**
     * Cancel subscription
     */
    public void cancelSubscription(UUID subscriptionId, String reason) {
        log.info("Cancelling subscription: {}", subscriptionId);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancelledReason(reason);
        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
        
        log.info("Subscription cancelled successfully: {}", subscriptionId);
    }

    /**
     * Process subscription renewals
     */
    @Transactional
    public void processRenewals() {
        log.info("Processing subscription renewals");
        
        List<Subscription> subscriptionsForRenewal = subscriptionRepository.findForRenewal(LocalDateTime.now());
        
        for (Subscription subscription : subscriptionsForRenewal) {
            try {
                renewSubscription(subscription);
            } catch (Exception e) {
                log.error("Failed to renew subscription: {}", subscription.getId(), e);
                subscription.setStatus(SubscriptionStatus.RENEWAL_PENDING);
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
            }
        }
        
        log.info("Processed {} subscription renewals", subscriptionsForRenewal.size());
    }

    /**
     * Process expired subscriptions
     */
    @Transactional
    public void processExpiredSubscriptions() {
        log.info("Processing expired subscriptions");
        
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredActive(LocalDateTime.now());
        
        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            log.info("Marked subscription as expired: {}", subscription.getId());
        }
        
        log.info("Processed {} expired subscriptions", expiredSubscriptions.size());
    }

    /**
     * Get subscriptions expiring in next N days
     */
    @Transactional(readOnly = true)
    public List<Subscription> getExpiringSubscriptions(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        return subscriptionRepository.findEndingInDays(now, futureDate);
    }

    /**
     * Check if vendor has active subscription
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(UUID vendorId) {
        return subscriptionRepository.hasActiveSubscription(vendorId, LocalDateTime.now());
    }

    /**
     * Get subscription feature limits
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSubscriptionLimits(UUID vendorId) {
        Optional<Subscription> subscriptionOpt = getActiveSubscription(vendorId);
        
        Map<String, Object> limits = new HashMap<>();
        
        if (subscriptionOpt.isPresent()) {
            Plan plan = subscriptionOpt.get().getPlan();
            limits.put("maxProducts", plan.getMaxProducts());
            limits.put("storageLimit", plan.getStorageLimit());
            limits.put("hasAdvancedFeatures", plan.hasFeature("advancedFeatures"));
            limits.put("hasAnalytics", plan.hasFeature("analytics"));
            limits.put("hasCustomDomain", plan.hasFeature("customDomain"));
        } else {
            // Default free limits
            limits.put("maxProducts", 5);
            limits.put("storageLimit", 100 * 1024 * 1024L); // 100MB
            limits.put("hasAdvancedFeatures", false);
            limits.put("hasAnalytics", false);
            limits.put("hasCustomDomain", false);
        }
        
        return limits;
    }

    /**
     * Handle Razorpay webhook
     */
    public void handleWebhook(String payload, String signature) {
        log.info("Processing Razorpay webhook");
        
        try {
            // Verify webhook signature using RazorpayService
            if (!razorpayService.verifyWebhookSignature(payload, signature)) {
                throw new SecurityException("Invalid webhook signature");
            }
            
            JSONObject webhookData = razorpayService.parseWebhookPayload(payload);
            String event = webhookData.getString("event");
            JSONObject paymentEntity = webhookData.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");
            
            String orderId = paymentEntity.getString("order_id");
            String paymentId = paymentEntity.getString("id");
            
            switch (event) {
                case "payment.captured":
                    handlePaymentCaptured(orderId, paymentId);
                    break;
                case "payment.failed":
                    handlePaymentFailed(orderId, paymentId);
                    break;
                case "subscription.cancelled":
                    handleSubscriptionCancelled(paymentEntity);
                    break;
                default:
                    log.info("Unhandled webhook event: {}", event);
            }
            
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private void handlePaymentCaptured(String orderId, String paymentId) {
        log.info("Handling payment captured for order: {}", orderId);
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByRazorpayOrderId(orderId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setRazorpayPaymentId(paymentId);
            subscription.setStartDate(LocalDateTime.now());
            subscription.setNextBillingDate(LocalDateTime.now().plusDays(subscription.getPlan().getDurationDays()));
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            log.info("Subscription activated via webhook: {}", subscription.getId());
        }
    }

    private void handlePaymentFailed(String orderId, String paymentId) {
        log.info("Handling payment failed for order: {}", orderId);
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByRazorpayOrderId(orderId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(SubscriptionStatus.PAYMENT_FAILED);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            log.info("Subscription marked as payment failed via webhook: {}", subscription.getId());
        }
    }

    private void handleSubscriptionCancelled(JSONObject paymentEntity) {
        log.info("Handling subscription cancelled webhook");
        // Implementation for subscription cancellation via webhook
    }

    // Additional service methods
    
    public Page<SubscriptionResponseDto> getSubscriptionHistory(UUID vendorId, Pageable pageable) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByVendorId(vendorId);
        
        // Convert to DTOs
        List<SubscriptionResponseDto> dtos = subscriptions.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        
        // Create pageable result
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<SubscriptionResponseDto> pageContent = dtos.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, dtos.size());
    }
    
    public JsonNode getCurrentFeatures(UUID vendorId) {
        Optional<Subscription> activeSubscription = subscriptionRepository.findActiveByVendorId(vendorId, LocalDateTime.now());
        return activeSubscription.map(subscription -> subscription.getPlan().getFeatures())
                .orElse(null);
    }
    
    public boolean hasFeature(UUID vendorId, String featureName) {
        Optional<Subscription> activeSubscription = subscriptionRepository.findActiveByVendorId(vendorId, LocalDateTime.now());
        if (activeSubscription.isEmpty()) {
            return false;
        }
        
        JsonNode features = activeSubscription.get().getPlan().getFeatures();
        if (features == null || !features.has(featureName)) {
            return false;
        }
        
        JsonNode featureNode = features.get(featureName);
        return featureNode.isBoolean() ? featureNode.asBoolean() : false;
    }
    
    // Private helper methods
    
    public SubscriptionResponseDto convertToResponseDto(Subscription subscription) {
        return SubscriptionResponseDto.builder()
                .id(subscription.getId())
                .vendorId(subscription.getVendorId())
                .planId(subscription.getPlanId())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .razorpaySubscriptionId(subscription.getRazorpaySubscriptionId())
                .razorpayOrderId(subscription.getRazorpayOrderId())
                .razorpayPaymentId(subscription.getRazorpayPaymentId())
                .autoRenew(subscription.getAutoRenew())
                .cancelledAt(subscription.getCancelledAt())
                .cancelledReason(subscription.getCancelledReason())
                .nextBillingDate(subscription.getNextBillingDate())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
    
    private void renewSubscription(Subscription subscription) {
        log.info("Renewing subscription: {}", subscription.getId());
        
        Plan plan = subscription.getPlan();
        
        // Create renewal order
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("amount", plan.getPrice().multiply(BigDecimal.valueOf(100)).intValue());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "renewal_" + subscription.getId() + "_" + System.currentTimeMillis());
        
        // For now, mark as renewal pending - in real implementation, 
        // you would integrate with Razorpay's subscription API
        subscription.setStatus(SubscriptionStatus.RENEWAL_PENDING);
        subscription.setNextBillingDate(LocalDateTime.now().plusDays(plan.getDurationDays()));
        subscription.setUpdatedAt(LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
    }
}
package com.bharatshop.shared.controller;

import com.bharatshop.shared.dto.PlanResponseDto;
import com.bharatshop.shared.dto.SubscriptionResponseDto;
import com.bharatshop.shared.dto.SubscriptionRequestDto;
import com.bharatshop.shared.dto.RazorpayOrderResponseDto;
import com.bharatshop.shared.entity.Subscription;
import com.bharatshop.shared.entity.Plan;
import com.bharatshop.shared.service.SubscriptionService;
import com.bharatshop.shared.service.PlanService;
import com.bharatshop.shared.tenant.TenantContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);
    private final SubscriptionService subscriptionService;
    private final PlanService planService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponseDto>> getAllPlans() {
        try {
            List<Plan> planEntities = planService.getAllActivePlans();
        List<PlanResponseDto> plans = planEntities.stream()
                .map(planService::convertToResponseDto)
                .collect(Collectors.toList());
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Error fetching plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<PlanResponseDto> getPlanById(@PathVariable Long id) {
        try {
            Optional<Plan> planOpt = planService.getPlanById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PlanResponseDto plan = planService.convertToResponseDto(planOpt.get());
            return ResponseEntity.ok(plan);
        } catch (RuntimeException e) {
            log.error("Plan not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching plan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponseDto> getCurrentSubscription() {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            Optional<Subscription> subscriptionOpt = subscriptionService.getActiveSubscription(vendorId);
            if (subscriptionOpt.isPresent()) {
                SubscriptionResponseDto subscription = subscriptionService.convertToResponseDto(subscriptionOpt.get());
                return ResponseEntity.ok(subscription);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            log.error("No active subscription found for vendor: {}", TenantContext.getCurrentTenant(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching current subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<SubscriptionResponseDto>> getSubscriptionHistory(Pageable pageable) {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            Page<SubscriptionResponseDto> subscriptions = subscriptionService.getSubscriptionHistory(vendorId, pageable);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            log.error("Error fetching subscription history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderResponseDto> createSubscriptionOrder(
            @Valid @RequestBody SubscriptionRequestDto request) {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            RazorpayOrderResponseDto orderResponse = subscriptionService.createSubscriptionOrder(vendorId, request.getPlanId());
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            log.error("Error creating subscription order", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating subscription order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<SubscriptionResponseDto> verifyPayment(
            @Valid @RequestBody SubscriptionRequestDto request) {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            Subscription subscription = subscriptionService.verifyAndActivateSubscription(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );
            return ResponseEntity.ok(subscriptionService.convertToResponseDto(subscription));
        } catch (RuntimeException e) {
            log.error("Payment verification failed", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error verifying payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponseDto> cancelSubscription(
            @Valid @RequestBody SubscriptionRequestDto request) {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            // Get current subscription first
            Optional<Subscription> currentSubscriptionOpt = subscriptionService.getActiveSubscription(vendorId);
            if (currentSubscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            subscriptionService.cancelSubscription(currentSubscriptionOpt.get().getId(), request.getCancellationReason());
            // Return the cancelled subscription
            SubscriptionResponseDto subscription = subscriptionService.convertToResponseDto(currentSubscriptionOpt.get());
            return ResponseEntity.ok(subscription);
        } catch (RuntimeException e) {
            log.error("Error cancelling subscription", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error cancelling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            subscriptionService.handleWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }

    @GetMapping("/features")
    public ResponseEntity<Object> getCurrentFeatures() {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            Object features = subscriptionService.getCurrentFeatures(vendorId);
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            log.error("Error fetching current features", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/check-feature/{feature}")
    public ResponseEntity<Boolean> checkFeature(@PathVariable String feature) {
        try {
            Long vendorId = Long.parseLong(TenantContext.getCurrentTenant());
            boolean hasFeature = subscriptionService.hasFeature(vendorId, feature);
            return ResponseEntity.ok(hasFeature);
        } catch (Exception e) {
            log.error("Error checking feature: {}", feature, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.config.RazorpayConfig;
import com.bharatshop.shared.dto.RazorpayOrderResponseDto;
import com.bharatshop.shared.entity.Plan;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;

    public RazorpayOrderResponseDto createOrder(Plan plan, UUID subscriptionId, String customerEmail, String customerName, String customerPhone) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Convert price to paise (multiply by 100)
            int amountInPaise = plan.getPrice().multiply(BigDecimal.valueOf(100)).intValue();
            
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sub_" + subscriptionId.toString());
            
            JSONObject notes = new JSONObject();
            notes.put("subscription_id", subscriptionId.toString());
            notes.put("plan_id", plan.getId().toString());
            notes.put("plan_name", plan.getName());
            orderRequest.put("notes", notes);

            Order order = razorpayClient.orders.create(orderRequest);
            
            return RazorpayOrderResponseDto.builder()
                    .orderId(order.get("id"))
                    .currency("INR")
                    .amount(plan.getPrice())
                    .key(razorpayConfig.getKeyId())
                    .name("BharatShop Subscription")
                    .description("Subscription to " + plan.getName())
                    .image("https://bharatshop.com/logo.png")
                    .subscriptionId(subscriptionId)
                    .prefillName(customerName)
                    .prefillEmail(customerEmail)
                    .prefillContact(customerPhone)
                    .theme("#3399cc")
                    .callbackUrl("/api/subscriptions/verify")
                    .cancelUrl("/plans")
                    .planName(plan.getName())
                    .planDescription(plan.getDescription())
                    .planDurationDays(plan.getDurationDays())
                    .build();
                    
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order for plan: {}", plan.getId(), e);
            throw new RuntimeException("Failed to create payment order", e);
        }
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            
            return Utils.verifyPaymentSignature(attributes, razorpayConfig.getKeySecret());
        } catch (RazorpayException e) {
            log.error("Error verifying payment signature", e);
            return false;
        }
    }

    public Payment getPaymentDetails(String paymentId) {
        try {
            return razorpayClient.payments.fetch(paymentId);
        } catch (RazorpayException e) {
            log.error("Error fetching payment details for: {}", paymentId, e);
            throw new RuntimeException("Failed to fetch payment details", e);
        }
    }

    public Order getOrderDetails(String orderId) {
        try {
            return razorpayClient.orders.fetch(orderId);
        } catch (RazorpayException e) {
            log.error("Error fetching order details for: {}", orderId, e);
            throw new RuntimeException("Failed to fetch order details", e);
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String expectedSignature = generateWebhookSignature(payload);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private String generateWebhookSignature(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                razorpayConfig.getKeySecret().getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    public JSONObject parseWebhookPayload(String payload) {
        try {
            return new JSONObject(payload);
        } catch (Exception e) {
            log.error("Error parsing webhook payload", e);
            throw new RuntimeException("Invalid webhook payload", e);
        }
    }
}
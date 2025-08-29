package com.bharatshop.shared.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key.id:rzp_test_key}")
    private String keyId;

    @Value("${razorpay.key.secret:rzp_test_secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            log.info("Initializing Razorpay client with key: {}", keyId);
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client", e);
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }
}
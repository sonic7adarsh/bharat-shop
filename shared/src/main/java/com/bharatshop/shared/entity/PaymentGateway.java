package com.bharatshop.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_gateways")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGateway {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false)
    private GatewayType gatewayType;
    
    @Column(name = "gateway_name", nullable = false)
    private String gatewayName;
    
    @Column(name = "key_id", nullable = false)
    private String keyId;
    
    @Column(name = "key_secret", nullable = false)
    private String keySecret;
    
    @Column(name = "webhook_secret")
    private String webhookSecret;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_test_mode", nullable = false)
    private Boolean isTestMode = true;
    
    @Column(name = "merchant_id")
    private String merchantId;
    
    @Column(name = "callback_url")
    private String callbackUrl;
    
    @Column(name = "webhook_url")
    private String webhookUrl;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum GatewayType {
        RAZORPAY,
        PAYU,
        STRIPE,
        PAYPAL
    }
}
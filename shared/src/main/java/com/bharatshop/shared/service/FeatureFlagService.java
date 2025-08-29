package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Subscription;
import com.bharatshop.shared.enums.SubscriptionStatus;
import com.bharatshop.shared.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {
    
    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final SubscriptionRepository subscriptionRepository;

    // Default limits for free/trial users
    private static final int DEFAULT_MAX_PRODUCTS = 10;
    private static final long DEFAULT_STORAGE_LIMIT = 100 * 1024 * 1024; // 100MB
    private static final int DEFAULT_MAX_ORDERS = 50;
    private static final int DEFAULT_MAX_CATEGORIES = 5;
    private static final boolean DEFAULT_ANALYTICS = false;
    private static final boolean DEFAULT_CUSTOM_DOMAIN = false;
    private static final boolean DEFAULT_ADVANCED_FEATURES = false;
    private static final boolean DEFAULT_EMAIL_SUPPORT = false;
    private static final boolean DEFAULT_PRIORITY_SUPPORT = false;

    public boolean hasFeature(UUID vendorId, String featureName) {
        try {
            Optional<Subscription> activeSubscription = getActiveSubscription(vendorId);
            
            if (activeSubscription.isEmpty()) {
                return getDefaultFeatureValue(featureName);
            }

            JsonNode features = activeSubscription.get().getPlan().getFeatures();
            if (features == null || !features.has(featureName)) {
                return getDefaultFeatureValue(featureName);
            }

            JsonNode featureNode = features.get(featureName);
            if (featureNode.isBoolean()) {
                return featureNode.asBoolean();
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error checking feature {} for vendor {}", featureName, vendorId, e);
            return getDefaultFeatureValue(featureName);
        }
    }

    public int getFeatureLimit(UUID vendorId, String featureName) {
        try {
            Optional<Subscription> activeSubscription = getActiveSubscription(vendorId);
            
            if (activeSubscription.isEmpty()) {
                return getDefaultFeatureLimit(featureName);
            }

            JsonNode features = activeSubscription.get().getPlan().getFeatures();
            if (features == null || !features.has(featureName)) {
                return getDefaultFeatureLimit(featureName);
            }

            JsonNode featureNode = features.get(featureName);
            if (featureNode.isInt()) {
                return featureNode.asInt();
            }
            
            return getDefaultFeatureLimit(featureName);
        } catch (Exception e) {
            log.error("Error getting feature limit {} for vendor {}", featureName, vendorId, e);
            return getDefaultFeatureLimit(featureName);
        }
    }

    public long getFeatureLimitLong(UUID vendorId, String featureName) {
        try {
            Optional<Subscription> activeSubscription = getActiveSubscription(vendorId);
            
            if (activeSubscription.isEmpty()) {
                return getDefaultFeatureLimitLong(featureName);
            }

            JsonNode features = activeSubscription.get().getPlan().getFeatures();
            if (features == null || !features.has(featureName)) {
                return getDefaultFeatureLimitLong(featureName);
            }

            JsonNode featureNode = features.get(featureName);
            if (featureNode.isLong() || featureNode.isInt()) {
                return featureNode.asLong();
            }
            
            return getDefaultFeatureLimitLong(featureName);
        } catch (Exception e) {
            log.error("Error getting feature limit {} for vendor {}", featureName, vendorId, e);
            return getDefaultFeatureLimitLong(featureName);
        }
    }

    public void enforceProductLimit(UUID vendorId, int currentProductCount) {
        int maxProducts = getFeatureLimit(vendorId, "maxProducts");
        if (currentProductCount >= maxProducts) {
            throw new RuntimeException("Product limit exceeded. Current plan allows maximum " + maxProducts + " products.");
        }
    }

    public void enforceStorageLimit(UUID vendorId, long currentStorageUsed, long additionalStorage) {
        long storageLimit = getFeatureLimitLong(vendorId, "storageLimit");
        if (currentStorageUsed + additionalStorage > storageLimit) {
            long limitInMB = storageLimit / (1024 * 1024);
            throw new RuntimeException("Storage limit exceeded. Current plan allows maximum " + limitInMB + "MB storage.");
        }
    }

    public void enforceOrderLimit(UUID vendorId, int currentOrderCount) {
        int maxOrders = getFeatureLimit(vendorId, "maxOrders");
        if (currentOrderCount >= maxOrders) {
            throw new RuntimeException("Order limit exceeded. Current plan allows maximum " + maxOrders + " orders per month.");
        }
    }

    public void enforceCategoryLimit(UUID vendorId, int currentCategoryCount) {
        int maxCategories = getFeatureLimit(vendorId, "maxCategories");
        if (currentCategoryCount >= maxCategories) {
            throw new RuntimeException("Category limit exceeded. Current plan allows maximum " + maxCategories + " categories.");
        }
    }

    public void enforceFeatureAccess(UUID vendorId, String featureName) {
        if (!hasFeature(vendorId, featureName)) {
            throw new RuntimeException("Feature '" + featureName + "' is not available in your current plan. Please upgrade to access this feature.");
        }
    }

    private Optional<Subscription> getActiveSubscription(UUID vendorId) {
        return subscriptionRepository.findActiveByVendorId(vendorId, LocalDateTime.now())
                .filter(subscription -> 
                    subscription.getStatus() == SubscriptionStatus.ACTIVE &&
                    subscription.getEndDate().isAfter(LocalDateTime.now())
                );
    }

    private boolean getDefaultFeatureValue(String featureName) {
        switch (featureName) {
            case "analytics":
                return DEFAULT_ANALYTICS;
            case "customDomain":
                return DEFAULT_CUSTOM_DOMAIN;
            case "advancedFeatures":
                return DEFAULT_ADVANCED_FEATURES;
            case "emailSupport":
                return DEFAULT_EMAIL_SUPPORT;
            case "prioritySupport":
                return DEFAULT_PRIORITY_SUPPORT;
            default:
                return false;
        }
    }

    private int getDefaultFeatureLimit(String featureName) {
        switch (featureName) {
            case "maxProducts":
                return DEFAULT_MAX_PRODUCTS;
            case "maxOrders":
                return DEFAULT_MAX_ORDERS;
            case "maxCategories":
                return DEFAULT_MAX_CATEGORIES;
            default:
                return 0;
        }
    }

    private long getDefaultFeatureLimitLong(String featureName) {
        switch (featureName) {
            case "storageLimit":
                return DEFAULT_STORAGE_LIMIT;
            default:
                return 0L;
        }
    }

    public JsonNode getCurrentPlanFeatures(UUID vendorId) {
        Optional<Subscription> activeSubscription = getActiveSubscription(vendorId);
        return activeSubscription.map(subscription -> subscription.getPlan().getFeatures())
                .orElse(null);
    }

    public boolean isSubscriptionActive(UUID vendorId) {
        return getActiveSubscription(vendorId).isPresent();
    }

    public String getCurrentPlanName(UUID vendorId) {
        return getActiveSubscription(vendorId)
                .map(subscription -> subscription.getPlan().getName())
                .orElse("Free Plan");
    }
}
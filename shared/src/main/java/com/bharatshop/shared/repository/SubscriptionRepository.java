package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Subscription;
import com.bharatshop.shared.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Find active subscription by vendor ID
    @Query(value = "SELECT * FROM subscriptions s WHERE s.vendor_id = :vendorId AND s.status = 'ACTIVE' AND s.end_date > :currentDate AND s.deleted_at IS NULL", nativeQuery = true)
    Optional<Subscription> findActiveByVendorId(@Param("vendorId") Long vendorId, @Param("currentDate") LocalDateTime currentDate);

    // Find current subscription by vendor ID
    @Query(value = "SELECT * FROM subscriptions s WHERE s.vendor_id = :vendorId AND s.deleted_at IS NULL ORDER BY s.end_date DESC LIMIT 1", nativeQuery = true)
    Optional<Subscription> findCurrentByVendorId(@Param("vendorId") Long vendorId);

    // Find all subscriptions by vendor ID
    List<Subscription> findByVendorIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long vendorId);

    // Find subscriptions by status
    List<Subscription> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(SubscriptionStatus status);

    // Find subscriptions by plan ID
    List<Subscription> findByPlanIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long planId);

    // Find expiring subscriptions
    @Query(value = "SELECT * FROM subscriptions s WHERE s.status = 'ACTIVE' AND s.end_date BETWEEN :startDate AND :endDate AND s.deleted_at IS NULL ORDER BY s.end_date ASC", nativeQuery = true)
    List<Subscription> findExpiringBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find expired subscriptions that need cleanup
    @Query(value = "SELECT * FROM subscriptions s WHERE s.status = 'ACTIVE' AND s.end_date < :currentDate AND s.deleted_at IS NULL", nativeQuery = true)
    List<Subscription> findExpiredActive(@Param("currentDate") LocalDateTime currentDate);

    // Find subscriptions for renewal
    @Query(value = "SELECT * FROM subscriptions s WHERE s.auto_renew = true AND s.status = 'ACTIVE' AND s.next_billing_date <= :currentDate AND s.deleted_at IS NULL", nativeQuery = true)
    List<Subscription> findForRenewal(@Param("currentDate") LocalDateTime currentDate);

    // Find subscription by Razorpay order ID
    Optional<Subscription> findByRazorpayOrderIdAndDeletedAtIsNull(String orderId);

    // Find subscription by Razorpay subscription ID
    Optional<Subscription> findByRazorpaySubscriptionIdAndDeletedAtIsNull(String subscriptionId);

    // Find subscription by Razorpay payment ID
    Optional<Subscription> findByRazorpayPaymentIdAndDeletedAtIsNull(String paymentId);

    // Count active subscriptions
    @Query(value = "SELECT COUNT(*) FROM subscriptions s WHERE s.status = 'ACTIVE' AND s.end_date > :currentDate AND s.deleted_at IS NULL", nativeQuery = true)
    long countActive(@Param("currentDate") LocalDateTime currentDate);

    // Count subscriptions by plan
    @Query(value = "SELECT COUNT(*) FROM subscriptions s WHERE s.plan_id = :planId AND s.status = 'ACTIVE' AND s.end_date > :currentDate AND s.deleted_at IS NULL", nativeQuery = true)
    long countActiveByPlan(@Param("planId") Long planId, @Param("currentDate") LocalDateTime currentDate);

    // Find subscriptions by vendor and status
    List<Subscription> findByVendorIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(Long vendorId, SubscriptionStatus status);

    // Find trial subscriptions
    @Query(value = "SELECT * FROM subscriptions s WHERE s.status = 'TRIAL' AND s.deleted_at IS NULL ORDER BY s.end_date ASC", nativeQuery = true)
    List<Subscription> findTrialSubscriptions();

    // Find subscriptions with payment failures
    @Query(value = "SELECT * FROM subscriptions s WHERE s.status = 'PAYMENT_FAILED' AND s.deleted_at IS NULL ORDER BY s.updated_at DESC", nativeQuery = true)
    List<Subscription> findPaymentFailedSubscriptions();

    // Check if vendor has any active subscription
    @Query(value = "SELECT COUNT(*) > 0 FROM subscriptions s WHERE s.vendor_id = :vendorId AND s.status = 'ACTIVE' AND s.end_date > :currentDate AND s.deleted_at IS NULL", nativeQuery = true)
    boolean hasActiveSubscription(@Param("vendorId") Long vendorId, @Param("currentDate") LocalDateTime currentDate);

    // Find subscriptions ending in next N days
    @Query(value = "SELECT * FROM subscriptions s WHERE s.status = 'ACTIVE' AND s.end_date BETWEEN :currentDate AND :futureDate AND s.deleted_at IS NULL ORDER BY s.end_date ASC", nativeQuery = true)
    List<Subscription> findEndingInDays(@Param("currentDate") LocalDateTime currentDate, @Param("futureDate") LocalDateTime futureDate);
    
    // Alias methods for service compatibility
    default Optional<Subscription> findByRazorpayOrderId(String orderId) {
        return findByRazorpayOrderIdAndDeletedAtIsNull(orderId);
    }
    
    default List<Subscription> findAllByVendorId(Long vendorId) {
        return findByVendorIdAndDeletedAtIsNullOrderByCreatedAtDesc(vendorId);
    }
}
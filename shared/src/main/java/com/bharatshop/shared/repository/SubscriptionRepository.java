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
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // Find active subscription by vendor ID
    @Query("SELECT s FROM Subscription s WHERE s.vendorId = :vendorId AND s.status = 'ACTIVE' AND s.endDate > :currentDate AND s.deletedAt IS NULL")
    Optional<Subscription> findActiveByVendorId(@Param("vendorId") UUID vendorId, @Param("currentDate") LocalDateTime currentDate);

    // Find current subscription by vendor ID (active or expired)
    @Query("SELECT s FROM Subscription s WHERE s.vendorId = :vendorId AND s.deletedAt IS NULL ORDER BY s.endDate DESC LIMIT 1")
    Optional<Subscription> findCurrentByVendorId(@Param("vendorId") UUID vendorId);

    // Find all subscriptions by vendor ID
    @Query("SELECT s FROM Subscription s WHERE s.vendorId = :vendorId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Subscription> findAllByVendorId(@Param("vendorId") UUID vendorId);

    // Find subscriptions by status
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Subscription> findByStatus(@Param("status") SubscriptionStatus status);

    // Find subscriptions by plan ID
    @Query("SELECT s FROM Subscription s WHERE s.planId = :planId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Subscription> findByPlanId(@Param("planId") UUID planId);

    // Find expiring subscriptions
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :startDate AND :endDate AND s.deletedAt IS NULL ORDER BY s.endDate ASC")
    List<Subscription> findExpiringBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find expired subscriptions that need cleanup
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :currentDate AND s.deletedAt IS NULL")
    List<Subscription> findExpiredActive(@Param("currentDate") LocalDateTime currentDate);

    // Find subscriptions for renewal
    @Query("SELECT s FROM Subscription s WHERE s.autoRenew = true AND s.status = 'ACTIVE' AND s.nextBillingDate <= :currentDate AND s.deletedAt IS NULL")
    List<Subscription> findForRenewal(@Param("currentDate") LocalDateTime currentDate);

    // Find subscription by Razorpay order ID
    @Query("SELECT s FROM Subscription s WHERE s.razorpayOrderId = :orderId AND s.deletedAt IS NULL")
    Optional<Subscription> findByRazorpayOrderId(@Param("orderId") String orderId);

    // Find subscription by Razorpay subscription ID
    @Query("SELECT s FROM Subscription s WHERE s.razorpaySubscriptionId = :subscriptionId AND s.deletedAt IS NULL")
    Optional<Subscription> findByRazorpaySubscriptionId(@Param("subscriptionId") String subscriptionId);

    // Find subscription by Razorpay payment ID
    @Query("SELECT s FROM Subscription s WHERE s.razorpayPaymentId = :paymentId AND s.deletedAt IS NULL")
    Optional<Subscription> findByRazorpayPaymentId(@Param("paymentId") String paymentId);

    // Count active subscriptions
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate > :currentDate AND s.deletedAt IS NULL")
    long countActive(@Param("currentDate") LocalDateTime currentDate);

    // Count subscriptions by plan
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.planId = :planId AND s.status = 'ACTIVE' AND s.endDate > :currentDate AND s.deletedAt IS NULL")
    long countActiveByPlan(@Param("planId") UUID planId, @Param("currentDate") LocalDateTime currentDate);

    // Find subscriptions by vendor and status
    @Query("SELECT s FROM Subscription s WHERE s.vendorId = :vendorId AND s.status = :status AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Subscription> findByVendorIdAndStatus(@Param("vendorId") UUID vendorId, @Param("status") SubscriptionStatus status);

    // Find trial subscriptions
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.deletedAt IS NULL ORDER BY s.endDate ASC")
    List<Subscription> findTrialSubscriptions();

    // Find subscriptions with payment failures
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAYMENT_FAILED' AND s.deletedAt IS NULL ORDER BY s.updatedAt DESC")
    List<Subscription> findPaymentFailedSubscriptions();

    // Check if vendor has any active subscription
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.vendorId = :vendorId AND s.status = 'ACTIVE' AND s.endDate > :currentDate AND s.deletedAt IS NULL")
    boolean hasActiveSubscription(@Param("vendorId") UUID vendorId, @Param("currentDate") LocalDateTime currentDate);

    // Find subscriptions ending in next N days
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :currentDate AND :futureDate AND s.deletedAt IS NULL ORDER BY s.endDate ASC")
    List<Subscription> findEndingInDays(@Param("currentDate") LocalDateTime currentDate, @Param("futureDate") LocalDateTime futureDate);
}
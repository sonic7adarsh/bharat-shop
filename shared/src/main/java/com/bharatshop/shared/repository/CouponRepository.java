package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Coupon entity operations.
 * Provides custom queries for coupon validation, usage tracking, and atomic operations.
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Find a coupon by code and tenant ID
     */
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Optional<Coupon> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    /**
     * Find active coupons by tenant ID
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.isActive = true AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Coupon> findActiveCouponsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Find valid coupons (active and within date range) by tenant ID
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.isActive = true " +
           "AND c.deletedAt IS NULL " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) " +
           "AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "ORDER BY c.createdAt DESC")
    List<Coupon> findValidCouponsByTenantId(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    /**
     * Find a valid coupon by code and tenant ID (active and within date range)
     */
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.tenantId = :tenantId " +
           "AND c.isActive = true AND c.deletedAt IS NULL " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) " +
           "AND (c.endDate IS NULL OR c.endDate >= :now)")
    Optional<Coupon> findValidCouponByCodeAndTenantId(@Param("code") String code, 
                                                      @Param("tenantId") Long tenantId, 
                                                      @Param("now") LocalDateTime now);

    /**
     * Check if a coupon code exists for a tenant (excluding soft-deleted)
     */
    @Query("SELECT COUNT(c) > 0 FROM Coupon c WHERE c.code = :code AND c.tenantId = :tenantId AND c.deletedAt IS NULL")
    boolean existsByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    /**
     * Atomically increment usage count for a coupon with full validation
     * Returns the number of rows affected (should be 1 for success, 0 for failure)
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount + 1 " +
           "WHERE c.id = :couponId AND c.tenantId = :tenantId " +
           "AND c.isActive = true " +
           "AND c.startDate <= :now " +
           "AND c.endDate > :now " +
           "AND (c.usageLimit IS NULL OR c.usageCount < c.usageLimit)")
    int incrementUsageCountAtomically(@Param("couponId") Long couponId, 
                                      @Param("tenantId") Long tenantId, 
                                      @Param("now") LocalDateTime now);

    /**
     * Atomically decrement usage count for a coupon (for rollback scenarios)
     * Returns the number of rows affected (should be 1 for success, 0 for failure)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.usageCount = CASE WHEN c.usageCount > 0 THEN c.usageCount - 1 ELSE 0 END, " +
           "c.updatedAt = :now WHERE c.id = :couponId AND c.tenantId = :tenantId")
    int decrementUsageCountAtomically(@Param("couponId") Long couponId, 
                                      @Param("tenantId") Long tenantId, 
                                      @Param("now") LocalDateTime now);
    
    /**
     * Simple increment usage count for a coupon (non-atomic, for backward compatibility)
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount + 1 WHERE c.id = :couponId AND c.tenantId = :tenantId")
    int incrementUsageCount(@Param("couponId") Long couponId, @Param("tenantId") Long tenantId);

    /**
     * Simple decrement usage count for a coupon (non-atomic, for backward compatibility)
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount - 1 WHERE c.id = :couponId AND c.tenantId = :tenantId AND c.usageCount > 0")
    int decrementUsageCount(@Param("couponId") Long couponId, @Param("tenantId") Long tenantId);
    
    /**
     * Get current usage count for a coupon
     */
    @Query("SELECT c.usageCount FROM Coupon c WHERE c.id = :couponId AND c.tenantId = :tenantId")
    Integer getCurrentUsageCount(@Param("couponId") Long couponId, @Param("tenantId") Long tenantId);

    /**
     * Find coupons that are expiring soon (within specified days)
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.isActive = true " +
           "AND c.deletedAt IS NULL AND c.endDate BETWEEN :now AND :expiryThreshold " +
           "ORDER BY c.endDate ASC")
    List<Coupon> findCouponsExpiringSoon(@Param("tenantId") Long tenantId, 
                                         @Param("now") LocalDateTime now, 
                                         @Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * Find coupons by usage statistics (for analytics)
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL " +
           "AND c.usageCount >= :minUsage ORDER BY c.usageCount DESC")
    List<Coupon> findCouponsByMinUsage(@Param("tenantId") Long tenantId, @Param("minUsage") Integer minUsage);

    /**
     * Find first-order-only coupons
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.firstOrderOnly = true " +
           "AND c.isActive = true AND c.deletedAt IS NULL " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) " +
           "AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "ORDER BY c.createdAt DESC")
    List<Coupon> findFirstOrderOnlyCoupons(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    /**
     * Find coupons applicable to specific categories
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.isActive = true " +
           "AND c.deletedAt IS NULL " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) " +
           "AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "AND (c.eligibleCategories IS NULL OR c.eligibleCategories = '' " +
           "OR c.eligibleCategories LIKE CONCAT('%', :categoryId, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<Coupon> findCouponsForCategory(@Param("tenantId") Long tenantId, 
                                        @Param("categoryId") String categoryId, 
                                        @Param("now") LocalDateTime now);

    /**
     * Find coupons applicable to specific products
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.isActive = true " +
           "AND c.deletedAt IS NULL " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) " +
           "AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "AND (c.eligibleProducts IS NULL OR c.eligibleProducts = '' " +
           "OR c.eligibleProducts LIKE CONCAT('%', :productId, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<Coupon> findCouponsForProduct(@Param("tenantId") Long tenantId, 
                                       @Param("productId") String productId, 
                                       @Param("now") LocalDateTime now);

    /**
     * Count total active coupons for a tenant
     */
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.tenantId = :tenantId AND c.isActive = true AND c.deletedAt IS NULL")
    long countActiveCouponsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Find coupons with usage limit reached
     */
    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL " +
           "AND c.usageLimit IS NOT NULL AND c.usageCount >= c.usageLimit " +
           "ORDER BY c.updatedAt DESC")
    List<Coupon> findCouponsWithUsageLimitReached(@Param("tenantId") Long tenantId);

    /**
     * Count customer usage of a specific coupon
     * This would typically count from order/discount_line tables
     * For now, returning 0 as placeholder until order integration is complete
     */
    @Query("SELECT COUNT(o) FROM Orders o JOIN o.discountLines dl " +
           "WHERE dl.couponId = :couponId AND o.customerId = :customerId")
    int countCustomerUsage(@Param("couponId") Long couponId, @Param("customerId") Long customerId);

    /**
     * Soft delete expired coupons (batch operation)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.deletedAt = :now, c.updatedAt = :now " +
           "WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL " +
           "AND c.endDate < :now")
    int softDeleteExpiredCoupons(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
}
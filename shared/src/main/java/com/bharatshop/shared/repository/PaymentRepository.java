package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    @Query(value = "SELECT * FROM payments WHERE tenant_id = :tenantId ORDER BY created_at DESC", nativeQuery = true)
    List<Payment> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") Long tenantId);
    
    @Query(value = "SELECT * FROM payments WHERE tenant_id = :tenantId AND order_id = :orderId", nativeQuery = true)
    List<Payment> findByTenantIdAndOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);
    
    @Query(value = "SELECT * FROM payments WHERE tenant_id = :tenantId AND order_id = :orderId AND status = :status", nativeQuery = true)
    List<Payment> findByTenantIdAndOrderIdAndStatus(
        @Param("tenantId") Long tenantId, 
        @Param("orderId") Long orderId, 
        @Param("status") Payment.PaymentStatus status
    );
    
    @Query(value = "SELECT * FROM payments WHERE razorpay_order_id = :razorpayOrderId", nativeQuery = true)
    Optional<Payment> findByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId);
    
    @Query(value = "SELECT * FROM payments WHERE razorpay_payment_id = :razorpayPaymentId", nativeQuery = true)
    Optional<Payment> findByRazorpayPaymentId(@Param("razorpayPaymentId") String razorpayPaymentId);
    
    @Query(value = "SELECT * FROM payments WHERE tenant_id = :tenantId AND status = :status", nativeQuery = true)
    List<Payment> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Payment.PaymentStatus status);
    
    @Query(value = "SELECT * FROM payments WHERE tenant_id = :tenantId AND order_id = :orderId ORDER BY created_at DESC", nativeQuery = true)
    List<Payment> findByTenantIdAndOrderIdOrderByCreatedAtDesc(
        @Param("tenantId") Long tenantId, 
        @Param("orderId") Long orderId
    );
}
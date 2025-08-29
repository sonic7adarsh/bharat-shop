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
    
    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId")
    List<Payment> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") Long tenantId);
    
    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.orderId = :orderId")
    List<Payment> findByTenantIdAndOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);
    
    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.orderId = :orderId AND p.status = :status")
    List<Payment> findByTenantIdAndOrderIdAndStatus(
        @Param("tenantId") Long tenantId, 
        @Param("orderId") Long orderId, 
        @Param("status") Payment.PaymentStatus status
    );
    
    @Query("SELECT p FROM Payment p WHERE p.razorpayOrderId = :razorpayOrderId")
    Optional<Payment> findByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId);
    
    @Query("SELECT p FROM Payment p WHERE p.razorpayPaymentId = :razorpayPaymentId")
    Optional<Payment> findByRazorpayPaymentId(@Param("razorpayPaymentId") String razorpayPaymentId);
    
    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.status = :status")
    List<Payment> findByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.orderId = :orderId ORDER BY p.createdAt DESC")
    List<Payment> findByTenantIdAndOrderIdOrderByCreatedAtDesc(
        @Param("tenantId") Long tenantId, 
        @Param("orderId") Long orderId
    );
}
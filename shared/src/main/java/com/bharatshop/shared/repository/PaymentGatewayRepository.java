package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGateway, UUID> {
    
    @Query("SELECT pg FROM PaymentGateway pg WHERE pg.tenantId = :tenantId")
    List<PaymentGateway> findByTenantId(@Param("tenantId") Long tenantId);
    
    @Query("SELECT pg FROM PaymentGateway pg WHERE pg.tenantId = :tenantId AND pg.isActive = true")
    List<PaymentGateway> findActiveByTenantId(@Param("tenantId") Long tenantId);
    
    @Query("SELECT pg FROM PaymentGateway pg WHERE pg.tenantId = :tenantId AND pg.gatewayType = :gatewayType AND pg.isActive = true")
    Optional<PaymentGateway> findActiveByTenantIdAndGatewayType(
        @Param("tenantId") Long tenantId, 
        @Param("gatewayType") PaymentGateway.GatewayType gatewayType
    );
    
    @Query("SELECT pg FROM PaymentGateway pg WHERE pg.tenantId = :tenantId AND pg.gatewayType = :gatewayType")
    List<PaymentGateway> findByTenantIdAndGatewayType(
        @Param("tenantId") Long tenantId, 
        @Param("gatewayType") PaymentGateway.GatewayType gatewayType
    );
}
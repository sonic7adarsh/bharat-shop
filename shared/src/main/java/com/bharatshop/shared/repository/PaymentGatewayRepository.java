package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGateway, Long> {
    
    List<PaymentGateway> findByTenantId(String tenantId);
    
    List<PaymentGateway> findByTenantIdAndIsActiveTrue(String tenantId);
    
    Optional<PaymentGateway> findByTenantIdAndGatewayTypeAndIsActiveTrue(
        String tenantId, 
        PaymentGateway.GatewayType gatewayType
    );
    
    List<PaymentGateway> findByTenantIdAndGatewayType(
        String tenantId, 
        PaymentGateway.GatewayType gatewayType
    );
    
    /**
     * Find active payment gateway by tenant ID and gateway type (alias method)
     */
    default Optional<PaymentGateway> findActiveByTenantIdAndGatewayType(
        Long tenantId, 
        PaymentGateway.GatewayType gatewayType
    ) {
        return findByTenantIdAndGatewayTypeAndIsActiveTrue(tenantId.toString(), gatewayType);
    }
}
package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    
    /**
     * Find all addresses for a customer in a specific tenant
     */
    @Query(value = "SELECT * FROM customer_addresses ca WHERE ca.customer_id = :customerId AND ca.tenant_id = :tenantId AND ca.is_active = true ORDER BY ca.is_default DESC, ca.created_at DESC", nativeQuery = true)
    List<CustomerAddress> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Find active addresses for a customer in a specific tenant
     */
    @Query(value = "SELECT * FROM customer_addresses ca WHERE ca.customer_id = :customerId AND ca.tenant_id = :tenantId AND ca.is_active = true ORDER BY ca.is_default DESC, ca.created_at DESC", nativeQuery = true)
    List<CustomerAddress> findActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Find default address for a customer in a specific tenant
     */
    Optional<CustomerAddress> findByCustomerIdAndTenantIdAndIsDefaultTrueAndIsActiveTrue(Long customerId, Long tenantId);
    
    /**
     * Find address by ID, customer ID and tenant ID (for security)
     */
    Optional<CustomerAddress> findByIdAndCustomerIdAndTenantId(Long id, Long customerId, Long tenantId);
    
    /**
     * Count active addresses for a customer
     */
    long countByCustomerIdAndTenantIdAndIsActiveTrue(Long customerId, Long tenantId);
    
    /**
     * Set all addresses as non-default for a customer (used before setting a new default)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE customer_addresses SET is_default = false WHERE customer_id = :customerId AND tenant_id = :tenantId", nativeQuery = true)
    void clearDefaultForCustomer(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Soft delete an address (set isActive to false)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE customer_addresses SET is_active = false WHERE id = :id AND customer_id = :customerId AND tenant_id = :tenantId", nativeQuery = true)
    int softDeleteByIdAndCustomerIdAndTenantId(@Param("id") Long id, @Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Check if address exists for customer (active addresses only)
     */
    boolean existsByIdAndCustomerIdAndTenantIdAndIsActiveTrue(Long id, Long customerId, Long tenantId);
    
    /**
     * Count active addresses for a customer and tenant
     */
    @Query(value = "SELECT COUNT(*) FROM customer_addresses WHERE customer_id = ?1 AND tenant_id = ?2 AND is_active = true", nativeQuery = true)
    long countActiveByCustomerIdAndTenantId(Long customerId, Long tenantId);
    
    /**
     * Check if address exists by ID, customer ID and tenant ID
     */
    boolean existsByIdAndCustomerIdAndTenantId(Long id, Long customerId, Long tenantId);
    
    /**
     * Find default address by customer ID and tenant ID
     */
    Optional<CustomerAddress> findDefaultByCustomerIdAndTenantId(Long customerId, Long tenantId);
}
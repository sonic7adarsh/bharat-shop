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
    @Query("SELECT ca FROM CustomerAddress ca WHERE ca.customerId = :customerId AND ca.tenantId = :tenantId AND ca.isActive = true ORDER BY ca.isDefault DESC, ca.createdAt DESC")
    List<CustomerAddress> findByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Find active addresses for a customer in a specific tenant
     */
    @Query("SELECT ca FROM CustomerAddress ca WHERE ca.customerId = :customerId AND ca.tenantId = :tenantId AND ca.isActive = true ORDER BY ca.isDefault DESC, ca.createdAt DESC")
    List<CustomerAddress> findActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Find default address for a customer in a specific tenant
     */
    @Query("SELECT ca FROM CustomerAddress ca WHERE ca.customerId = :customerId AND ca.tenantId = :tenantId AND ca.isDefault = true AND ca.isActive = true")
    Optional<CustomerAddress> findDefaultByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Find address by ID, customer ID and tenant ID (for security)
     */
    @Query("SELECT ca FROM CustomerAddress ca WHERE ca.id = :id AND ca.customerId = :customerId AND ca.tenantId = :tenantId")
    Optional<CustomerAddress> findByIdAndCustomerIdAndTenantId(@Param("id") Long id, @Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Count active addresses for a customer
     */
    @Query("SELECT COUNT(ca) FROM CustomerAddress ca WHERE ca.customerId = :customerId AND ca.tenantId = :tenantId AND ca.isActive = true")
    long countActiveByCustomerIdAndTenantId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Set all addresses as non-default for a customer (used before setting a new default)
     */
    @Modifying
    @Transactional
    @Query("UPDATE CustomerAddress ca SET ca.isDefault = false WHERE ca.customerId = :customerId AND ca.tenantId = :tenantId")
    void clearDefaultForCustomer(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Soft delete an address (set isActive to false)
     */
    @Modifying
    @Transactional
    @Query("UPDATE CustomerAddress ca SET ca.isActive = false WHERE ca.id = :id AND ca.customerId = :customerId AND ca.tenantId = :tenantId")
    int softDeleteByIdAndCustomerIdAndTenantId(@Param("id") Long id, @Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
    
    /**
     * Check if address exists for customer
     */
    @Query("SELECT COUNT(ca) > 0 FROM CustomerAddress ca WHERE ca.id = :id AND ca.customerId = :customerId AND ca.tenantId = :tenantId AND ca.isActive = true")
    boolean existsByIdAndCustomerIdAndTenantId(@Param("id") Long id, @Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
}
package com.bharatshop.storefront.service;

import com.bharatshop.shared.entity.CustomerAddress;
import com.bharatshop.shared.repository.CustomerAddressRepository;
import com.bharatshop.storefront.dto.AddressRequest;
import com.bharatshop.storefront.dto.AddressResponse;
import com.bharatshop.shared.entity.User;
import com.bharatshop.storefront.repository.StorefrontUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AddressService {
    
    private static final Logger log = LoggerFactory.getLogger(AddressService.class);
    
    private final CustomerAddressRepository addressRepository;
    private final StorefrontUserRepository userRepository;
    private static final int MAX_ADDRESSES_PER_CUSTOMER = 10;
    
    /**
     * Get customer ID and tenant ID from email
     */
    private CustomerInfo getCustomerInfo(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found with email: " + email));
        
        // Convert UUID to Long using hashCode for compatibility with CustomerAddress entity
        Long customerId = Math.abs((long) user.getId().hashCode());
        Long tenantId = user.getTenantId() != null ? Math.abs((long) user.getTenantId().hashCode()) : 1L;
        
        return new CustomerInfo(customerId, tenantId);
    }
    
    /**
     * Get all addresses for a customer
     */
    public List<AddressResponse> getCustomerAddresses(String email) {
        log.debug("Getting addresses for customer email: {}", email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        List<CustomerAddress> addresses = addressRepository.findActiveByCustomerIdAndTenantId(
                customerInfo.customerId(), customerInfo.tenantId());
        return addresses.stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get address by ID for a customer
     */
    public Optional<AddressResponse> getAddressById(Long addressId, String email) {
        log.debug("Getting address: {} for customer email: {}", addressId, email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        return addressRepository.findByIdAndCustomerIdAndTenantId(
                addressId, customerInfo.customerId(), customerInfo.tenantId())
                .map(AddressResponse::fromEntity);
    }
    
    /**
     * Get default address for a customer
     */
    public Optional<AddressResponse> getDefaultAddress(String email) {
        log.debug("Getting default address for customer email: {}", email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        return addressRepository.findDefaultByCustomerIdAndTenantId(
                customerInfo.customerId(), customerInfo.tenantId())
                .map(AddressResponse::fromEntity);
    }
    
    /**
     * Create a new address for a customer
     */
    @Transactional
    public AddressResponse createAddress(AddressRequest request, String email) {
        log.debug("Creating address for customer email: {}", email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        Long customerId = customerInfo.customerId();
        Long tenantId = customerInfo.tenantId();
        
        // Check if customer has reached the maximum number of addresses
        long addressCount = addressRepository.countActiveByCustomerIdAndTenantId(customerId, tenantId);
        if (addressCount >= MAX_ADDRESSES_PER_CUSTOMER) {
            throw new IllegalStateException("Maximum number of addresses (" + MAX_ADDRESSES_PER_CUSTOMER + ") reached");
        }
        
        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(customerId);
        address.setTenantId(tenantId);
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());
        address.setIsDefault(request.getIsDefault());
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        address.setIsActive(true);
        
        // If this is the first address or marked as default, set it as default
        if (addressCount == 0 || request.getIsDefault()) {
            // Unset other default addresses first
            if (request.getIsDefault()) {
                addressRepository.clearDefaultForCustomer(customerId, tenantId);
            }
            address.setIsDefault(true);
        }
        
        CustomerAddress savedAddress = addressRepository.save(address);
        log.info("Created address with ID: {} for customer: {}", savedAddress.getId(), customerId);
        
        return AddressResponse.fromEntity(savedAddress);
    }
    
    /**
     * Get customer ID by email (for controller use)
     */
    public Long getCustomerIdByEmail(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found with email: " + email));
        
        // Convert UUID to Long using hashCode for compatibility
        return Math.abs((long) user.getId().hashCode());
    }
    
    /**
     * Helper record to hold customer info
     */
    private record CustomerInfo(Long customerId, Long tenantId) {}
    
    /**
     * Update an existing address
     */
    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request, String email) {
        log.debug("Updating address: {} for customer email: {}", addressId, email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        Optional<CustomerAddress> existingAddress = addressRepository.findByIdAndCustomerIdAndTenantId(
                addressId, customerInfo.customerId(), customerInfo.tenantId());
        
        if (existingAddress.isEmpty()) {
            throw new IllegalArgumentException("Address not found: " + addressId);
        }
        
        CustomerAddress address = existingAddress.get();
        
        // Handle default status change
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            // Clear existing default and set this as default
            addressRepository.clearDefaultForCustomer(customerInfo.customerId(), customerInfo.tenantId());
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault()) && address.getIsDefault()) {
            // If unsetting default, set first available address as default
            address.setIsDefault(false);
        }
        
        // Update fields
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());
        address.setUpdatedAt(LocalDateTime.now());
        
        CustomerAddress savedAddress = addressRepository.save(address);
        
        // If we unset the default, set another address as default after saving
        if (Boolean.FALSE.equals(request.getIsDefault()) && !savedAddress.getIsDefault()) {
            setFirstAddressAsDefault(customerInfo.customerId(), customerInfo.tenantId());
        }
        
        log.info("Updated address: {} for customer: {}", addressId, customerInfo.customerId());
        
        return AddressResponse.fromEntity(savedAddress);
    }
    
    /**
     * Delete an address (soft delete)
     */
    @Transactional
    public boolean deleteAddress(Long addressId, String email) {
        log.debug("Deleting address: {} for customer email: {}", addressId, email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        
        // Check if address exists and belongs to customer
        if (!addressRepository.existsByIdAndCustomerIdAndTenantId(
                addressId, customerInfo.customerId(), customerInfo.tenantId())) {
            log.warn("Address not found: {} for customer: {}", addressId, customerInfo.customerId());
            return false;
        }
        
        // Check if this is the default address
        Optional<CustomerAddress> address = addressRepository.findByIdAndCustomerIdAndTenantId(
                addressId, customerInfo.customerId(), customerInfo.tenantId());
        boolean wasDefault = address.map(CustomerAddress::getIsDefault).orElse(false);
        
        // Soft delete the address
        int deletedCount = addressRepository.softDeleteByIdAndCustomerIdAndTenantId(
                addressId, customerInfo.customerId(), customerInfo.tenantId());
        
        if (deletedCount > 0) {
            log.info("Deleted address: {} for customer: {}", addressId, customerInfo.customerId());
            
            // If deleted address was default, set another address as default
            if (wasDefault) {
                setFirstAddressAsDefault(customerInfo.customerId(), customerInfo.tenantId());
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Set an address as default
     */
    @Transactional
    public boolean setAsDefault(Long addressId, String email) {
        log.debug("Setting address: {} as default for customer email: {}", addressId, email);
        
        CustomerInfo customerInfo = getCustomerInfo(email);
        Optional<CustomerAddress> addressOpt = addressRepository.findByIdAndCustomerIdAndTenantId(
                addressId, customerInfo.customerId(), customerInfo.tenantId());
        
        if (addressOpt.isEmpty()) {
            log.warn("Address not found: {} for customer: {}", addressId, customerInfo.customerId());
            return false;
        }
        
        CustomerAddress address = addressOpt.get();
        
        if (!address.getIsActive()) {
            log.warn("Cannot set inactive address as default: {} for customer: {}", addressId, customerInfo.customerId());
            return false;
        }
        
        // Clear existing default
        addressRepository.clearDefaultForCustomer(customerInfo.customerId(), customerInfo.tenantId());
        
        // Set this address as default
        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        addressRepository.save(address);
        
        log.info("Set address: {} as default for customer: {}", addressId, customerInfo.customerId());
        return true;
    }
    
    /**
     * Helper method to set first available address as default
     */
    private void setFirstAddressAsDefault(Long customerId, Long tenantId) {
        List<CustomerAddress> addresses = addressRepository.findActiveByCustomerIdAndTenantId(customerId, tenantId);
        
        if (!addresses.isEmpty()) {
            CustomerAddress firstAddress = addresses.get(0);
            firstAddress.setIsDefault(true);
            firstAddress.setUpdatedAt(LocalDateTime.now());
            addressRepository.save(firstAddress);
            
            log.info("Set address: {} as new default for customer: {}", 
                    firstAddress.getId(), customerId);
        }
    }
    
    /**
     * Get address entity by ID (for internal use)
     */
    public CustomerAddress getAddressEntity(Long addressId, Long customerId, Long tenantId) {
        return addressRepository.findByIdAndCustomerIdAndTenantId(addressId, customerId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
    }
}
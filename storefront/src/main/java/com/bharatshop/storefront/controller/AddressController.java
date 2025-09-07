package com.bharatshop.storefront.controller;

import com.bharatshop.storefront.dto.AddressRequest;
import com.bharatshop.storefront.dto.AddressResponse;
import com.bharatshop.storefront.service.AddressService;
import com.bharatshop.storefront.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
// import java.util.UUID; // Replaced with Long

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Management", description = "APIs for managing customer addresses")
public class AddressController {
    
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    
    private final AddressService addressService;
    
    @GetMapping
    @Operation(summary = "Get all addresses", description = "Get all addresses for the authenticated customer")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            List<AddressResponse> addresses = addressService.getCustomerAddresses(customerEmail);
            
            return ResponseEntity.ok(ApiResponse.success(addresses, "Addresses retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error getting addresses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve addresses"));
        }
    }
    
    @GetMapping("/{addressId}")
    @Operation(summary = "Get address by ID", description = "Get a specific address by its ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @Parameter(description = "Address ID") @PathVariable Long addressId,
            Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            Optional<AddressResponse> address = addressService.getAddressById(addressId, customerEmail);
            
            if (address.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(address.get(), "Address retrieved successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Address not found"));
            }
            
        } catch (Exception e) {
            log.error("Error getting address: {}", addressId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve address"));
        }
    }
    
    @GetMapping("/default")
    @Operation(summary = "Get default address", description = "Get the default address for the authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            Optional<AddressResponse> address = addressService.getDefaultAddress(customerEmail);
            
            if (address.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(address.get(), "Default address retrieved successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No default address found"));
            }
            
        } catch (Exception e) {
            log.error("Error getting default address", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve default address"));
        }
    }
    
    @PostMapping
    @Operation(summary = "Create address", description = "Create a new address for the authenticated customer")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            AddressResponse address = addressService.createAddress(request, customerEmail);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(address, "Address created successfully"));
            
        } catch (IllegalStateException e) {
            log.warn("Address creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating address", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create address"));
        }
    }
    
    @PutMapping("/{addressId}")
    @Operation(summary = "Update address", description = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @Parameter(description = "Address ID") @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            AddressResponse address = addressService.updateAddress(addressId, request, customerEmail);
            
            return ResponseEntity.ok(ApiResponse.success(address, "Address updated successfully"));
            
        } catch (Exception e) {
            log.error("Error updating address: {}", addressId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update address"));
        }
    }
    
    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete address", description = "Delete an address (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @Parameter(description = "Address ID") @PathVariable Long addressId,
            Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            boolean deleted = addressService.deleteAddress(addressId, customerEmail);
            
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Address deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Address not found"));
            }
            
        } catch (Exception e) {
            log.error("Error deleting address: {}", addressId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete address"));
        }
    }
    
    @PutMapping("/{addressId}/default")
    @Operation(summary = "Set as default", description = "Set an address as the default address")
    public ResponseEntity<ApiResponse<Void>> setAsDefault(
            @Parameter(description = "Address ID") @PathVariable Long addressId,
            Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            
            boolean success = addressService.setAsDefault(addressId, customerEmail);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success(null, "Address set as default successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Address not found or inactive"));
            }
            
        } catch (Exception e) {
            log.error("Error setting address as default: {}", addressId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to set address as default"));
        }
    }
}
package com.bharatshop.storefront.dto;

import com.bharatshop.shared.entity.CustomerAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    
    private Long id;
    private String name;
    private String phone;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private Boolean isDefault;
    private Boolean isActive;
    private String fullAddress;
    private String shortAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert CustomerAddress entity to AddressResponse DTO
     */
    public static AddressResponse fromEntity(CustomerAddress address) {
        return AddressResponse.builder()
            .id(address.getId())
            .name(address.getName())
            .phone(address.getPhone())
            .line1(address.getLine1())
            .line2(address.getLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .isDefault(address.getIsDefault())
            .isActive(address.getIsActive())
            .fullAddress(address.getFullAddress())
            .shortAddress(address.getShortAddress())
            .createdAt(address.getCreatedAt())
            .updatedAt(address.getUpdatedAt())
            .build();
    }
}
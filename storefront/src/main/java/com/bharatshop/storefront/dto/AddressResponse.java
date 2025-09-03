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
    
    // Manual builder method to bypass Lombok issues
    public static AddressResponseBuilder builder() {
        return new AddressResponseBuilder();
    }
    
    // Manual AddressResponseBuilder class
    public static class AddressResponseBuilder {
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
        
        public AddressResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public AddressResponseBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public AddressResponseBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public AddressResponseBuilder line1(String line1) {
            this.line1 = line1;
            return this;
        }
        
        public AddressResponseBuilder line2(String line2) {
            this.line2 = line2;
            return this;
        }
        
        public AddressResponseBuilder city(String city) {
            this.city = city;
            return this;
        }
        
        public AddressResponseBuilder state(String state) {
            this.state = state;
            return this;
        }
        
        public AddressResponseBuilder pincode(String pincode) {
            this.pincode = pincode;
            return this;
        }
        
        public AddressResponseBuilder country(String country) {
            this.country = country;
            return this;
        }
        
        public AddressResponseBuilder isDefault(Boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }
        
        public AddressResponseBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        public AddressResponseBuilder fullAddress(String fullAddress) {
            this.fullAddress = fullAddress;
            return this;
        }
        
        public AddressResponseBuilder shortAddress(String shortAddress) {
            this.shortAddress = shortAddress;
            return this;
        }
        
        public AddressResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public AddressResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public AddressResponse build() {
            AddressResponse response = new AddressResponse();
            response.id = this.id;
            response.name = this.name;
            response.phone = this.phone;
            response.line1 = this.line1;
            response.line2 = this.line2;
            response.city = this.city;
            response.state = this.state;
            response.pincode = this.pincode;
            response.country = this.country;
            response.isDefault = this.isDefault;
            response.isActive = this.isActive;
            response.fullAddress = this.fullAddress;
            response.shortAddress = this.shortAddress;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
        }
    }
}
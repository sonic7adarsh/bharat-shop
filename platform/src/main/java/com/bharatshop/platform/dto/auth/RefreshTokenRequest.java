package com.bharatshop.platform.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {

    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
    
    // Manual getter for compilation compatibility
    public String getRefreshToken() {
        return refreshToken;
    }
    
    // Manual builder method for compilation compatibility
    public static RefreshTokenRequestBuilder builder() {
        return new RefreshTokenRequestBuilder();
    }
    
    public static class RefreshTokenRequestBuilder {
        private String refreshToken;
        
        public RefreshTokenRequestBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public RefreshTokenRequest build() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.refreshToken = this.refreshToken;
            return request;
        }
    }
}
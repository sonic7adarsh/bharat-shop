package com.bharatshop.platform.dto.auth;

import com.bharatshop.platform.entity.PlatformUser;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response")
public class AuthResponse {

    @Schema(description = "Access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiration time in milliseconds", example = "86400000")
    private long expiresIn;

    @Schema(description = "User information")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserInfo {
        @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID id;

        @Schema(description = "Email address", example = "user@example.com")
        private String email;

        @Schema(description = "User roles", example = "[\"VENDOR\"]")
        private Set<PlatformUser.PlatformRole> roles;

        @Schema(description = "Account enabled status", example = "true")
        private Boolean enabled;

        @Schema(description = "Creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        // Manual builder method for compilation compatibility
        public static UserInfoBuilder builder() {
            return new UserInfoBuilder();
        }
        
        public static class UserInfoBuilder {
            private UUID id;
            private String email;
            private Set<PlatformUser.PlatformRole> roles;
            private Boolean enabled;
            private LocalDateTime createdAt;
            
            public UserInfoBuilder id(UUID id) {
                this.id = id;
                return this;
            }
            
            public UserInfoBuilder email(String email) {
                this.email = email;
                return this;
            }
            
            public UserInfoBuilder roles(Set<PlatformUser.PlatformRole> roles) {
                this.roles = roles;
                return this;
            }
            
            public UserInfoBuilder enabled(Boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            
            public UserInfoBuilder createdAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
                return this;
            }
            
            public UserInfo build() {
                UserInfo userInfo = new UserInfo();
                userInfo.id = this.id;
                userInfo.email = this.email;
                userInfo.roles = this.roles;
                userInfo.enabled = this.enabled;
                userInfo.createdAt = this.createdAt;
                return userInfo;
            }
        }
    }
    
    // Manual builder method for compilation compatibility
    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }
    
    public static class AuthResponseBuilder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserInfo user;
        
        public AuthResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public AuthResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public AuthResponseBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public AuthResponseBuilder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }
        
        public AuthResponseBuilder user(UserInfo user) {
            this.user = user;
            return this;
        }
        
        public AuthResponse build() {
            AuthResponse response = new AuthResponse();
            response.accessToken = this.accessToken;
            response.refreshToken = this.refreshToken;
            response.tokenType = this.tokenType;
            response.expiresIn = this.expiresIn;
            response.user = this.user;
            return response;
        }
    }
}
package com.bharatshop.storefront.shared;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    
    @Schema(description = "Indicates if the request was successful", example = "true")
    private Boolean success;
    
    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "Response data")
    private T data;
    
    @Schema(description = "List of errors if any")
    private List<String> errors;
    
    @Schema(description = "Response timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Schema(description = "Request path", example = "/api/v1/products")
    private String path;
    
    // Static helper methods for success responses
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // Static helper methods for error responses
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, List<String> errors, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // Manual builder method for compilation compatibility
    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<T>();
    }
    
    public static class ApiResponseBuilder<T> {
        private Boolean success;
        private String message;
        private T data;
        private List<String> errors;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String path;
        
        public ApiResponseBuilder<T> success(Boolean success) {
            this.success = success;
            return this;
        }
        
        public ApiResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }
        
        public ApiResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }
        
        public ApiResponseBuilder<T> errors(List<String> errors) {
            this.errors = errors;
            return this;
        }
        
        public ApiResponseBuilder<T> timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public ApiResponseBuilder<T> path(String path) {
            this.path = path;
            return this;
        }
        
        public ApiResponse<T> build() {
            ApiResponse<T> response = new ApiResponse<T>();
            response.success = this.success;
            response.message = this.message;
            response.data = this.data;
            response.errors = this.errors;
            response.timestamp = this.timestamp;
            response.path = this.path;
            return response;
        }
    }
}
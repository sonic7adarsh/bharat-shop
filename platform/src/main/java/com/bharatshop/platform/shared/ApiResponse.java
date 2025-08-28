package com.bharatshop.platform.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private String timestamp;
    private String path;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
    
    public static <T> ApiResponse<T> error(List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .errors(errors)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
    
    // Manual builder method for compilation compatibility
    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<T>();
    }
    
    public static class ApiResponseBuilder<T> {
        private boolean success;
        private String message;
        private T data;
        private List<String> errors;
        private String timestamp;
        private String path;
        
        public ApiResponseBuilder<T> success(boolean success) {
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
        
        public ApiResponseBuilder<T> timestamp(String timestamp) {
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
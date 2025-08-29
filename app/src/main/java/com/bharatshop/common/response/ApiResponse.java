package com.bharatshop.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response envelope for all REST endpoints.
 * Provides consistent response structure across the application.
 *
 * @param <T> the type of data being returned
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetails error,
        String traceId
) {
    
    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, data, null, traceId);
    }
    
    /**
     * Creates a successful response without data.
     */
    public static <T> ApiResponse<T> success(String traceId) {
        return new ApiResponse<>(true, null, null, traceId);
    }
    
    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(ErrorDetails error, String traceId) {
        return new ApiResponse<>(false, null, error, traceId);
    }
    
    /**
     * Creates an error response with message and code.
     */
    public static <T> ApiResponse<T> error(String message, String code, String traceId) {
        return new ApiResponse<>(false, null, new ErrorDetails(message, code), traceId);
    }
}
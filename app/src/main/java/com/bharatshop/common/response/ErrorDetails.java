package com.bharatshop.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Error details for API responses.
 * Provides structured error information with message and code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(
        String message,
        String code
) {
    
    /**
     * Creates error details with message only.
     */
    public static ErrorDetails of(String message) {
        return new ErrorDetails(message, null);
    }
    
    /**
     * Creates error details with message and code.
     */
    public static ErrorDetails of(String message, String code) {
        return new ErrorDetails(message, code);
    }
}
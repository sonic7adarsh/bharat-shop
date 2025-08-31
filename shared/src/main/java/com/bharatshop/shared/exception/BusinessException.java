package com.bharatshop.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom business exception for application-specific errors
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public BusinessException(String message) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    public BusinessException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.BAD_REQUEST);
    }
    
    public BusinessException(String message, HttpStatus httpStatus) {
        this(message, "BUSINESS_ERROR", httpStatus);
    }
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public BusinessException(String message, Throwable cause) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    // Static factory methods for common business exceptions
    public static BusinessException notFound(String entity, Object id) {
        return new BusinessException(
            String.format("%s with id '%s' not found", entity, id),
            "ENTITY_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
    
    public static BusinessException alreadyExists(String entity, String field, Object value) {
        return new BusinessException(
            String.format("%s with %s '%s' already exists", entity, field, value),
            "ENTITY_ALREADY_EXISTS",
            HttpStatus.CONFLICT
        );
    }
    
    public static BusinessException invalidOperation(String operation) {
        return new BusinessException(
            String.format("Invalid operation: %s", operation),
            "INVALID_OPERATION",
            HttpStatus.BAD_REQUEST
        );
    }
    
    public static BusinessException insufficientStock(String product, int available, int requested) {
        return new BusinessException(
            String.format("Insufficient stock for %s. Available: %d, Requested: %d", product, available, requested),
            "INSUFFICIENT_STOCK",
            HttpStatus.BAD_REQUEST
        );
    }
    
    public static BusinessException unauthorized(String action) {
        return new BusinessException(
            String.format("Unauthorized to perform action: %s", action),
            "UNAUTHORIZED_ACTION",
            HttpStatus.FORBIDDEN
        );
    }
    
    public static BusinessException paymentFailed(String reason) {
        return new BusinessException(
            String.format("Payment failed: %s", reason),
            "PAYMENT_FAILED",
            HttpStatus.PAYMENT_REQUIRED
        );
    }
    
    public static BusinessException rateLimitExceeded() {
        return new BusinessException(
            "Rate limit exceeded. Please try again later.",
            "RATE_LIMIT_EXCEEDED",
            HttpStatus.TOO_MANY_REQUESTS
        );
    }
}
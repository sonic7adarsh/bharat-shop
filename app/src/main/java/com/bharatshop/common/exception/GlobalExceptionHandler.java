package com.bharatshop.common.exception;

import com.bharatshop.common.response.ApiResponse;
import com.bharatshop.common.response.ErrorDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses.
 * Handles all exceptions and converts them to standard ApiResponse format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId();
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        logger.warn("Validation error: {} [traceId: {}]", errorMessage, traceId);
        
        ApiResponse<Void> response = ApiResponse.error(
                ErrorDetails.of(errorMessage, "VALIDATION_ERROR"), traceId);
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId();
        logger.warn("Business error: {} [traceId: {}]", ex.getMessage(), traceId);
        
        ApiResponse<Void> response = ApiResponse.error(
                ErrorDetails.of(ex.getMessage(), ex.getErrorCode()), traceId);
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = getOrCreateTraceId();
        logger.error("Unexpected error [traceId: {}]", traceId, ex);
        
        ApiResponse<Void> response = ApiResponse.error(
                ErrorDetails.of("An unexpected error occurred", "INTERNAL_ERROR"), traceId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    private String getOrCreateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }
}
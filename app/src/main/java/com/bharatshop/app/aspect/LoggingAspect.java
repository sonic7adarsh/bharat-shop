package com.bharatshop.app.aspect;

import com.bharatshop.app.util.LoggingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for automatic logging of method entry, exit, and execution time.
 * Provides structured logging for service layer methods and error handling.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Log execution of service layer methods
     */
    @Around("execution(* com.bharatshop.app.service..*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Log execution of controller methods
     */
    @Around("execution(* com.bharatshop.app.controller..*.*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "CONTROLLER");
    }

    /**
     * Log execution of repository methods (excluding basic CRUD)
     */
    @Around("execution(* com.bharatshop.app.repository..*.*(..)) && !execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }

    /**
     * Generic method execution logging
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Create method identifier
        String methodId = className + "." + methodName;
        
        // Log method entry
        logger.debug("{} method entry: {} with args: {}", 
                layer, methodId, sanitizeArgs(args));
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        
        try {
            // Execute the method
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            exception = ex;
            throw ex;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (exception != null) {
                // Log method exit with exception
                logger.error("{} method error: {} - Duration: {}ms - Exception: {}", 
                        layer, methodId, executionTime, exception.getMessage(), exception);
            } else {
                // Log successful method exit
                if (executionTime > 1000) {
                    // Warn for slow methods (> 1 second)
                    logger.warn("{} method exit: {} - Duration: {}ms (SLOW) - Result type: {}", 
                            layer, methodId, executionTime, 
                            result != null ? result.getClass().getSimpleName() : "null");
                } else {
                    logger.debug("{} method exit: {} - Duration: {}ms - Result type: {}", 
                            layer, methodId, executionTime, 
                            result != null ? result.getClass().getSimpleName() : "null");
                }
            }
        }
    }

    /**
     * Sanitize method arguments for logging (remove sensitive data)
     */
    private Object[] sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        
        return Arrays.stream(args)
                .map(this::sanitizeArg)
                .toArray();
    }
    
    /**
     * Sanitize individual argument
     */
    private Object sanitizeArg(Object arg) {
        if (arg == null) {
            return null;
        }
        
        String argString = arg.toString();
        String className = arg.getClass().getSimpleName();
        
        // Don't log sensitive information
        if (className.toLowerCase().contains("password") || 
            className.toLowerCase().contains("token") ||
            className.toLowerCase().contains("secret") ||
            argString.toLowerCase().contains("password") ||
            argString.toLowerCase().contains("token")) {
            return "[REDACTED]";
        }
        
        // Limit string length to prevent log pollution
        if (argString.length() > 200) {
            return argString.substring(0, 200) + "...";
        }
        
        return arg;
    }

    /**
     * Log database operations with tenant context
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logTransactionalMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenantId = LoggingContext.getTenantId();
        String userId = LoggingContext.getUserId();
        String methodName = joinPoint.getSignature().getName();
        
        logger.info("Database transaction started: {} for tenant: {}, user: {}", 
                methodName, tenantId, userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Database transaction completed: {} - Duration: {}ms", 
                    methodName, duration);
            
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            
            logger.error("Database transaction failed: {} - Duration: {}ms - Error: {}", 
                    methodName, duration, ex.getMessage(), ex);
            
            throw ex;
        }
    }
}
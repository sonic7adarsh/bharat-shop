package com.bharatshop.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for price fields
 */
@Documented
@Constraint(validatedBy = ValidPriceValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPrice {
    
    String message() default "Price must be a positive number with maximum 2 decimal places";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    double min() default 0.0;
    
    double max() default Double.MAX_VALUE;
}
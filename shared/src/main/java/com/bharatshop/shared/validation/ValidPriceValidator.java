package com.bharatshop.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validator implementation for ValidPrice annotation
 */
public class ValidPriceValidator implements ConstraintValidator<ValidPrice, BigDecimal> {
    
    private double min;
    private double max;
    
    @Override
    public void initialize(ValidPrice constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }
    
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Check if value is within range
        double doubleValue = value.doubleValue();
        if (doubleValue < min || doubleValue > max) {
            return false;
        }
        
        // Check if value has maximum 2 decimal places
        try {
            BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
            return value.compareTo(rounded) == 0;
        } catch (ArithmeticException e) {
            return false;
        }
    }
}
package com.bharatshop.shared.service;

import org.springframework.stereotype.Service;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling currency formatting per vendor with locale-specific rules
 */
@Service
public class CurrencyService {

    private static final Map<String, String> VENDOR_CURRENCY_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Locale> CURRENCY_LOCALE_MAP = new ConcurrentHashMap<>();
    
    static {
        // Default currency mappings for different regions
        CURRENCY_LOCALE_MAP.put("INR", new Locale("en", "IN"));
        CURRENCY_LOCALE_MAP.put("USD", Locale.US);
        CURRENCY_LOCALE_MAP.put("EUR", Locale.GERMANY);
        CURRENCY_LOCALE_MAP.put("GBP", Locale.UK);
        CURRENCY_LOCALE_MAP.put("JPY", Locale.JAPAN);
        CURRENCY_LOCALE_MAP.put("CAD", Locale.CANADA);
        CURRENCY_LOCALE_MAP.put("AUD", new Locale("en", "AU"));
        
        // Default vendor currency mappings (can be overridden per vendor)
        VENDOR_CURRENCY_MAP.put("default", "INR");
    }
    
    /**
     * Format currency amount for a specific vendor
     * @param amount The amount to format
     * @param vendorId The vendor ID
     * @return Formatted currency string
     */
    public String formatCurrency(BigDecimal amount, String vendorId) {
        String currencyCode = getVendorCurrency(vendorId);
        return formatCurrency(amount, currencyCode);
    }
    
    /**
     * Format currency amount with specific currency code
     * @param amount The amount to format
     * @param currencyCode The currency code (e.g., "INR", "USD")
     * @return Formatted currency string
     */
    public String formatCurrency(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return "";
        }
        
        try {
            Locale locale = getCurrencyLocale(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
            
            // Set the currency for the formatter
            Currency currency = Currency.getInstance(currencyCode);
            formatter.setCurrency(currency);
            
            return formatter.format(amount);
        } catch (Exception e) {
            // Fallback to simple formatting
            return getSimpleCurrencySymbol(currencyCode) + " " + amount.toString();
        }
    }
    
    /**
     * Format currency amount using current locale
     * @param amount The amount to format
     * @param currencyCode The currency code
     * @return Formatted currency string
     */
    public String formatCurrencyWithCurrentLocale(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return "";
        }
        
        try {
            Locale currentLocale = LocaleContextHolder.getLocale();
            NumberFormat formatter = NumberFormat.getCurrencyInstance(currentLocale);
            
            // Set the currency for the formatter
            Currency currency = Currency.getInstance(currencyCode);
            formatter.setCurrency(currency);
            
            return formatter.format(amount);
        } catch (Exception e) {
            // Fallback to default locale formatting
            return formatCurrency(amount, currencyCode);
        }
    }
    
    /**
     * Get currency code for a vendor
     * @param vendorId The vendor ID
     * @return Currency code
     */
    public String getVendorCurrency(String vendorId) {
        return VENDOR_CURRENCY_MAP.getOrDefault(vendorId, VENDOR_CURRENCY_MAP.get("default"));
    }
    
    /**
     * Set currency for a vendor
     * @param vendorId The vendor ID
     * @param currencyCode The currency code
     */
    public void setVendorCurrency(String vendorId, String currencyCode) {
        VENDOR_CURRENCY_MAP.put(vendorId, currencyCode);
    }
    
    /**
     * Get locale for a currency
     * @param currencyCode The currency code
     * @return Locale for the currency
     */
    private Locale getCurrencyLocale(String currencyCode) {
        return CURRENCY_LOCALE_MAP.getOrDefault(currencyCode, new Locale("en", "IN"));
    }
    
    /**
     * Get simple currency symbol for fallback formatting
     * @param currencyCode The currency code
     * @return Currency symbol
     */
    private String getSimpleCurrencySymbol(String currencyCode) {
        switch (currencyCode.toUpperCase()) {
            case "INR": return "₹";
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "JPY": return "¥";
            case "CAD": return "C$";
            case "AUD": return "A$";
            default: return currencyCode;
        }
    }
    
    /**
     * Parse currency amount from formatted string
     * @param formattedAmount The formatted currency string
     * @param currencyCode The expected currency code
     * @return Parsed BigDecimal amount
     */
    public BigDecimal parseCurrency(String formattedAmount, String currencyCode) {
        if (formattedAmount == null || formattedAmount.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            Locale locale = getCurrencyLocale(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
            
            Currency currency = Currency.getInstance(currencyCode);
            formatter.setCurrency(currency);
            
            Number number = formatter.parse(formattedAmount);
            return new BigDecimal(number.toString());
        } catch (Exception e) {
            // Fallback: try to extract numeric value
            String numericString = formattedAmount.replaceAll("[^0-9.,-]", "");
            try {
                return new BigDecimal(numericString);
            } catch (NumberFormatException ex) {
                return BigDecimal.ZERO;
            }
        }
    }
    
    /**
     * Get all supported currencies
     * @return Map of currency codes to their display names
     */
    public Map<String, String> getSupportedCurrencies() {
        Map<String, String> currencies = new ConcurrentHashMap<>();
        currencies.put("INR", "Indian Rupee");
        currencies.put("USD", "US Dollar");
        currencies.put("EUR", "Euro");
        currencies.put("GBP", "British Pound");
        currencies.put("JPY", "Japanese Yen");
        currencies.put("CAD", "Canadian Dollar");
        currencies.put("AUD", "Australian Dollar");
        return currencies;
    }
    
    /**
     * Check if a currency is supported
     * @param currencyCode The currency code to check
     * @return true if supported, false otherwise
     */
    public boolean isCurrencySupported(String currencyCode) {
        return getSupportedCurrencies().containsKey(currencyCode.toUpperCase());
    }
    
    /**
     * Get currency symbol for display
     * @param currencyCode The currency code
     * @return Currency symbol
     */
    public String getCurrencySymbol(String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getSymbol();
        } catch (Exception e) {
            return getSimpleCurrencySymbol(currencyCode);
        }
    }
    
    /**
     * Get currency symbol for a specific locale
     * @param currencyCode The currency code
     * @param locale The locale
     * @return Currency symbol for the locale
     */
    public String getCurrencySymbol(String currencyCode, Locale locale) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getSymbol(locale);
        } catch (Exception e) {
            return getSimpleCurrencySymbol(currencyCode);
        }
    }
}
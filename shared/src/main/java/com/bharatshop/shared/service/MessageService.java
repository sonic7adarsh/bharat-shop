package com.bharatshop.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for handling internationalized messages.
 * Provides easy access to localized content based on current locale.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get localized message for current locale
     */
    public String getMessage(String key) {
        return getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get localized message with parameters for current locale
     */
    public String getMessage(String key, Object[] args) {
        return getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Get localized message for specific locale
     */
    public String getMessage(String key, Locale locale) {
        return getMessage(key, null, locale);
    }

    /**
     * Get localized message with parameters for specific locale
     */
    public String getMessage(String key, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (Exception e) {
            // Fallback to key if message not found
            return key;
        }
    }

    /**
     * Get localized message with default value
     */
    public String getMessage(String key, String defaultMessage) {
        return messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Get localized message with parameters and default value
     */
    public String getMessage(String key, Object[] args, String defaultMessage) {
        return messageSource.getMessage(key, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Get current locale
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * Check if message exists for current locale
     */
    public boolean hasMessage(String key) {
        try {
            messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
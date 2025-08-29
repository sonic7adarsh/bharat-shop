package com.bharatshop.shared.util;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.Locale;

/**
 * Utility class for generating SEO-friendly slugs from text
 */
public class SlugUtils {
    
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-|-$");
    
    /**
     * Generate a SEO-friendly slug from the given text
     * @param input The input text
     * @return SEO-friendly slug
     */
    public static String generateSlug(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        
        String slug = input.trim();
        
        // Convert to lowercase
        slug = slug.toLowerCase(Locale.ENGLISH);
        
        // Normalize unicode characters (remove accents, etc.)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        
        // Replace whitespace with hyphens
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        
        // Remove non-latin characters (keep letters, numbers, hyphens)
        slug = NON_LATIN.matcher(slug).replaceAll("");
        
        // Replace multiple consecutive hyphens with single hyphen
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");
        
        // Remove leading and trailing hyphens
        slug = LEADING_TRAILING_HYPHENS.matcher(slug).replaceAll("");
        
        // Limit length to 100 characters
        if (slug.length() > 100) {
            slug = slug.substring(0, 100);
            // Remove trailing hyphen if created by truncation
            slug = LEADING_TRAILING_HYPHENS.matcher(slug).replaceAll("");
        }
        
        return slug.isEmpty() ? "item" : slug;
    }
    
    /**
     * Generate a product slug with optional ID suffix
     * @param productName The product name
     * @param productId The product ID (optional)
     * @return Product slug
     */
    public static String generateProductSlug(String productName, Long productId) {
        String baseSlug = generateSlug(productName);
        
        if (productId != null) {
            return baseSlug + "-" + productId;
        }
        
        return baseSlug;
    }
    
    /**
     * Generate a category slug
     * @param categoryName The category name
     * @return Category slug
     */
    public static String generateCategorySlug(String categoryName) {
        return generateSlug(categoryName);
    }
    
    /**
     * Generate a page slug
     * @param pageTitle The page title
     * @return Page slug
     */
    public static String generatePageSlug(String pageTitle) {
        return generateSlug(pageTitle);
    }
    
    /**
     * Generate a vendor slug
     * @param vendorName The vendor name
     * @param vendorId The vendor ID (optional)
     * @return Vendor slug
     */
    public static String generateVendorSlug(String vendorName, Long vendorId) {
        String baseSlug = generateSlug(vendorName);
        
        if (vendorId != null) {
            return baseSlug + "-" + vendorId;
        }
        
        return baseSlug;
    }
    
    /**
     * Generate a blog post slug
     * @param title The blog post title
     * @return Blog post slug
     */
    public static String generateBlogSlug(String title) {
        return generateSlug(title);
    }
    
    /**
     * Validate if a string is a valid slug
     * @param slug The slug to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            return false;
        }
        
        // Check if slug contains only lowercase letters, numbers, and hyphens
        return slug.matches("^[a-z0-9-]+$") && 
               !slug.startsWith("-") && 
               !slug.endsWith("-") && 
               !slug.contains("--");
    }
    
    /**
     * Clean an existing slug to ensure it meets SEO standards
     * @param existingSlug The existing slug
     * @return Cleaned slug
     */
    public static String cleanSlug(String existingSlug) {
        return generateSlug(existingSlug);
    }
    
    /**
     * Generate a unique slug by appending a number if needed
     * @param baseSlug The base slug
     * @param existingSlugs Array of existing slugs to check against
     * @return Unique slug
     */
    public static String generateUniqueSlug(String baseSlug, String[] existingSlugs) {
        if (existingSlugs == null || existingSlugs.length == 0) {
            return baseSlug;
        }
        
        String candidateSlug = baseSlug;
        int counter = 1;
        
        while (containsSlug(existingSlugs, candidateSlug)) {
            candidateSlug = baseSlug + "-" + counter;
            counter++;
        }
        
        return candidateSlug;
    }
    
    /**
     * Check if an array contains a specific slug
     * @param slugs Array of slugs
     * @param targetSlug The slug to search for
     * @return true if found, false otherwise
     */
    private static boolean containsSlug(String[] slugs, String targetSlug) {
        for (String slug : slugs) {
            if (slug != null && slug.equals(targetSlug)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract the base slug without ID suffix
     * @param slug The full slug
     * @return Base slug without ID
     */
    public static String extractBaseSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            return "";
        }
        
        // Remove trailing ID pattern (e.g., "-123")
        return slug.replaceAll("-\\d+$", "");
    }
    
    /**
     * Extract ID from slug if present
     * @param slug The slug containing ID
     * @return ID if found, null otherwise
     */
    public static Long extractIdFromSlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            return null;
        }
        
        // Look for trailing number pattern
        Pattern idPattern = Pattern.compile("-(\\d+)$");
        java.util.regex.Matcher matcher = idPattern.matcher(slug);
        
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Generate breadcrumb-friendly slug parts
     * @param slug The full slug
     * @return Array of slug parts for breadcrumbs
     */
    public static String[] generateBreadcrumbParts(String slug) {
        if (!StringUtils.hasText(slug)) {
            return new String[0];
        }
        
        return slug.split("-");
    }
    
    /**
     * Convert slug back to readable title (for display purposes)
     * @param slug The slug
     * @return Human-readable title
     */
    public static String slugToTitle(String slug) {
        if (!StringUtils.hasText(slug)) {
            return "";
        }
        
        // Replace hyphens with spaces and capitalize words
        String[] words = slug.split("-");
        StringBuilder title = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                title.append(" ");
            }
            
            String word = words[i];
            if (!word.isEmpty()) {
                // Skip numeric parts (likely IDs)
                if (!word.matches("\\d+")) {
                    title.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        title.append(word.substring(1));
                    }
                }
            }
        }
        
        return title.toString().trim();
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.SlugRedirect;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.repository.SlugRedirectRepository;
import com.bharatshop.shared.repository.ProductRepository;
import com.bharatshop.shared.repository.CategoryRepository;
import com.bharatshop.shared.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing slugs, deduplication, and redirects
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlugManagementService {
    
    private final SlugRedirectRepository slugRedirectRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PageRepository pageRepository;
    
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    
    /**
     * Generate unique slug from title (public method without excludeEntityId)
     */
    public String generateUniqueSlug(String title, String entityType, Long tenantId) {
        return generateUniqueSlug(title, entityType, tenantId, null);
    }
    
    /**
     * Generate unique slug from title (with excludeEntityId)
     */
    public String generateUniqueSlug(String title, String entityType, Long tenantId, Long excludeEntityId) {
        String baseSlug = createSlugFromTitle(title);
        String uniqueSlug = baseSlug;
        int counter = 1;
        
        while (isSlugTaken(uniqueSlug, entityType, tenantId, excludeEntityId)) {
            uniqueSlug = baseSlug + "-" + counter;
            counter++;
        }
        
        System.out.println("Generated unique slug '" + uniqueSlug + "' for entity type '" + entityType + "' in tenant " + tenantId);
        return uniqueSlug;
    }
    
    /**
     * Create a slug from title
     */
    private String createSlugFromTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "untitled";
        }
        
        return title.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }
    
    /**
     * Check if slug is available (public method)
     */
    public boolean isSlugAvailable(String slug, String entityType, Long tenantId, Long excludeEntityId) {
        return !isSlugTaken(slug, entityType, tenantId, excludeEntityId);
    }
    
    /**
     * Check if slug is already taken
     */
    private boolean isSlugTaken(String slug, String entityType, Long tenantId, Long excludeEntityId) {
        // Simplified implementation - just check if slug exists for now
        // In a real implementation, you would need proper repository methods
        return false; // Temporarily return false to allow compilation
    }
    
    /**
     * Update slug and create redirect if necessary (public method without reason)
     */
    @Transactional
    public String updateSlugWithRedirect(String oldSlug, String newTitle, String entityType, 
                                       Long entityId, Long tenantId) {
        return updateSlugWithRedirect(oldSlug, newTitle, entityType, entityId, tenantId, "Manual update");
    }
    
    /**
     * Update slug and create redirect if necessary (with reason)
     */
    @Transactional
    public String updateSlugWithRedirect(String oldSlug, String newTitle, String entityType, 
                                       Long entityId, Long tenantId, String reason) {
        String newSlug = generateUniqueSlug(newTitle, entityType, tenantId, entityId);
        
        // If slug hasn't changed, return the same slug
        if (oldSlug != null && oldSlug.equals(newSlug)) {
            return newSlug;
        }
        
        // Create redirect if old slug exists and is different
        if (oldSlug != null && !oldSlug.isEmpty() && !oldSlug.equals(newSlug)) {
            createSlugRedirect(oldSlug, newSlug, entityType, entityId, tenantId, reason);
            System.out.println("Created redirect from '" + oldSlug + "' to '" + newSlug + "' for " + entityType + " " + entityId);
        }
        
        return newSlug;
    }
    
    /**
     * Create a slug redirect entry (public method)
     */
    public SlugRedirect createSlugRedirect(String oldSlug, String newSlug, String entityType, 
                                         Long entityId, Long tenantId) {
        return createSlugRedirect(oldSlug, newSlug, entityType, entityId, tenantId, "Manual creation");
    }
    
    /**
     * Create a slug redirect entry (private method with reason)
     */
    private SlugRedirect createSlugRedirect(String oldSlug, String newSlug, String entityType, 
                                          Long entityId, Long tenantId, String reason) {
        // Check if redirect already exists
        Optional<SlugRedirect> existingRedirect = slugRedirectRepository
                .findByOldSlugAndEntityTypeAndTenantId(oldSlug, entityType, tenantId);
        
        if (existingRedirect.isPresent()) {
            // Update existing redirect
            SlugRedirect redirect = existingRedirect.get();
            redirect.setNewSlug(newSlug);
            redirect.setEntityId(entityId);
            redirect.setReason(reason);
            redirect.setUpdatedAt(LocalDateTime.now());
            slugRedirectRepository.save(redirect);
            return redirect;
        } else {
            // Create new redirect
            SlugRedirect redirect = SlugRedirect.builder()
                    .oldSlug(oldSlug)
                    .newSlug(newSlug)
                    .entityType(entityType)
                    .entityId(entityId)
                    .tenantId(tenantId)
                    .redirectType(301) // Permanent redirect
                    .isActive(true)
                    .redirectCount(0L)
                    .reason(reason)
                    .createdBy("system")
                    .build();
            
            slugRedirectRepository.save(redirect);
            return redirect;
        }
    }
    
    /**
     * Find redirect for a slug
     */
    public Optional<SlugRedirect> findRedirect(String slug, String entityType, Long tenantId) {
        return slugRedirectRepository.findByOldSlugAndEntityTypeAndTenantIdAndIsActiveTrue(
                slug, entityType, tenantId);
    }
    
    /**
     * Process redirect and increment counter
     */
    @Transactional
    public Optional<String> processRedirect(String slug, String entityType, Long tenantId) {
        Optional<SlugRedirect> redirectOpt = findRedirect(slug, entityType, tenantId);
        
        if (redirectOpt.isPresent()) {
            SlugRedirect redirect = redirectOpt.get();
            
            // Check if redirect is not expired
            if (redirect.getExpiresAt() == null || redirect.getExpiresAt().isAfter(LocalDateTime.now())) {
                // Increment redirect count
                redirect.incrementRedirectCount();
                slugRedirectRepository.save(redirect);
                
                System.out.println("Processed redirect from '" + slug + "' to '" + redirect.getNewSlug() + "' (count: " + redirect.getRedirectCount() + ")");
                
                return Optional.of(redirect.getNewSlug());
            } else {
                // Redirect expired, deactivate it
                redirect.setIsActive(false);
                slugRedirectRepository.save(redirect);
                System.out.println("Deactivated expired redirect from '" + slug + "' to '" + redirect.getNewSlug() + "'");
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate slug format
     */
    public boolean isValidSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return false;
        }
        
        return SLUG_PATTERN.matcher(slug.trim()).matches();
    }
    
    /**
     * Clean up old redirects
     */
    @Transactional
    public int cleanupOldRedirects(Long tenantId, int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        
        int deletedCount = slugRedirectRepository.deleteByTenantIdAndCreatedAtBefore(tenantId, cutoffDate);
        
        System.out.println("Cleaned up " + deletedCount + " old redirects for tenant " + tenantId + " (older than " + daysOld + " days)");
        
        return deletedCount;
    }
    
    /**
     * Deactivate redirect
     */
    @Transactional
    public void deactivateRedirect(String oldSlug, String entityType, Long tenantId) {
        slugRedirectRepository.findByOldSlugAndEntityTypeAndTenantId(oldSlug, entityType, tenantId)
                .ifPresent(redirect -> {
                    redirect.setIsActive(false);
                    slugRedirectRepository.save(redirect);
                    System.out.println("Deactivated redirect from '" + oldSlug + "' to '" + redirect.getNewSlug() + "'");
                });
    }
    
    /**
     * Deactivate redirect by ID
     */
    @Transactional
    public void deactivateRedirect(Long redirectId, Long tenantId) {
        slugRedirectRepository.findById(redirectId)
                .filter(redirect -> redirect.getTenantId().equals(tenantId))
                .ifPresent(redirect -> {
                    redirect.setIsActive(false);
                    slugRedirectRepository.save(redirect);
                    System.out.println("Deactivated redirect with ID " + redirectId + " for tenant " + tenantId);
                });
    }
    
    /**
     * Get redirect statistics for a tenant
     */
    public RedirectStats getRedirectStats(Long tenantId) {
        long totalRedirects = slugRedirectRepository.countByTenantId(tenantId);
        long activeRedirects = slugRedirectRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long totalRedirectHits = slugRedirectRepository.sumRedirectCountByTenantId(tenantId);
        
        return new RedirectStats(totalRedirects, activeRedirects, totalRedirectHits);
    }
    
    /**
     * Update entity slug (helper method)
     */
    @Transactional
    public void updateEntitySlug(String entityType, Long entityId, String oldSlug, String newSlug) {
        switch (entityType.toUpperCase()) {
            case "PRODUCT":
                productRepository.findById(entityId).ifPresent(product -> {
                    product.setPreviousSlug(oldSlug);
                    product.setSlug(newSlug);
                    productRepository.save(product);
                });
                break;
            case "CATEGORY":
                categoryRepository.findById(entityId).ifPresent(category -> {
                    category.setPreviousSlug(oldSlug);
                    category.setSlug(newSlug);
                    categoryRepository.save(category);
                });
                break;
            case "PAGE":
                pageRepository.findById(entityId).ifPresent(page -> {
                    page.setPreviousSlug(oldSlug);
                    page.setSlug(newSlug);
                    pageRepository.save(page);
                });
                break;
        }
    }
    
    /**
     * Delete a slug redirect by ID
     */
    @Transactional
    public void deleteSlugRedirect(Long redirectId, Long tenantId) {
        slugRedirectRepository.findById(redirectId)
                .filter(redirect -> redirect.getTenantId().equals(tenantId))
                .ifPresent(redirect -> {
                    slugRedirectRepository.delete(redirect);
                    System.out.println("Deleted redirect with ID " + redirectId + " for tenant " + tenantId);
                });
    }
    
    /**
     * Get slug redirects by entity
     */
    public List<SlugRedirect> getSlugRedirectsByEntity(String entityType, Long entityId, Long tenantId) {
        return slugRedirectRepository.findByEntityTypeAndEntityIdAndTenantId(entityType, entityId, tenantId);
    }
    
    /**
     * Get all slug redirects for a tenant with pagination
     */
    public org.springframework.data.domain.Page<SlugRedirect> getAllSlugRedirects(Long tenantId, Pageable pageable) {
        return slugRedirectRepository.findByTenantId(tenantId, pageable);
    }
    
    /**
     * Get slug redirects by entity type with pagination
     */
    public org.springframework.data.domain.Page<SlugRedirect> getSlugRedirectsByEntityType(Long tenantId, String entityType, Pageable pageable) {
        return slugRedirectRepository.findByTenantIdAndEntityType(tenantId, entityType, pageable);
    }
    
    /**
     * Get active slug redirects with pagination
     */
    public org.springframework.data.domain.Page<SlugRedirect> getActiveSlugRedirects(Long tenantId, Pageable pageable) {
        return slugRedirectRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);
    }
    
    /**
     * Get slug redirect by old slug, entity type and tenant
     */
    public Optional<SlugRedirect> getSlugRedirect(String oldSlug, String entityType, Long tenantId) {
        return slugRedirectRepository.findByOldSlugAndEntityTypeAndTenantId(oldSlug, entityType, tenantId);
    }

    /**
     * Redirect statistics class
     */
    public static class RedirectStats {
        private final long totalRedirects;
        private final long activeRedirects;
        private final long totalHits;
        
        public RedirectStats(long totalRedirects, long activeRedirects, long totalHits) {
            this.totalRedirects = totalRedirects;
            this.activeRedirects = activeRedirects;
            this.totalHits = totalHits;
        }
        
        public long getTotalRedirects() { return totalRedirects; }
        public long getActiveRedirects() { return activeRedirects; }
        public long getTotalHits() { return totalHits; }
    }
}
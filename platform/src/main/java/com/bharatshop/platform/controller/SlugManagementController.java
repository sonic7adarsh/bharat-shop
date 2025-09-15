package com.bharatshop.platform.controller;

import com.bharatshop.shared.service.SlugManagementService;
import com.bharatshop.shared.entity.SlugRedirect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for managing slug redirects and slug operations
 */
@RestController
@RequestMapping("/api/slug-management")
@RequiredArgsConstructor
@Slf4j
public class SlugManagementController {

    private final SlugManagementService slugManagementService;

    /**
     * Generate a unique slug from a title
     */
    @PostMapping("/generate-slug")
    public ResponseEntity<String> generateSlug(
            @RequestParam String title,
            @RequestParam String entityType,
            @RequestParam Long tenantId) {
        try {
            String slug = slugManagementService.generateUniqueSlug(title, entityType, tenantId);
            return ResponseEntity.ok(slug);
        } catch (Exception e) {
            log.error("Error generating slug for title '{}' in tenant {}: {}", title, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if a slug is available
     */
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkSlugAvailability(
            @RequestParam String slug,
            @RequestParam String entityType,
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long excludeEntityId) {
        try {
            boolean isAvailable = slugManagementService.isSlugAvailable(slug, entityType, tenantId, excludeEntityId);
            return ResponseEntity.ok(isAvailable);
        } catch (Exception e) {
            log.error("Error checking slug availability for '{}' in tenant {}: {}", slug, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update slug with redirect creation
     */
    @PutMapping("/update-slug")
    public ResponseEntity<String> updateSlugWithRedirect(
            @RequestParam String oldSlug,
            @RequestParam String newSlug,
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam Long tenantId) {
        try {
            String finalSlug = slugManagementService.updateSlugWithRedirect(
                    oldSlug, newSlug, entityType, entityId, tenantId);
            return ResponseEntity.ok(finalSlug);
        } catch (Exception e) {
            log.error("Error updating slug from '{}' to '{}' for {} {} in tenant {}: {}", 
                    oldSlug, newSlug, entityType, entityId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a slug redirect manually
     */
    @PostMapping("/redirects")
    public ResponseEntity<SlugRedirect> createSlugRedirect(@RequestBody SlugRedirect slugRedirect) {
        try {
            SlugRedirect created = slugManagementService.createSlugRedirect(
                    slugRedirect.getOldSlug(),
                    slugRedirect.getNewSlug(),
                    slugRedirect.getEntityType(),
                    slugRedirect.getEntityId(),
                    slugRedirect.getTenantId()
            );
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating slug redirect: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all redirects for a tenant
     */
    @GetMapping("/redirects")
    public ResponseEntity<Page<SlugRedirect>> getSlugRedirects(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        try {
            Page<SlugRedirect> redirects;
            if (entityType != null) {
                redirects = slugManagementService.getSlugRedirectsByEntityType(tenantId, entityType, pageable);
            } else if (isActive != null) {
                redirects = slugManagementService.getActiveSlugRedirects(tenantId, pageable);
            } else {
                redirects = slugManagementService.getAllSlugRedirects(tenantId, pageable);
            }
            return ResponseEntity.ok(redirects);
        } catch (Exception e) {
            log.error("Error getting slug redirects for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get redirect by old slug
     */
    @GetMapping("/redirects/by-slug")
    public ResponseEntity<SlugRedirect> getRedirectByOldSlug(
            @RequestParam String oldSlug,
            @RequestParam String entityType,
            @RequestParam Long tenantId) {
        try {
            Optional<SlugRedirect> redirect = slugManagementService.getSlugRedirect(oldSlug, entityType, tenantId);
            return redirect.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting redirect for slug '{}' in tenant {}: {}", oldSlug, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process a redirect (increment hit count)
     */
    @PostMapping("/redirects/process")
    public ResponseEntity<String> processRedirect(
            @RequestParam String oldSlug,
            @RequestParam String entityType,
            @RequestParam Long tenantId) {
        try {
            Optional<String> newSlugOpt = slugManagementService.processRedirect(oldSlug, entityType, tenantId);
            if (newSlugOpt.isPresent()) {
                return ResponseEntity.ok(newSlugOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error processing redirect for slug '{}' in tenant {}: {}", oldSlug, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Deactivate a redirect
     */
    @PutMapping("/redirects/{id}/deactivate")
    public ResponseEntity<Void> deactivateRedirect(
            @PathVariable Long id,
            @RequestParam Long tenantId) {
        try {
            slugManagementService.deactivateRedirect(id, tenantId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deactivating redirect {} in tenant {}: {}", id, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete a redirect
     */
    @DeleteMapping("/redirects/{id}")
    public ResponseEntity<Void> deleteRedirect(
            @PathVariable Long id,
            @RequestParam Long tenantId) {
        try {
            slugManagementService.deleteSlugRedirect(id, tenantId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting redirect {} in tenant {}: {}", id, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clean up old redirects
     */
    @PostMapping("/redirects/cleanup")
    public ResponseEntity<Integer> cleanupOldRedirects(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "365") int daysOld) {
        try {
            int deletedCount = slugManagementService.cleanupOldRedirects(tenantId, daysOld);
            return ResponseEntity.ok(deletedCount);
        } catch (Exception e) {
            log.error("Error cleaning up old redirects for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get redirect statistics
     */
    @GetMapping("/redirects/stats")
    public ResponseEntity<SlugManagementService.RedirectStats> getRedirectStats(@RequestParam Long tenantId) {
        try {
            SlugManagementService.RedirectStats stats = slugManagementService.getRedirectStats(tenantId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting redirect stats for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate a slug format
     */
    @GetMapping("/validate-slug")
    public ResponseEntity<Boolean> validateSlug(@RequestParam String slug) {
        try {
            boolean isValid = slugManagementService.isValidSlug(slug);
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            log.error("Error validating slug '{}': {}", slug, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get redirects by entity
     */
    @GetMapping("/redirects/by-entity")
    public ResponseEntity<List<SlugRedirect>> getRedirectsByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam Long tenantId) {
        try {
            List<SlugRedirect> redirects = slugManagementService.getSlugRedirectsByEntity(entityType, entityId, tenantId);
            return ResponseEntity.ok(redirects);
        } catch (Exception e) {
            log.error("Error getting redirects for {} {} in tenant {}: {}", 
                    entityType, entityId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
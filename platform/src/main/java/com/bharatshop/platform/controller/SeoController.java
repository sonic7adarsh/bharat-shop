package com.bharatshop.platform.controller;

import com.bharatshop.shared.service.SitemapService;
import com.bharatshop.shared.service.RobotsService;
import com.bharatshop.shared.service.SeoMetadataService;
import com.bharatshop.shared.service.StructuredDataService;
import com.bharatshop.shared.entity.SeoMetadata;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.platform.service.PlatformProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * SEO Controller for managing SEO-related endpoints
 * Handles sitemap.xml, robots.txt, and SEO metadata operations
 */
@RestController
@RequestMapping("/api/seo")
@RequiredArgsConstructor
@Slf4j
public class SeoController {

    private final SitemapService sitemapService;
    private final RobotsService robotsService;
    private final SeoMetadataService seoMetadataService;
    private final StructuredDataService structuredDataService;
    private final PlatformProductService platformProductService;

    /**
     * Generate sitemap.xml for a tenant
     */
    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> getSitemap(@RequestParam Long tenantId) {
        try {
            String sitemap = sitemapService.generateSitemap(tenantId, "https://example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Cache-Control", "public, max-age=3600"); // Cache for 1 hour
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(sitemap);
        } catch (Exception e) {
            log.error("Error generating sitemap for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate sitemap index for a tenant
     */
    @GetMapping("/sitemap-index.xml")
    public ResponseEntity<String> getSitemapIndex(@RequestParam Long tenantId) {
        try {
            String sitemapIndex = sitemapService.generateSitemapIndex(tenantId, "https://example.com", java.util.Arrays.asList("https://example.com/sitemap.xml"));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Cache-Control", "public, max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(sitemapIndex);
        } catch (Exception e) {
            log.error("Error generating sitemap index for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate robots.txt for a tenant
     */
    @GetMapping("/robots.txt")
    public ResponseEntity<String> getRobots(@RequestParam Long tenantId) {
        try {
            String robots = robotsService.generateRobotsTxt(tenantId, "https://example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("Cache-Control", "public, max-age=86400"); // Cache for 24 hours
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(robots);
        } catch (Exception e) {
            log.error("Error generating robots.txt for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get SEO metadata for an entity
     */
    @GetMapping("/metadata")
    public ResponseEntity<SeoMetadata> getSeoMetadata(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam Long tenantId) {
        try {
            Optional<SeoMetadata> metadata = seoMetadataService.getSeoMetadata(entityType, entityId, tenantId);
            return metadata.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting SEO metadata for {} {} in tenant {}: {}", 
                    entityType, entityId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create or update SEO metadata for a product
     */
    @PostMapping("/metadata/product")
    public ResponseEntity<SeoMetadata> createProductSeoMetadata(
            @RequestParam Long productId,
            @RequestParam Long tenantId,
            @RequestBody SeoMetadata seoMetadata) {
        try {
            SeoMetadata created = seoMetadataService.createProductSeoMetadata(productId, tenantId, seoMetadata);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating product SEO metadata for product {} in tenant {}: {}", 
                    productId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create or update SEO metadata for a category
     */
    @PostMapping("/metadata/category")
    public ResponseEntity<SeoMetadata> createCategorySeoMetadata(
            @RequestParam Long categoryId,
            @RequestParam Long tenantId,
            @RequestBody SeoMetadata seoMetadata) {
        try {
            SeoMetadata created = seoMetadataService.createCategorySeoMetadata(categoryId, tenantId, seoMetadata);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating category SEO metadata for category {} in tenant {}: {}", 
                    categoryId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create or update SEO metadata for a page
     */
    @PostMapping("/metadata/page")
    public ResponseEntity<SeoMetadata> createPageSeoMetadata(
            @RequestParam Long pageId,
            @RequestParam Long tenantId,
            @RequestBody SeoMetadata seoMetadata) {
        try {
            SeoMetadata created = seoMetadataService.createPageSeoMetadata(pageId, tenantId, seoMetadata);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating page SEO metadata for page {} in tenant {}: {}", 
                    pageId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update SEO metadata
     */
    @PutMapping("/metadata/{id}")
    public ResponseEntity<SeoMetadata> updateSeoMetadata(
            @PathVariable Long id,
            @RequestParam Long tenantId,
            @RequestBody SeoMetadata seoMetadata) {
        try {
            SeoMetadata updated = seoMetadataService.updateSeoMetadata(id, tenantId, seoMetadata);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating SEO metadata {} in tenant {}: {}", 
                    id, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete SEO metadata
     */
    @DeleteMapping("/metadata/{id}")
    public ResponseEntity<Void> deleteSeoMetadata(
            @PathVariable Long id,
            @RequestParam Long tenantId) {
        try {
            seoMetadataService.deleteSeoMetadata(id, tenantId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting SEO metadata {} in tenant {}: {}", 
                    id, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate structured data for a product
     */
    @GetMapping("/structured-data/product")
    public ResponseEntity<String> getProductStructuredData(
            @RequestParam Long productId,
            @RequestParam Long tenantId) {
        try {
            Optional<Product> productOpt = platformProductService.getProductById(productId, tenantId);
            if (productOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            String structuredData = structuredDataService.generateProductStructuredData(productOpt.get(), "https://example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(structuredData);
        } catch (Exception e) {
            log.error("Error generating structured data for product {} in tenant {}: {}", 
                    productId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate meta tags HTML for an entity
     */
    @GetMapping("/meta-tags")
    public ResponseEntity<String> getMetaTags(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam Long tenantId) {
        try {
            String metaTags = seoMetadataService.generateMetaTagsHtml(entityType, entityId, tenantId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(metaTags);
        } catch (Exception e) {
            log.error("Error generating meta tags for {} {} in tenant {}: {}", 
                    entityType, entityId, tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all SEO metadata for a tenant
     */
    @GetMapping("/metadata/all")
    public ResponseEntity<List<SeoMetadata>> getAllSeoMetadata(@RequestParam Long tenantId) {
        try {
            List<SeoMetadata> metadataList = seoMetadataService.getAllSeoMetadataByTenant(tenantId);
            return ResponseEntity.ok(metadataList);
        } catch (Exception e) {
            log.error("Error getting all SEO metadata for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
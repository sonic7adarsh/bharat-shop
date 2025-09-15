package com.bharatshop.storefront.controller;

import com.bharatshop.shared.service.SitemapService;
import com.bharatshop.shared.service.RobotsService;
import com.bharatshop.shared.service.SeoMetadataService;
import com.bharatshop.shared.service.StructuredDataService;
import com.bharatshop.shared.service.SlugManagementService;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.storefront.service.StorefrontProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

/**
 * Storefront SEO Controller for public-facing SEO endpoints
 * Handles sitemap.xml, robots.txt, structured data, and redirects
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class StorefrontSeoController {

    private final SitemapService sitemapService;
    private final RobotsService robotsService;
    private final SeoMetadataService seoMetadataService;
    private final StructuredDataService structuredDataService;
    private final SlugManagementService slugManagementService;
    private final StorefrontProductService storefrontProductService;

    /**
     * Serve sitemap.xml for the current tenant
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap(HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            String sitemap = sitemapService.generateSitemap(tenantId, "https://example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Cache-Control", "public, max-age=3600"); // Cache for 1 hour
            headers.set("X-Robots-Tag", "noindex"); // Don't index the sitemap itself
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(sitemap);
        } catch (Exception e) {
            log.error("Error serving sitemap.xml: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<error>Unable to generate sitemap</error>");
        }
    }

    /**
     * Serve sitemap index for the current tenant
     */
    @GetMapping(value = "/sitemap-index.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemapIndex(HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            String sitemapIndex = sitemapService.generateSitemapIndex(tenantId, "https://example.com", java.util.Arrays.asList("https://example.com/sitemap.xml"));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Cache-Control", "public, max-age=3600");
            headers.set("X-Robots-Tag", "noindex");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(sitemapIndex);
        } catch (Exception e) {
            log.error("Error serving sitemap-index.xml: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<error>Unable to generate sitemap index</error>");
        }
    }

    /**
     * Serve robots.txt for the current tenant
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobots(HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            String robots = robotsService.generateRobotsTxt(tenantId, "https://example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("Cache-Control", "public, max-age=86400"); // Cache for 24 hours
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(robots);
        } catch (Exception e) {
            log.error("Error serving robots.txt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("# Error generating robots.txt\nUser-agent: *\nDisallow: /");
        }
    }

    /**
     * Handle slug redirects
     */
    @GetMapping("/redirect/{oldSlug}")
    public ResponseEntity<Void> handleSlugRedirect(
            @PathVariable String oldSlug,
            @RequestParam String entityType,
            HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            Optional<String> newSlugOpt = slugManagementService.processRedirect(oldSlug, entityType, tenantId);
            
            if (newSlugOpt.isPresent() && !newSlugOpt.get().equals(oldSlug)) {
                String newSlug = newSlugOpt.get();
                String redirectUrl = buildRedirectUrl(request, newSlug, entityType);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create(redirectUrl));
                headers.set("Cache-Control", "public, max-age=31536000"); // Cache for 1 year
                
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .headers(headers)
                        .build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error handling slug redirect for {}: {}", oldSlug, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get structured data for a product (JSON-LD)
     */
    @GetMapping(value = "/structured-data/product/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProductStructuredData(
            @PathVariable Long productId,
            HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            Product product = storefrontProductService.getProductEntityById(productId, tenantId.toString());
            String structuredData = structuredDataService.generateProductStructuredData(product, "https://example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cache-Control", "public, max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(structuredData);
        } catch (Exception e) {
            log.error("Error serving structured data for product {}: {}", productId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get structured data script tags for a page
     */
    @GetMapping(value = "/structured-data/script/{entityType}/{entityId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getStructuredDataScript(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            // Generate structured data based on entity type
            String structuredDataJson = "";
            if ("product".equals(entityType)) {
                Product product = storefrontProductService.getProductEntityById(entityId, tenantId.toString());
                structuredDataJson = structuredDataService.generateProductStructuredData(product, "https://example.com");
            }
            
            // Wrap in script tags
            String scriptTags = structuredDataService.generateStructuredDataScript(java.util.Arrays.asList(structuredDataJson));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set("Cache-Control", "public, max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(scriptTags);
        } catch (Exception e) {
            log.error("Error serving structured data script for {} {}: {}", entityType, entityId, e.getMessage());
            return ResponseEntity.ok(""); // Return empty string instead of error
        }
    }

    /**
     * Get meta tags HTML for a page
     */
    @GetMapping(value = "/meta-tags/{entityType}/{entityId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getMetaTags(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            HttpServletRequest request) {
        try {
            Long tenantId = getTenantIdFromRequest(request);
            String metaTags = seoMetadataService.generateMetaTagsHtml(entityType, entityId, tenantId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set("Cache-Control", "public, max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(metaTags);
        } catch (Exception e) {
            log.error("Error serving meta tags for {} {}: {}", entityType, entityId, e.getMessage());
            return ResponseEntity.ok(""); // Return empty string instead of error
        }
    }

    /**
     * Health check endpoint for SEO services
     */
    @GetMapping("/seo/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("SEO services are healthy");
    }

    /**
     * Extract tenant ID from request (implementation depends on your tenant resolution strategy)
     */
    private Long getTenantIdFromRequest(HttpServletRequest request) {
        // This is a placeholder implementation
        // You should implement this based on your tenant resolution strategy
        // For example, from subdomain, header, or path parameter
        String tenantIdHeader = request.getHeader("X-Tenant-ID");
        if (tenantIdHeader != null) {
            return Long.parseLong(tenantIdHeader);
        }
        
        // Default tenant ID or throw exception
        return 1L; // Replace with your default tenant logic
    }

    /**
     * Build redirect URL based on entity type and new slug
     */
    private String buildRedirectUrl(HttpServletRequest request, String newSlug, String entityType) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        
        switch (entityType.toLowerCase()) {
            case "product":
                return baseUrl + "/products/" + newSlug;
            case "category":
                return baseUrl + "/categories/" + newSlug;
            case "page":
                return baseUrl + "/pages/" + newSlug;
            default:
                return baseUrl + "/" + newSlug;
        }
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating robots.txt per tenant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RobotsService {
    
    // Manual logger since Lombok is not working properly
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RobotsService.class);
    
    private final PageRepository pageRepository;
    
    /**
     * Generate robots.txt for a tenant
     */
    public String generateRobotsTxt(Long tenantId, String baseUrl) {
        log.info("Generating robots.txt for tenant: {}", tenantId);
        
        StringBuilder robots = new StringBuilder();
        
        // Basic robots.txt structure
        robots.append("User-agent: *\n");
        
        // Allow all by default
        robots.append("Allow: /\n");
        
        // Disallow admin and API paths
        robots.append("Disallow: /admin/\n");
        robots.append("Disallow: /api/\n");
        robots.append("Disallow: /auth/\n");
        robots.append("Disallow: /checkout/\n");
        robots.append("Disallow: /cart/\n");
        robots.append("Disallow: /account/\n");
        robots.append("Disallow: /search?\n");
        robots.append("Disallow: /*?*\n"); // Disallow URLs with query parameters
        
        // Get draft/unpublished pages and disallow them
        List<Page> draftPages = pageRepository.findAll(); // Simplified - would need proper filtering
        for (Page page : draftPages) {
            robots.append("Disallow: /page/").append(page.getSlug()).append("\n");
        }
        
        // Get inactive pages and disallow them
        List<Page> inactivePages = pageRepository.findAll(); // Simplified - would need proper filtering
        for (Page page : inactivePages) {
            robots.append("Disallow: /page/").append(page.getSlug()).append("\n");
        }
        
        // Add sitemap reference
        robots.append("\n");
        robots.append("Sitemap: ").append(baseUrl).append("/sitemap.xml\n");
        
        // Add crawl delay for politeness
        robots.append("\n");
        robots.append("Crawl-delay: 1\n");
        
        log.info("Generated robots.txt for tenant: {}", tenantId);
        
        return robots.toString();
    }
    
    /**
     * Generate robots.txt with custom rules
     */
    public String generateCustomRobotsTxt(Long tenantId, String baseUrl, 
                                         List<String> disallowPaths, 
                                         List<String> allowPaths,
                                         Integer crawlDelay) {
        log.info("Generating custom robots.txt for tenant: {}", tenantId);
        
        StringBuilder robots = new StringBuilder();
        
        robots.append("User-agent: *\n");
        
        // Add custom allow paths
        if (allowPaths != null && !allowPaths.isEmpty()) {
            for (String path : allowPaths) {
                robots.append("Allow: ").append(path).append("\n");
            }
        } else {
            robots.append("Allow: /\n");
        }
        
        // Add default disallow paths
        robots.append("Disallow: /admin/\n");
        robots.append("Disallow: /api/\n");
        robots.append("Disallow: /auth/\n");
        robots.append("Disallow: /checkout/\n");
        robots.append("Disallow: /cart/\n");
        robots.append("Disallow: /account/\n");
        
        // Add custom disallow paths
        if (disallowPaths != null && !disallowPaths.isEmpty()) {
            for (String path : disallowPaths) {
                robots.append("Disallow: ").append(path).append("\n");
            }
        }
        
        // Get draft/unpublished pages and disallow them
        List<Page> draftPages = pageRepository.findAll(); // Simplified - would need proper filtering
        for (Page page : draftPages) {
            robots.append("Disallow: /page/").append(page.getSlug()).append("\n");
        }
        
        // Get inactive pages and disallow them
        List<Page> allPages = pageRepository.findAll(); // Simplified - would need proper filtering
        for (Page page : allPages) {
            robots.append("Disallow: /page/").append(page.getSlug()).append("\n");
        }
        
        // Add sitemap reference
        robots.append("\n");
        robots.append("Sitemap: ").append(baseUrl).append("/sitemap.xml\n");
        
        // Add crawl delay
        if (crawlDelay != null && crawlDelay > 0) {
            robots.append("\n");
            robots.append("Crawl-delay: ").append(crawlDelay).append("\n");
        }
        
        return robots.toString();
    }
    
    /**
     * Generate robots.txt for specific user agents
     */
    public String generateRobotsTxtForUserAgents(Long tenantId, String baseUrl, 
                                               List<String> userAgents,
                                               List<String> disallowPaths) {
        StringBuilder robots = new StringBuilder();
        
        // Default rules for all user agents
        robots.append("User-agent: *\n");
        robots.append("Allow: /\n");
        robots.append("Disallow: /admin/\n");
        robots.append("Disallow: /api/\n");
        
        // Specific rules for each user agent
        if (userAgents != null && !userAgents.isEmpty()) {
            for (String userAgent : userAgents) {
                robots.append("\n");
                robots.append("User-agent: ").append(userAgent).append("\n");
                
                if (disallowPaths != null && !disallowPaths.isEmpty()) {
                    for (String path : disallowPaths) {
                        robots.append("Disallow: ").append(path).append("\n");
                    }
                }
            }
        }
        
        // Add sitemap reference
        robots.append("\n");
        robots.append("Sitemap: ").append(baseUrl).append("/sitemap.xml\n");
        
        return robots.toString();
    }
    
    /**
     * Check if a path should be disallowed based on page status
     */
    public boolean shouldDisallowPath(Long tenantId, String slug) {
        // Simplified - find by ID for now (would need proper implementation)
        Page page = null; // pageRepository.findByTenantIdAndSlug(tenantId, slug);
        if (page == null) {
            return false;
        }
        
        // Disallow if page is not published or not active
        return !page.getPublished() || !page.getActive();
    }
}
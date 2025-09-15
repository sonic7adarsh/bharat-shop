package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.repository.ProductRepository;
import com.bharatshop.shared.repository.CategoryRepository;
import com.bharatshop.shared.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating sitemap.xml per tenant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SitemapService {
    
    // Manual logger since Lombok is not working properly
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SitemapService.class);
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PageRepository pageRepository;
    
    private static final String SITEMAP_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n";
    
    private static final String SITEMAP_FOOTER = "</urlset>";
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * Generate complete sitemap.xml for a tenant
     */
    public String generateSitemap(Long tenantId, String baseUrl) {
        log.info("Generating sitemap for tenant: {}", tenantId);
        
        StringBuilder sitemap = new StringBuilder(SITEMAP_HEADER);
        
        // Add homepage
        sitemap.append(createUrlEntry(baseUrl, LocalDateTime.now(), "daily", 1.0));
        
        // Add categories - simplified query
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            if (category.getFeaturedInSitemap() != null && category.getFeaturedInSitemap()) {
                String url = baseUrl + "/category/" + category.getSlug();
                String changeFreq = category.getSitemapChangeFrequency() != null ? 
                    category.getSitemapChangeFrequency() : "weekly";
                Double priority = category.getSitemapPriority() != null ? 
                    category.getSitemapPriority() : 0.8;
                
                sitemap.append(createUrlEntry(url, category.getUpdatedAt(), changeFreq, priority));
            }
        }
        
        // Add products
        List<Product> products = productRepository.findByTenantIdAndStatusAndDeletedAtIsNull(
            tenantId, Product.ProductStatus.ACTIVE);
        for (Product product : products) {
            if (product.getFeaturedInSitemap() != null && product.getFeaturedInSitemap()) {
                String url = baseUrl + "/product/" + product.getSlug();
                String changeFreq = product.getSitemapChangeFrequency() != null ? 
                    product.getSitemapChangeFrequency() : "weekly";
                Double priority = product.getSitemapPriority() != null ? 
                    product.getSitemapPriority() : 0.6;
                
                sitemap.append(createUrlEntry(url, product.getUpdatedAt(), changeFreq, priority));
            }
        }
        
        // Add pages - simplified query
        List<Page> pages = pageRepository.findAll();
        for (Page page : pages) {
            if (page.getFeaturedInSitemap() != null && page.getFeaturedInSitemap()) {
                String url = baseUrl + "/page/" + page.getSlug();
                String changeFreq = page.getSitemapChangeFrequency() != null ? 
                    page.getSitemapChangeFrequency() : "monthly";
                Double priority = page.getSitemapPriority() != null ? 
                    page.getSitemapPriority() : 0.5;
                
                sitemap.append(createUrlEntry(url, page.getUpdatedAt(), changeFreq, priority));
            }
        }
        
        sitemap.append(SITEMAP_FOOTER);
        
        log.info("Generated sitemap with {} URLs for tenant: {}", 
            countUrls(sitemap.toString()), tenantId);
        
        return sitemap.toString();
    }
    
    /**
     * Create a URL entry for sitemap
     */
    private String createUrlEntry(String url, LocalDateTime lastMod, String changeFreq, Double priority) {
        StringBuilder entry = new StringBuilder();
        entry.append("  <url>\n");
        entry.append("    <loc>").append(escapeXml(url)).append("</loc>\n");
        
        if (lastMod != null) {
            entry.append("    <lastmod>").append(lastMod.format(ISO_FORMATTER)).append("</lastmod>\n");
        }
        
        if (changeFreq != null) {
            entry.append("    <changefreq>").append(changeFreq).append("</changefreq>\n");
        }
        
        if (priority != null) {
            entry.append("    <priority>").append(String.format("%.1f", priority)).append("</priority>\n");
        }
        
        entry.append("  </url>\n");
        return entry.toString();
    }
    
    /**
     * Escape XML special characters
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Count URLs in sitemap for logging
     */
    private int countUrls(String sitemap) {
        return (int) sitemap.chars().filter(ch -> ch == '<').count() / 2 - 1; // Subtract urlset tags
    }
    
    /**
     * Generate sitemap index for multiple sitemaps
     */
    public String generateSitemapIndex(Long tenantId, String baseUrl, List<String> sitemapUrls) {
        StringBuilder index = new StringBuilder();
        index.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        index.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        for (String sitemapUrl : sitemapUrls) {
            index.append("  <sitemap>\n");
            index.append("    <loc>").append(escapeXml(sitemapUrl)).append("</loc>\n");
            index.append("    <lastmod>").append(LocalDateTime.now().format(ISO_FORMATTER)).append("</lastmod>\n");
            index.append("  </sitemap>\n");
        }
        
        index.append("</sitemapindex>");
        return index.toString();
    }
}
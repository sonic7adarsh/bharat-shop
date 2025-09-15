package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.SeoMetadata;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.repository.SeoMetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing SEO metadata including OpenGraph and Twitter Cards
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeoMetadataService {
    
    private final SeoMetadataRepository seoMetadataRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Create or update SEO metadata for a product
     */
    public SeoMetadata createOrUpdateProductSeoMetadata(Product product, String baseUrl) {
        SeoMetadata seoMetadata = findOrCreateSeoMetadata("PRODUCT", product.getId(), product.getTenantId());
        
        // Basic SEO fields
        seoMetadata.setTitle(product.getMetaTitle() != null ? product.getMetaTitle() : product.getName());
        seoMetadata.setDescription(product.getMetaDescription() != null ? product.getMetaDescription() : product.getShortDescription());
        seoMetadata.setKeywords(product.getMetaKeywords());
        seoMetadata.setCanonicalUrl(baseUrl + "/product/" + product.getSlug());
        
        // OpenGraph metadata
        Map<String, Object> openGraph = new HashMap<>();
        openGraph.put("type", "product");
        openGraph.put("title", seoMetadata.getTitle());
        openGraph.put("description", seoMetadata.getDescription());
        openGraph.put("url", seoMetadata.getCanonicalUrl());
        
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            openGraph.put("image", product.getImages().get(0));
            openGraph.put("image:alt", product.getName());
        }
        
        // Product specific OpenGraph
        if (product.getEffectivePrice() != null) {
            openGraph.put("product:price:amount", product.getEffectivePrice());
            openGraph.put("product:price:currency", "INR");
        }
        
        // Set individual OpenGraph fields
        seoMetadata.setOgTitle((String) openGraph.get("title"));
        seoMetadata.setOgDescription((String) openGraph.get("description"));
        seoMetadata.setOgType((String) openGraph.get("type"));
        seoMetadata.setOgUrl((String) openGraph.get("url"));
        if (openGraph.get("image") != null) {
            seoMetadata.setOgImage((String) openGraph.get("image"));
        }
        
        // Twitter Card metadata
        Map<String, Object> twitterCard = new HashMap<>();
        twitterCard.put("card", "summary_large_image");
        twitterCard.put("title", seoMetadata.getTitle());
        twitterCard.put("description", seoMetadata.getDescription());
        
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            twitterCard.put("image", product.getImages().get(0));
        }
        
        // Set individual Twitter Card fields
        seoMetadata.setTwitterCard((String) twitterCard.get("card"));
        seoMetadata.setTwitterTitle((String) twitterCard.get("title"));
        seoMetadata.setTwitterDescription((String) twitterCard.get("description"));
        if (twitterCard.get("image") != null) {
            seoMetadata.setTwitterImage((String) twitterCard.get("image"));
        }
        
        return seoMetadataRepository.save(seoMetadata);
    }
    
    /**
     * Create or update SEO metadata for a category
     */
    public SeoMetadata createOrUpdateCategorySeoMetadata(Category category, String baseUrl) {
        SeoMetadata seoMetadata = findOrCreateSeoMetadata("CATEGORY", category.getId(), category.getTenantId());
        
        // Basic SEO fields
        seoMetadata.setTitle(category.getMetaTitle() != null ? category.getMetaTitle() : category.getName());
        seoMetadata.setDescription(category.getMetaDescription() != null ? category.getMetaDescription() : category.getShortDescription());
        seoMetadata.setKeywords(category.getMetaKeywords());
        seoMetadata.setCanonicalUrl(baseUrl + "/category/" + category.getSlug());
        
        // OpenGraph metadata
        Map<String, Object> openGraph = new HashMap<>();
        openGraph.put("type", "website");
        openGraph.put("title", seoMetadata.getTitle());
        openGraph.put("description", seoMetadata.getDescription());
        openGraph.put("url", seoMetadata.getCanonicalUrl());
        
        // Set individual OpenGraph fields
        seoMetadata.setOgTitle((String) openGraph.get("title"));
        seoMetadata.setOgDescription((String) openGraph.get("description"));
        seoMetadata.setOgType((String) openGraph.get("type"));
        seoMetadata.setOgUrl((String) openGraph.get("url"));
        
        // Twitter Card metadata
        Map<String, Object> twitterCard = new HashMap<>();
        twitterCard.put("card", "summary");
        twitterCard.put("title", seoMetadata.getTitle());
        twitterCard.put("description", seoMetadata.getDescription());
        
        // Set individual Twitter Card fields
        seoMetadata.setTwitterCard("summary");
        seoMetadata.setTwitterTitle((String) twitterCard.get("title"));
        seoMetadata.setTwitterDescription((String) twitterCard.get("description"));
        
        return seoMetadataRepository.save(seoMetadata);
    }
    
    /**
     * Create or update SEO metadata for a page
     */
    public SeoMetadata createOrUpdatePageSeoMetadata(Page page, String baseUrl) {
        SeoMetadata seoMetadata = findOrCreateSeoMetadata("PAGE", page.getId(), page.getTenantId());
        
        // Basic SEO fields
        seoMetadata.setTitle(page.getMetaTitle() != null ? page.getMetaTitle() : page.getTitle());
        seoMetadata.setDescription(page.getMetaDescription() != null ? page.getMetaDescription() : page.getExcerpt());
        seoMetadata.setKeywords(page.getMetaKeywords());
        seoMetadata.setCanonicalUrl(baseUrl + "/page/" + page.getSlug());
        
        // OpenGraph metadata
        Map<String, Object> openGraph = new HashMap<>();
        openGraph.put("type", "article");
        openGraph.put("title", seoMetadata.getTitle());
        openGraph.put("description", seoMetadata.getDescription());
        openGraph.put("url", seoMetadata.getCanonicalUrl());
        
        if (page.getFeaturedImage() != null) {
            openGraph.put("image", page.getFeaturedImage());
            openGraph.put("image:alt", page.getTitle());
        }
        
        if (page.getAuthor() != null) {
            openGraph.put("article:author", page.getAuthor());
        }
        
        if (page.getCreatedAt() != null) {
            openGraph.put("article:published_time", page.getCreatedAt().toString());
        }
        
        if (page.getUpdatedAt() != null) {
            openGraph.put("article:modified_time", page.getUpdatedAt().toString());
        }
        
        // Set individual OpenGraph fields
        seoMetadata.setOgTitle((String) openGraph.get("title"));
        seoMetadata.setOgDescription((String) openGraph.get("description"));
        seoMetadata.setOgType((String) openGraph.get("type"));
        seoMetadata.setOgUrl((String) openGraph.get("url"));
        
        // Twitter Card metadata
        Map<String, Object> twitterCard = new HashMap<>();
        twitterCard.put("card", page.getFeaturedImage() != null ? "summary_large_image" : "summary");
        twitterCard.put("title", seoMetadata.getTitle());
        twitterCard.put("description", seoMetadata.getDescription());
        
        if (page.getFeaturedImage() != null) {
            twitterCard.put("image", page.getFeaturedImage());
        }
        
        // Set individual Twitter Card fields
        seoMetadata.setTwitterCard((String) twitterCard.get("card"));
        seoMetadata.setTwitterTitle((String) twitterCard.get("title"));
        seoMetadata.setTwitterDescription((String) twitterCard.get("description"));
        if (twitterCard.get("image") != null) {
            seoMetadata.setTwitterImage((String) twitterCard.get("image"));
        }
        
        return seoMetadataRepository.save(seoMetadata);
    }
    
    /**
     * Find existing SEO metadata or create new one
     */
    private SeoMetadata findOrCreateSeoMetadata(String entityType, Long entityId, Long tenantId) {
        return seoMetadataRepository.findByEntityTypeAndEntityIdAndTenantId(entityType, entityId, tenantId)
                .orElse(SeoMetadata.builder()
                        .entityType(entityType)
                        .entityId(entityId)
                        .tenantId(tenantId)
                        .build());
    }
    
    /**
     * Get SEO metadata by entity
     */
    public Optional<SeoMetadata> getSeoMetadata(String entityType, Long entityId, Long tenantId) {
        return seoMetadataRepository.findByEntityTypeAndEntityIdAndTenantId(entityType, entityId, tenantId);
    }
    
    /**
     * Get all SEO metadata by tenant
     */
    public List<SeoMetadata> getAllSeoMetadataByTenant(Long tenantId) {
        return seoMetadataRepository.findByTenantId(tenantId);
    }
    
    /**
     * Delete SEO metadata
     */
    public void deleteSeoMetadata(String entityType, Long entityId, Long tenantId) {
        seoMetadataRepository.findByEntityTypeAndEntityIdAndTenantId(entityType, entityId, tenantId)
                .ifPresent(seoMetadataRepository::delete);
    }
    
    /**
     * Delete SEO metadata by ID
     */
    public void deleteSeoMetadata(Long id, Long tenantId) {
        seoMetadataRepository.findById(id)
                .filter(metadata -> metadata.getTenantId().equals(tenantId))
                .ifPresent(seoMetadataRepository::delete);
    }
    
    /**
     * Create SEO metadata for a product
     */
    public SeoMetadata createProductSeoMetadata(Long productId, Long tenantId, SeoMetadata seoMetadata) {
        seoMetadata.setEntityType("PRODUCT");
        seoMetadata.setEntityId(productId);
        seoMetadata.setTenantId(tenantId);
        return seoMetadataRepository.save(seoMetadata);
    }
    
    /**
     * Create SEO metadata for a category
     */
    public SeoMetadata createCategorySeoMetadata(Long categoryId, Long tenantId, SeoMetadata seoMetadata) {
        seoMetadata.setEntityType("CATEGORY");
        seoMetadata.setEntityId(categoryId);
        seoMetadata.setTenantId(tenantId);
        return seoMetadataRepository.save(seoMetadata);
    }
    
    /**
     * Create SEO metadata for a page
     */
    public SeoMetadata createPageSeoMetadata(Long pageId, Long tenantId, SeoMetadata seoMetadata) {
        seoMetadata.setEntityType("PAGE");
        seoMetadata.setEntityId(pageId);
        seoMetadata.setTenantId(tenantId);
        return seoMetadataRepository.save(seoMetadata);
    }
    
    /**
     * Update SEO metadata
     */
    public SeoMetadata updateSeoMetadata(Long id, Long tenantId, SeoMetadata seoMetadata) {
        SeoMetadata existing = seoMetadataRepository.findById(id)
                .filter(metadata -> metadata.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("SEO metadata not found"));
        
        // Update fields
        if (seoMetadata.getTitle() != null) existing.setTitle(seoMetadata.getTitle());
        if (seoMetadata.getDescription() != null) existing.setDescription(seoMetadata.getDescription());
        if (seoMetadata.getKeywords() != null) existing.setKeywords(seoMetadata.getKeywords());
        if (seoMetadata.getCanonicalUrl() != null) existing.setCanonicalUrl(seoMetadata.getCanonicalUrl());
        if (seoMetadata.getOgTitle() != null) existing.setOgTitle(seoMetadata.getOgTitle());
        if (seoMetadata.getOgDescription() != null) existing.setOgDescription(seoMetadata.getOgDescription());
        if (seoMetadata.getOgImage() != null) existing.setOgImage(seoMetadata.getOgImage());
        if (seoMetadata.getOgType() != null) existing.setOgType(seoMetadata.getOgType());
        if (seoMetadata.getOgUrl() != null) existing.setOgUrl(seoMetadata.getOgUrl());
        if (seoMetadata.getTwitterCard() != null) existing.setTwitterCard(seoMetadata.getTwitterCard());
        if (seoMetadata.getTwitterTitle() != null) existing.setTwitterTitle(seoMetadata.getTwitterTitle());
        if (seoMetadata.getTwitterDescription() != null) existing.setTwitterDescription(seoMetadata.getTwitterDescription());
        if (seoMetadata.getTwitterImage() != null) existing.setTwitterImage(seoMetadata.getTwitterImage());
        
        return seoMetadataRepository.save(existing);
    }
    
    /**
     * Generate meta tags HTML by entity type, ID, and tenant
     */
    public String generateMetaTagsHtml(String entityType, Long entityId, Long tenantId) {
        Optional<SeoMetadata> seoMetadataOpt = getSeoMetadata(entityType, entityId, tenantId);
        return seoMetadataOpt.map(this::generateMetaTagsHtml).orElse("");
    }
    
    /**
     * Generate meta tags HTML for a given SEO metadata
     */
    public String generateMetaTagsHtml(SeoMetadata seoMetadata) {
        if (seoMetadata == null) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        
        // Basic meta tags
        if (seoMetadata.getTitle() != null) {
            html.append("<title>").append(escapeHtml(seoMetadata.getTitle())).append("</title>\n");
            html.append("<meta property=\"og:title\" content=\"").append(escapeHtml(seoMetadata.getTitle())).append("\">\n");
        }
        
        if (seoMetadata.getDescription() != null) {
            html.append("<meta name=\"description\" content=\"").append(escapeHtml(seoMetadata.getDescription())).append("\">\n");
            html.append("<meta property=\"og:description\" content=\"").append(escapeHtml(seoMetadata.getDescription())).append("\">\n");
        }
        
        if (seoMetadata.getKeywords() != null) {
            html.append("<meta name=\"keywords\" content=\"").append(escapeHtml(seoMetadata.getKeywords())).append("\">\n");
        }
        
        if (seoMetadata.getCanonicalUrl() != null) {
            html.append("<link rel=\"canonical\" href=\"").append(escapeHtml(seoMetadata.getCanonicalUrl())).append("\">\n");
            html.append("<meta property=\"og:url\" content=\"").append(escapeHtml(seoMetadata.getCanonicalUrl())).append("\">\n");
        }
        
        // Add OpenGraph data from individual fields
        if (seoMetadata.getOgTitle() != null) {
            html.append("<meta property=\"og:title\" content=\"").append(escapeHtml(seoMetadata.getOgTitle())).append("\">\n");
        }
        if (seoMetadata.getOgDescription() != null) {
            html.append("<meta property=\"og:description\" content=\"").append(escapeHtml(seoMetadata.getOgDescription())).append("\">\n");
        }
        if (seoMetadata.getOgImage() != null) {
            html.append("<meta property=\"og:image\" content=\"").append(escapeHtml(seoMetadata.getOgImage())).append("\">\n");
        }
        if (seoMetadata.getOgType() != null) {
            html.append("<meta property=\"og:type\" content=\"").append(escapeHtml(seoMetadata.getOgType())).append("\">\n");
        }
        if (seoMetadata.getOgUrl() != null) {
            html.append("<meta property=\"og:url\" content=\"").append(escapeHtml(seoMetadata.getOgUrl())).append("\">\n");
        }
        
        // Add Twitter Card data from individual fields
        if (seoMetadata.getTwitterCard() != null) {
            html.append("<meta name=\"twitter:card\" content=\"").append(escapeHtml(seoMetadata.getTwitterCard())).append("\">\n");
        }
        if (seoMetadata.getTwitterTitle() != null) {
            html.append("<meta name=\"twitter:title\" content=\"").append(escapeHtml(seoMetadata.getTwitterTitle())).append("\">\n");
        }
        if (seoMetadata.getTwitterDescription() != null) {
            html.append("<meta name=\"twitter:description\" content=\"").append(escapeHtml(seoMetadata.getTwitterDescription())).append("\">\n");
        }
        if (seoMetadata.getTwitterImage() != null) {
            html.append("<meta name=\"twitter:image\" content=\"").append(escapeHtml(seoMetadata.getTwitterImage())).append("\">\n");
        }
        if (seoMetadata.getTwitterSite() != null) {
            html.append("<meta name=\"twitter:site\" content=\"").append(escapeHtml(seoMetadata.getTwitterSite())).append("\">\n");
        }
        
        return html.toString();
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
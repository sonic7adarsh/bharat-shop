package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.Category;
import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for generating structured data (JSON-LD) for SEO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructuredDataService {
    
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Generate Product structured data (JSON-LD)
     */
    public String generateProductStructuredData(Product product, String baseUrl) {
        try {
            Map<String, Object> productSchema = new HashMap<>();
            productSchema.put("@context", "https://schema.org/");
            productSchema.put("@type", "Product");
            productSchema.put("name", product.getName());
            productSchema.put("description", product.getShortDescription() != null ? product.getShortDescription() : product.getDescription());
            productSchema.put("url", baseUrl + "/product/" + product.getSlug());
            
            // Use product ID as identifier since SKU is not available
            productSchema.put("identifier", product.getId().toString());
            
            // Images
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                if (product.getImages().size() == 1) {
                    productSchema.put("image", product.getImages().get(0));
                } else {
                    productSchema.put("image", product.getImages());
                }
            }
            
            // Brand (assuming tenant name as brand)
            Map<String, Object> brand = new HashMap<>();
            brand.put("@type", "Brand");
            brand.put("name", "BharatShop"); // This should be dynamic based on tenant
            productSchema.put("brand", brand);
            
            // Offers
            if (product.getEffectivePrice() != null) {
                Map<String, Object> offer = new HashMap<>();
                offer.put("@type", "Offer");
                offer.put("url", baseUrl + "/product/" + product.getSlug());
                offer.put("priceCurrency", "INR");
                offer.put("price", product.getEffectivePrice());
                offer.put("priceValidUntil", "2024-12-31"); // Should be dynamic
                
                // Availability
                if (product.getTotalStock() != null && product.getTotalStock() > 0) {
                    offer.put("availability", "https://schema.org/InStock");
                } else {
                    offer.put("availability", "https://schema.org/OutOfStock");
                }
                
                // Seller
                Map<String, Object> seller = new HashMap<>();
                seller.put("@type", "Organization");
                seller.put("name", "BharatShop");
                offer.put("seller", seller);
                
                productSchema.put("offers", offer);
            }
            
            // Category
            if (product.getCategories() != null && !product.getCategories().isEmpty()) {
                Long categoryId = product.getCategories().get(0);
                Category category = categoryRepository.findById(categoryId).orElse(null);
                if (category != null) {
                    productSchema.put("category", category.getName());
                }
            }
            
            // Aggregate Rating (placeholder - should be calculated from reviews)
            Map<String, Object> aggregateRating = new HashMap<>();
            aggregateRating.put("@type", "AggregateRating");
            aggregateRating.put("ratingValue", "4.5");
            aggregateRating.put("reviewCount", "10");
            productSchema.put("aggregateRating", aggregateRating);
            
            return objectMapper.writeValueAsString(productSchema);
            
        } catch (Exception e) {
            System.out.println("Error generating product structured data for product " + product.getId() + ": " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Generate BreadcrumbList structured data (JSON-LD)
     */
    public String generateBreadcrumbStructuredData(List<BreadcrumbItem> breadcrumbs, String baseUrl) {
        try {
            Map<String, Object> breadcrumbSchema = new HashMap<>();
            breadcrumbSchema.put("@context", "https://schema.org/");
            breadcrumbSchema.put("@type", "BreadcrumbList");
            
            List<Map<String, Object>> itemListElements = new ArrayList<>();
            
            for (int i = 0; i < breadcrumbs.size(); i++) {
                BreadcrumbItem item = breadcrumbs.get(i);
                Map<String, Object> listItem = new HashMap<>();
                listItem.put("@type", "ListItem");
                listItem.put("position", i + 1);
                listItem.put("name", item.getName());
                listItem.put("item", baseUrl + item.getUrl());
                
                itemListElements.add(listItem);
            }
            
            breadcrumbSchema.put("itemListElement", itemListElements);
            
            return objectMapper.writeValueAsString(breadcrumbSchema);
            
        } catch (Exception e) {
            System.out.println("Error generating breadcrumb structured data: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Generate Organization structured data (JSON-LD)
     */
    public String generateOrganizationStructuredData(String organizationName, String baseUrl, 
                                                   String logo, String contactPhone, String contactEmail) {
        try {
            Map<String, Object> orgSchema = new HashMap<>();
            orgSchema.put("@context", "https://schema.org/");
            orgSchema.put("@type", "Organization");
            orgSchema.put("name", organizationName);
            orgSchema.put("url", baseUrl);
            
            if (logo != null) {
                orgSchema.put("logo", logo);
            }
            
            // Contact Point
            if (contactPhone != null || contactEmail != null) {
                Map<String, Object> contactPoint = new HashMap<>();
                contactPoint.put("@type", "ContactPoint");
                contactPoint.put("contactType", "customer service");
                
                if (contactPhone != null) {
                    contactPoint.put("telephone", contactPhone);
                }
                
                if (contactEmail != null) {
                    contactPoint.put("email", contactEmail);
                }
                
                orgSchema.put("contactPoint", contactPoint);
            }
            
            return objectMapper.writeValueAsString(orgSchema);
            
        } catch (Exception e) {
            System.out.println("Error generating organization structured data: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Generate WebSite structured data with search action (JSON-LD)
     */
    public String generateWebSiteStructuredData(String siteName, String baseUrl) {
        try {
            Map<String, Object> websiteSchema = new HashMap<>();
            websiteSchema.put("@context", "https://schema.org/");
            websiteSchema.put("@type", "WebSite");
            websiteSchema.put("name", siteName);
            websiteSchema.put("url", baseUrl);
            
            // Search Action
            Map<String, Object> searchAction = new HashMap<>();
            searchAction.put("@type", "SearchAction");
            searchAction.put("target", baseUrl + "/search?q={search_term_string}");
            searchAction.put("query-input", "required name=search_term_string");
            
            websiteSchema.put("potentialAction", searchAction);
            
            return objectMapper.writeValueAsString(websiteSchema);
            
        } catch (Exception e) {
            System.out.println("Error generating website structured data: " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Generate Article structured data for pages (JSON-LD)
     */
    public String generateArticleStructuredData(Page page, String baseUrl) {
        try {
            Map<String, Object> articleSchema = new HashMap<>();
            articleSchema.put("@context", "https://schema.org/");
            articleSchema.put("@type", "Article");
            articleSchema.put("headline", page.getTitle());
            articleSchema.put("description", page.getExcerpt() != null ? page.getExcerpt() : page.getMetaDescription());
            articleSchema.put("url", baseUrl + "/page/" + page.getSlug());
            
            if (page.getFeaturedImage() != null) {
                articleSchema.put("image", page.getFeaturedImage());
            }
            
            if (page.getAuthor() != null) {
                Map<String, Object> author = new HashMap<>();
                author.put("@type", "Person");
                author.put("name", page.getAuthor());
                articleSchema.put("author", author);
            }
            
            if (page.getCreatedAt() != null) {
                articleSchema.put("datePublished", page.getCreatedAt().toString());
            }
            
            if (page.getUpdatedAt() != null) {
                articleSchema.put("dateModified", page.getUpdatedAt().toString());
            }
            
            // Publisher
            Map<String, Object> publisher = new HashMap<>();
            publisher.put("@type", "Organization");
            publisher.put("name", "BharatShop");
            publisher.put("url", baseUrl);
            articleSchema.put("publisher", publisher);
            
            return objectMapper.writeValueAsString(articleSchema);
            
        } catch (Exception e) {
            System.out.println("Error generating article structured data for page " + page.getId() + ": " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Generate combined structured data script tag
     */
    public String generateStructuredDataScript(List<String> structuredDataJsons) {
        if (structuredDataJsons == null || structuredDataJsons.isEmpty()) {
            return "";
        }
        
        StringBuilder script = new StringBuilder();
        script.append("<script type=\"application/ld+json\">\n");
        
        if (structuredDataJsons.size() == 1) {
            script.append(structuredDataJsons.get(0));
        } else {
            script.append("[\n");
            for (int i = 0; i < structuredDataJsons.size(); i++) {
                script.append(structuredDataJsons.get(i));
                if (i < structuredDataJsons.size() - 1) {
                    script.append(",\n");
                }
            }
            script.append("\n]");
        }
        
        script.append("\n</script>");
        
        return script.toString();
    }
    
    /**
     * Generate breadcrumb items for a product
     */
    public List<BreadcrumbItem> generateProductBreadcrumbs(Product product) {
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        
        // Home
        breadcrumbs.add(new BreadcrumbItem("Home", "/"));
        
        // Categories
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            Long categoryId = product.getCategories().get(0);
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                // Build category hierarchy
                List<Category> categoryHierarchy = buildCategoryHierarchy(category);
                for (Category cat : categoryHierarchy) {
                    breadcrumbs.add(new BreadcrumbItem(cat.getName(), "/category/" + cat.getSlug()));
                }
            }
        }
        
        // Product
        breadcrumbs.add(new BreadcrumbItem(product.getName(), "/product/" + product.getSlug()));
        
        return breadcrumbs;
    }
    
    /**
     * Generate breadcrumb items for a category
     */
    public List<BreadcrumbItem> generateCategoryBreadcrumbs(Category category) {
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        
        // Home
        breadcrumbs.add(new BreadcrumbItem("Home", "/"));
        
        // Category hierarchy
        List<Category> categoryHierarchy = buildCategoryHierarchy(category);
        for (Category cat : categoryHierarchy) {
            breadcrumbs.add(new BreadcrumbItem(cat.getName(), "/category/" + cat.getSlug()));
        }
        
        return breadcrumbs;
    }
    
    /**
     * Build category hierarchy from leaf to root
     */
    private List<Category> buildCategoryHierarchy(Category category) {
        List<Category> hierarchy = new ArrayList<>();
        Category current = category;
        
        while (current != null) {
            hierarchy.add(0, current); // Add at beginning to maintain order
            
            if (current.getParentId() != null) {
                current = categoryRepository.findById(current.getParentId()).orElse(null);
            } else {
                current = null;
            }
        }
        
        return hierarchy;
    }
    
    /**
     * Breadcrumb item class
     */
    public static class BreadcrumbItem {
        private String name;
        private String url;
        
        public BreadcrumbItem(String name, String url) {
            this.name = name;
            this.url = url;
        }
        
        public String getName() { return name; }
        public String getUrl() { return url; }
    }
}
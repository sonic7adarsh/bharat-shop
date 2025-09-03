package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.ProductResponseDto;
import com.bharatshop.storefront.model.Product;
import com.bharatshop.storefront.repository.StorefrontProductRepository;
import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.dto.ProductOptionDto;
import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.ProductOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer-focused product service for storefront operations
 * Provides business logic specifically tailored for customer-facing product browsing
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontProductService {
    
    private static final Logger log = LoggerFactory.getLogger(StorefrontProductService.class);
    
    @Qualifier("storefrontProductRepository")
    private final StorefrontProductRepository productRepository;
    private final ProductVariantService productVariantService;
    private final ProductOptionService productOptionService;
    
    /**
     * Get products with customer-focused filtering and sorting
     * Prioritizes featured products and applies customer-friendly sorting
     */
    @Cacheable(value = "storefront:products", key = "#category + '_' + #search + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerProducts(String category, String search, Double minPrice, Double maxPrice, Pageable pageable) {
        log.debug("Fetching customer products with filters - category: {}, search: {}, minPrice: {}, maxPrice: {}", 
                category, search, minPrice, maxPrice);
        
        Page<Product> products = productRepository.findActiveProductsWithFilters(category, search, minPrice, maxPrice, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Get product by slug with customer-specific data enrichment
     */
    @Cacheable(value = "storefront:product", key = "'slug_' + #slug")
    public ProductResponseDto getCustomerProductBySlug(String slug) {
        log.debug("Fetching customer product with slug: {}", slug);
        
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        
        return mapToCustomerResponseDto(product);
    }
    
    /**
     * Advanced search with customer-focused relevance scoring
     */
    @Cacheable(value = "storefront:search", key = "#query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchCustomerProducts(String query, Pageable pageable) {
        log.debug("Customer search for query: {}", query);
        
        Page<Product> products = productRepository.searchProducts(query, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Search products within a specific category for customers
     */
    @Cacheable(value = "storefront:categorySearch", key = "#query + '_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchCustomerProductsByCategory(String query, String category, Pageable pageable) {
        log.debug("Customer search for query: {} in category: {}", query, category);
        
        Page<Product> products = productRepository.searchProductsByCategory(query, category, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Get products by brand with optional search
     */
    @Cacheable(value = "storefront:brandProducts", key = "#brand + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerProductsByBrand(String brand, String query, Pageable pageable) {
        log.debug("Customer products by brand: {} with query: {}", brand, query);
        
        Page<Product> products = productRepository.findByBrandAndSearch(brand, query, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Get products within price range with search
     */
    @Cacheable(value = "storefront:priceRangeProducts", key = "#minPrice + '_' + #maxPrice + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerProductsByPriceRange(Double minPrice, Double maxPrice, String query, Pageable pageable) {
        log.debug("Customer products in price range: {}-{} with query: {}", minPrice, maxPrice, query);
        
        Page<Product> products = productRepository.findByPriceRangeAndSearch(minPrice, maxPrice, query, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Get featured products for customer homepage
     */
    @Cacheable(value = "storefront:featuredProducts")
    public List<ProductResponseDto> getCustomerFeaturedProducts() {
        log.debug("Fetching customer featured products");
        
        List<Product> products = productRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();
        return products.stream()
                .map(this::mapToCustomerResponseDto)
                .toList();
    }
    
    /**
     * Get top-rated products for customers
     */
    @Cacheable(value = "storefront:topRatedProducts", key = "#minRating + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerTopRatedProducts(Double minRating, Pageable pageable) {
        log.debug("Fetching customer top-rated products with minimum rating: {}", minRating);
        
        Page<Product> products = productRepository.findByMinRating(minRating, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Get available categories for customer filtering
     */
    @Cacheable(value = "storefront:categories")
    public List<String> getCustomerCategories() {
        log.debug("Fetching customer categories");
        return productRepository.findDistinctCategories();
    }
    
    /**
     * Get available brands for customer filtering
     */
    @Cacheable(value = "storefront:brands")
    public List<String> getCustomerBrands() {
        log.debug("Fetching customer brands");
        return productRepository.findDistinctBrands();
    }
    
    /**
     * Get discounted products for customer promotions
     */
    @Cacheable(value = "storefront:discountedProducts", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerDiscountedProducts(Pageable pageable) {
        log.debug("Fetching customer discounted products");
        
        Page<Product> products = productRepository.findDiscountedProducts(pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Get in-stock products for customers
     */
    @Cacheable(value = "storefront:inStockProducts", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerInStockProducts(Pageable pageable) {
        log.debug("Fetching customer in-stock products");
        
        Page<Product> products = productRepository.findInStockProducts(pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Check product availability for customers
     */
    public boolean isProductAvailableForCustomer(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug).isPresent();
    }
    
    /**
     * Get product recommendations based on category
     */
    @Cacheable(value = "storefront:recommendations", key = "#category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerRecommendations(String category, Pageable pageable) {
        log.debug("Fetching customer recommendations for category: {}", category);
        
        Page<Product> products = productRepository.findByCategoryAndActiveTrueOrderByCreatedAtDesc(category, pageable);
        return products.map(this::mapToCustomerResponseDto);
    }
    
    /**
     * Map Product entity to customer-focused response DTO
     * Includes customer-specific data formatting and calculations
     */
    private ProductResponseDto mapToCustomerResponseDto(Product product) {
        // Get product variants
        List<ProductVariantDto> variants = productVariantService.getVariantsByProduct(product.getId(), product.getTenantId());
        
        // Get default variant (first variant or null if no variants)
        ProductVariantDto defaultVariant = variants.isEmpty() ? null : 
            variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(variants.get(0));
        
        // Get product options
        List<ProductOptionDto> options = productOptionService.getProductOptions(product.getId(), product.getTenantId());
        
        // Use variant data for price and stock if available, otherwise use product data
        ProductResponseDto.ProductResponseDtoBuilder builder = ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .imageUrls(product.getImageUrls())
                .featured(product.getFeatured())
                .active(product.getActive())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .slug(product.getSlug())
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .defaultVariant(defaultVariant)
                .variants(variants)
                .options(options)
                .hasVariants(!variants.isEmpty());
        
        // Use variant data if available, otherwise fallback to product data
        if (defaultVariant != null) {
            builder.price(defaultVariant.getPrice())
                   .discountPrice(defaultVariant.getDiscountAmount())
                   .sku(defaultVariant.getSku())
                   .stockQuantity(defaultVariant.getStock());
        } else {
            builder.price(product.getPrice())
                   .discountPrice(product.getDiscountPrice())
                   .sku(product.getSku())
                   .stockQuantity(product.getStockQuantity());
        }
        
        return builder.build();
    }
}
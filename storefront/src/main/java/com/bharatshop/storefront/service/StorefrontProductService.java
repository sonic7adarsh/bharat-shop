package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.ProductResponseDto;
import com.bharatshop.shared.entity.Product;
import com.bharatshop.storefront.repository.StorefrontProductRepository;
import com.bharatshop.shared.dto.ProductVariantDto;
import com.bharatshop.shared.dto.ProductOptionDto;
import com.bharatshop.platform.service.ProductVariantService;
import com.bharatshop.platform.service.ProductOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

/**
 * Storefront Product Service for storefront operations
 * Combines functionality from both ProductService and StorefrontProductService
 * Provides comprehensive product management for customer-facing operations
 */
@Service("storefrontProductServiceBean")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StorefrontProductService {
    
    private static final Logger log = LoggerFactory.getLogger(StorefrontProductService.class);
    
    private final StorefrontProductRepository productRepository;
    private final ProductVariantService productVariantService;
    private final ProductOptionService productOptionService;
    
    // ========== Core Product Operations ==========
    
    @Cacheable(value = "products", key = "#category + '_' + #search + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getAllProducts(String category, String search, Double minPrice, Double maxPrice, Pageable pageable) {
        log.debug("Fetching products with filters - category: {}, search: {}, minPrice: {}, maxPrice: {}", 
                category, search, minPrice, maxPrice);
        
        Page<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "product", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        
        Product product = productRepository.findByIdAndStatus(id, Product.ProductStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        return mapToResponseDto(product);
    }
    
    @Cacheable(value = "product", key = "'slug_' + #slug")
    public ProductResponseDto getProductBySlug(String slug) {
        log.debug("Fetching product with slug: {}", slug);
        
        Product product = productRepository.findBySlugAndStatus(slug, Product.ProductStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        
        return mapToResponseDto(product);
    }
    
    // ========== Customer-Focused Operations ==========
    
    @Cacheable(value = "storefront:products", key = "#category + '_' + #search + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerProducts(String category, String search, Double minPrice, Double maxPrice, Pageable pageable) {
        log.debug("Fetching customer products with filters - category: {}, search: {}, minPrice: {}, maxPrice: {}", 
                category, search, minPrice, maxPrice);
        
        Page<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:product", key = "'slug_' + #slug")
    public ProductResponseDto getCustomerProductBySlug(String slug) {
        log.debug("Fetching customer product with slug: {}", slug);
        
        Product product = productRepository.findBySlugAndStatus(slug, Product.ProductStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        
        return mapToResponseDto(product);
    }
    
    // ========== Search Operations ==========
    
    public Page<ProductResponseDto> searchProducts(String query, Pageable pageable) {
        log.debug("Searching products with query: {}", query);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:search", key = "#query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchCustomerProducts(String query, Pageable pageable) {
        log.debug("Customer search for query: {}", query);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "productsByCategory", key = "#query + '_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchProductsByCategory(String query, String category, Pageable pageable) {
        log.debug("Searching products with query: {} in category: {}", query, category);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:categorySearch", key = "#query + '_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchCustomerProductsByCategory(String query, String category, Pageable pageable) {
        log.debug("Customer search for query: {} in category: {}", query, category);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    // ========== Brand Operations ==========
    
    @Cacheable(value = "productsByBrand", key = "#brand + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchProductsByBrand(String brand, String query, Pageable pageable) {
        log.debug("Searching products by brand: {} with query: {}", brand, query);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:brandProducts", key = "#brand + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerProductsByBrand(String brand, String query, Pageable pageable) {
        log.debug("Customer products by brand: {} with query: {}", brand, query);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "brands")
    public List<String> getBrands() {
        log.debug("Fetching all distinct brands");
        return new ArrayList<>();
    }
    
    @Cacheable(value = "storefront:brands")
    public List<String> getCustomerBrands() {
        log.debug("Fetching customer brands");
        return new ArrayList<>();
    }
    
    // ========== Price Range Operations ==========
    
    @Cacheable(value = "productsByPriceRange", key = "#minPrice + '_' + #maxPrice + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchProductsByPriceRange(Double minPrice, Double maxPrice, String query, Pageable pageable) {
        log.debug("Searching products in price range: {}-{} with query: {}", minPrice, maxPrice, query);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:priceRangeProducts", key = "#minPrice + '_' + #maxPrice + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerProductsByPriceRange(Double minPrice, Double maxPrice, String query, Pageable pageable) {
        log.debug("Customer products in price range: {}-{} with query: {}", minPrice, maxPrice, query);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    // ========== Featured Products ==========
    
    @Cacheable(value = "featuredProducts")
    public List<ProductResponseDto> getFeaturedProducts() {
        log.debug("Fetching featured products");
        
        List<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(Product.ProductStatus.ACTIVE);
        return products.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "storefront:featuredProducts")
    public List<ProductResponseDto> getCustomerFeaturedProducts() {
        log.debug("Fetching customer featured products");
        
        List<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(Product.ProductStatus.ACTIVE);
        return products.stream()
                .map(this::mapToResponseDto)
                .toList();
    }
    
    // ========== Rating Operations ==========
    
    @Cacheable(value = "productsByRating", key = "#minRating + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getProductsByMinRating(Double minRating, Pageable pageable) {
        log.debug("Fetching products with minimum rating: {}", minRating);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:topRatedProducts", key = "#minRating + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerTopRatedProducts(Double minRating, Pageable pageable) {
        log.debug("Fetching customer top-rated products with minimum rating: {}", minRating);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    // ========== Category Operations ==========
    
    @Cacheable(value = "categories")
    public List<Long> getCategories() {
        log.debug("Fetching all product categories");
        return List.of();
    }
    
    @Cacheable(value = "storefront:categories")
    public List<Long> getCustomerCategories() {
        log.debug("Fetching customer categories");
        return List.of();
    }
    
    // ========== Additional Customer Operations ==========
    
    @Cacheable(value = "storefront:discountedProducts", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerDiscountedProducts(Pageable pageable) {
        log.debug("Fetching customer discounted products");
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "storefront:inStockProducts", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerInStockProducts(Pageable pageable) {
        log.debug("Fetching customer in-stock products");
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    public boolean isProductAvailableForCustomer(String slug) {
        return productRepository.findBySlugAndStatus(slug, Product.ProductStatus.ACTIVE).isPresent();
    }
    
    @Cacheable(value = "storefront:recommendations", key = "#category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getCustomerRecommendations(String category, Pageable pageable) {
        log.debug("Fetching customer recommendations for category: {}", category);
        
        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    // ========== Mapping Methods ==========
    
    private ProductResponseDto mapToResponseDto(Product product) {
        // Get product variants
        List<ProductVariantDto> variants = productVariantService.getVariantsByProduct(product.getId(), Long.parseLong(product.getTenantId()));
        
        // Get default variant (first variant or null if no variants)
        ProductVariantDto defaultVariant = variants.isEmpty() ? null : 
            variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(variants.get(0));
        
        // Get product options
        List<ProductOptionDto> options = productOptionService.getProductOptions(product.getId(), Long.parseLong(product.getTenantId()));
        
        // Use variant data for price and stock if available, otherwise use product data
        ProductResponseDto.ProductResponseDtoBuilder builder = ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(null) // Category will be handled by categories list
                .brand(null) // Brand not available in shared Product
                .imageUrls(product.getImages())
                .featured(false) // Default value
                .active(product.getStatus() == Product.ProductStatus.ACTIVE)
                .rating(java.math.BigDecimal.ZERO) // Default rating
                .reviewCount(0) // Default review count
                .slug(product.getSlug())
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
                   .discountPrice(null) // No discount price in shared Product
                   .sku(null) // No SKU in shared Product
                   .stockQuantity(product.getStock());
        }
        
        return builder.build();
    }
}
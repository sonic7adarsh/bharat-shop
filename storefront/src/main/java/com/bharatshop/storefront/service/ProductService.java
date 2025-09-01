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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {
    
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    @Qualifier("storefrontProductRepository")
    private final StorefrontProductRepository productRepository;
    private final ProductVariantService productVariantService;
    private final ProductOptionService productOptionService;
    
    @Cacheable(value = "products", key = "#category + '_' + #search + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getAllProducts(String category, String search, Double minPrice, Double maxPrice, Pageable pageable) {
        log.debug("Fetching products with filters - category: {}, search: {}, minPrice: {}, maxPrice: {}", 
                category, search, minPrice, maxPrice);
        
        Page<Product> products = productRepository.findActiveProductsWithFilters(category, search, minPrice, maxPrice, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "product", key = "#id")
    public ProductResponseDto getProductById(UUID id) {
        log.debug("Fetching product with id: {}", id);
        
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        return mapToResponseDto(product);
    }
    
    @Cacheable(value = "product", key = "'slug_' + #slug")
    public ProductResponseDto getProductBySlug(String slug) {
        log.debug("Fetching product with slug: {}", slug);
        
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new RuntimeException("Product not found with slug: " + slug));
        
        return mapToResponseDto(product);
    }
    
    @Cacheable(value = "featuredProducts")
    public List<ProductResponseDto> getFeaturedProducts() {
        log.debug("Fetching featured products");
        
        List<Product> products = productRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();
        return products.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "categories")
    public List<String> getCategories() {
        log.debug("Fetching all product categories");
        
        return productRepository.findDistinctCategories();
    }
    
    public Page<ProductResponseDto> searchProducts(String query, Pageable pageable) {
        log.debug("Searching products with query: {}", query);
        
        Page<Product> products = productRepository.searchProducts(query, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "productsByCategory", key = "#query + '_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchProductsByCategory(String query, String category, Pageable pageable) {
        log.debug("Searching products with query: {} in category: {}", query, category);
        
        Page<Product> products = productRepository.searchProductsByCategory(query, category, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "productsByBrand", key = "#brand + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchProductsByBrand(String brand, String query, Pageable pageable) {
        log.debug("Searching products by brand: {} with query: {}", brand, query);
        
        Page<Product> products = productRepository.findByBrandAndSearch(brand, query, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "productsByPriceRange", key = "#minPrice + '_' + #maxPrice + '_' + #query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> searchProductsByPriceRange(Double minPrice, Double maxPrice, String query, Pageable pageable) {
        log.debug("Searching products in price range: {}-{} with query: {}", minPrice, maxPrice, query);
        
        Page<Product> products = productRepository.findByPriceRangeAndSearch(minPrice, maxPrice, query, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    @Cacheable(value = "brands")
    public List<String> getBrands() {
        log.debug("Fetching all distinct brands");
        return productRepository.findDistinctBrands();
    }
    
    @Cacheable(value = "productsByRating", key = "#minRating + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponseDto> getProductsByMinRating(Double minRating, Pageable pageable) {
        log.debug("Fetching products with minimum rating: {}", minRating);
        
        Page<Product> products = productRepository.findByMinRating(minRating, pageable);
        return products.map(this::mapToResponseDto);
    }
    
    private ProductResponseDto mapToResponseDto(Product product) {
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
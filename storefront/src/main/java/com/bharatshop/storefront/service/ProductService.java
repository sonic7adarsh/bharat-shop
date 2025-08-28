package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.ProductResponseDto;
import com.bharatshop.storefront.model.Product;
import com.bharatshop.storefront.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ProductRepository productRepository;
    
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
    
    private ProductResponseDto mapToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .category(product.getCategory())
                .brand(product.getBrand())
                .sku(product.getSku())
                .stockQuantity(product.getStockQuantity())
                .imageUrls(product.getImageUrls())
                .featured(product.getFeatured())
                .active(product.getActive())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
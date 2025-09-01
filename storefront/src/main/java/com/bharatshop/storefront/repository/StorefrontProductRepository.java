package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.model.Product;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("storefrontProductRepository")
public interface StorefrontProductRepository extends TenantAwareRepository<Product> {
    
    Optional<Product> findByIdAndActiveTrue(UUID id);
    
    List<Product> findByActiveTrueOrderByCreatedAtDesc();
    
    List<Product> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();
    
    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    Page<Product> findByCategoryAndActiveTrueOrderByCreatedAtDesc(String category, Pageable pageable);
    
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true ORDER BY p.category")
    List<String> findDistinctCategories();
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "ORDER BY " +
           "CASE WHEN :search IS NULL THEN 0 " +
           "     WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "     WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "     WHEN LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')) THEN 3 " +
           "     WHEN LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) THEN 4 " +
           "     ELSE 5 END, " +
           "p.featured DESC, p.createdAt DESC")
    Page<Product> findActiveProductsWithFilters(
            @Param("category") String category,
            @Param("search") String search,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:query, '%')) THEN 1 " +
           "     WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 2 " +
           "     WHEN LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) THEN 3 " +
           "     WHEN LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) THEN 4 " +
           "     WHEN LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) THEN 5 " +
           "     ELSE 6 END, " +
           "p.featured DESC, p.rating DESC, p.createdAt DESC")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
    
    boolean existsBySkuAndActiveTrue(String sku);
    
    Optional<Product> findBySkuAndActiveTrue(String sku);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity > 0 ORDER BY p.createdAt DESC")
    Page<Product> findInStockProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.discountPrice IS NOT NULL ORDER BY p.createdAt DESC")
    Page<Product> findDiscountedProducts(Pageable pageable);
    
    Optional<Product> findBySlugAndActiveTrue(String slug);
    
    // Advanced search methods for better full-text search
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND p.category = :category " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:query, '%')) THEN 1 " +
           "     WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 2 " +
           "     ELSE 3 END, " +
           "p.featured DESC, p.rating DESC")
    Page<Product> searchProductsByCategory(@Param("query") String query, @Param("category") String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND p.brand = :brand " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.featured DESC, p.rating DESC, p.createdAt DESC")
    Page<Product> findByBrandAndSearch(@Param("brand") String brand, @Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND p.price BETWEEN :minPrice AND :maxPrice " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.featured DESC, p.rating DESC, p.createdAt DESC")
    Page<Product> findByPriceRangeAndSearch(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice, @Param("query") String query, Pageable pageable);
    
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND p.rating >= :minRating " +
           "ORDER BY p.rating DESC, p.reviewCount DESC, p.createdAt DESC")
    Page<Product> findByMinRating(@Param("minRating") Double minRating, Pageable pageable);
}
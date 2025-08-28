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

@Repository
public interface ProductRepository extends TenantAwareRepository<Product> {
    
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
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "ORDER BY p.createdAt DESC")
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
           "     OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
    
    boolean existsBySkuAndActiveTrue(String sku);
    
    Optional<Product> findBySkuAndActiveTrue(String sku);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity > 0 ORDER BY p.createdAt DESC")
    Page<Product> findInStockProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.discountPrice IS NOT NULL ORDER BY p.createdAt DESC")
    Page<Product> findDiscountedProducts(Pageable pageable);
}
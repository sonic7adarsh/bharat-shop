package com.bharatshop.storefront.repository;

import com.bharatshop.shared.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorefrontProductRepository extends JpaRepository<Product, Long> {
    
    // Using method name-based queries to avoid HQL validation issues
    Optional<Product> findByIdAndStatus(Long id, Product.ProductStatus status);

    List<Product> findByStatusOrderByCreatedAtDesc(Product.ProductStatus status);

    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);
    
    Page<Product> findByStatusOrderByCreatedAtDesc(Product.ProductStatus status, Pageable pageable);
    

    
    // Use existing method instead of problematic method names
    // findInStockProducts and findDiscountedProducts will use findByStatus
    
    // Simplified method name-based queries to avoid HQL validation issues
    Optional<Product> findBySlugAndStatus(String slug, Product.ProductStatus status);
}
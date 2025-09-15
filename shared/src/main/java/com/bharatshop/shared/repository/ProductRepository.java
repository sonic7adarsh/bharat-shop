package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByTenantIdAndDeletedAtIsNull(Long tenantId);

    Page<Product> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    Optional<Product> findBySlugAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);

    List<Product> findByTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, Product.ProductStatus status);

    Page<Product> findByTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, Product.ProductStatus status, Pageable pageable);

    // Simplified method name-based queries to avoid HQL validation issues
    long countByTenantIdAndDeletedAtIsNull(Long tenantId);
    
    long countByTenantIdAndStatus(Long tenantId, Product.ProductStatus status);
    
    // Add back countByTenantId since it's used by services
    long countByTenantId(Long tenantId);
    
    // SEO-related methods for sitemap generation
    List<Product> findByTenantIdAndFeaturedInSitemapTrueAndDeletedAtIsNull(Long tenantId);
    
    List<Product> findByTenantIdAndStatusAndFeaturedInSitemapTrueAndDeletedAtIsNull(Long tenantId, Product.ProductStatus status);
    
    Page<Product> findByTenantIdAndFeaturedInSitemapTrueAndDeletedAtIsNull(Long tenantId, Pageable pageable);
    
    long countByTenantIdAndFeaturedInSitemapTrueAndDeletedAtIsNull(Long tenantId);
}
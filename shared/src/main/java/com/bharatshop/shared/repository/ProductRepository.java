package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Page<Product> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Product> findBySlugAndTenantIdAndDeletedAtIsNull(String slug, UUID tenantId);

    List<Product> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, Product.ProductStatus status);

    Page<Product> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, Product.ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchByTenantIdAndKeyword(@Param("tenantId") UUID tenantId, 
                                           @Param("search") String search, 
                                           Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL AND " +
           ":categoryId MEMBER OF p.categories")
    List<Product> findByTenantIdAndCategoryId(@Param("tenantId") UUID tenantId, 
                                            @Param("categoryId") UUID categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.deletedAt IS NULL")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.status = :status AND p.deletedAt IS NULL")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") Product.ProductStatus status);
}
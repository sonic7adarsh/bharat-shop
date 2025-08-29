package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdAndIsDeletedFalseOrderBySortOrderAsc(UUID productId);

    Optional<ProductImage> findByIdAndProductIdAndIsDeletedFalse(UUID id, UUID productId);

    List<ProductImage> findByProductIdAndIsPrimaryAndIsDeletedFalse(UUID productId, Boolean isPrimary);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrueAndIsDeletedFalse(UUID productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.isDeleted = false ORDER BY pi.sortOrder ASC")
    List<ProductImage> findByProductIdOrderBySortOrder(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.productId = :productId AND pi.isDeleted = false")
    void clearPrimaryImageForProduct(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isDeleted = true WHERE pi.productId = :productId")
    void deleteByProductId(@Param("productId") UUID productId);

    @Query("SELECT COUNT(pi) FROM ProductImage pi WHERE pi.productId = :productId AND pi.isDeleted = false")
    long countByProductId(@Param("productId") UUID productId);

    @Query("SELECT MAX(pi.sortOrder) FROM ProductImage pi WHERE pi.productId = :productId AND pi.isDeleted = false")
    Integer findMaxSortOrderByProductId(@Param("productId") UUID productId);
}
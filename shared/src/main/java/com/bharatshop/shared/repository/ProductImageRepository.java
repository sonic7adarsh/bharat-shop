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

    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.deletedAt IS NULL ORDER BY pi.sortOrder ASC")
    List<ProductImage> findActiveByProductIdOrderBySortOrder(@Param("productId") UUID productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.id = :id AND pi.productId = :productId AND pi.deletedAt IS NULL")
    Optional<ProductImage> findActiveByIdAndProductId(@Param("id") UUID id, @Param("productId") UUID productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.isPrimary = :isPrimary AND pi.deletedAt IS NULL")
    List<ProductImage> findActiveByProductIdAndIsPrimary(@Param("productId") UUID productId, @Param("isPrimary") Boolean isPrimary);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.isPrimary = true AND pi.deletedAt IS NULL")
    Optional<ProductImage> findActivePrimaryByProductId(@Param("productId") UUID productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId AND pi.deletedAt IS NULL ORDER BY pi.sortOrder ASC")
    List<ProductImage> findByProductIdOrderBySortOrder(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.productId = :productId AND pi.deletedAt IS NULL")
    void clearPrimaryImageForProduct(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.deletedAt = CURRENT_TIMESTAMP WHERE pi.productId = :productId")
    void deleteByProductId(@Param("productId") UUID productId);

    @Query("SELECT COUNT(pi) FROM ProductImage pi WHERE pi.productId = :productId AND pi.deletedAt IS NULL")
    long countByProductId(@Param("productId") UUID productId);

    @Query("SELECT MAX(pi.sortOrder) FROM ProductImage pi WHERE pi.productId = :productId AND pi.deletedAt IS NULL")
    Integer findMaxSortOrderByProductId(@Param("productId") UUID productId);
}
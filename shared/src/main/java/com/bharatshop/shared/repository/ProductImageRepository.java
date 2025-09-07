package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query(value = "SELECT * FROM product_images WHERE product_id = :productId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductImage> findActiveByProductIdOrderBySortOrder(@Param("productId") Long productId);

    @Query(value = "SELECT * FROM product_images WHERE id = :id AND product_id = :productId AND deleted_at IS NULL", nativeQuery = true)
    Optional<ProductImage> findActiveByIdAndProductId(@Param("id") Long id, @Param("productId") Long productId);

    @Query(value = "SELECT * FROM product_images WHERE product_id = :productId AND is_primary = :isPrimary AND deleted_at IS NULL", nativeQuery = true)
    List<ProductImage> findActiveByProductIdAndIsPrimary(@Param("productId") Long productId, @Param("isPrimary") Boolean isPrimary);

    @Query(value = "SELECT * FROM product_images WHERE product_id = :productId AND is_primary = true AND deleted_at IS NULL", nativeQuery = true)
    Optional<ProductImage> findActivePrimaryByProductId(@Param("productId") Long productId);

    @Query(value = "SELECT * FROM product_images WHERE product_id = :productId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<ProductImage> findByProductIdOrderBySortOrder(@Param("productId") Long productId);

    @Modifying
    @Query(value = "UPDATE product_images SET is_primary = false WHERE product_id = :productId AND deleted_at IS NULL", nativeQuery = true)
    void clearPrimaryImageForProduct(@Param("productId") Long productId);

    @Modifying
    @Query(value = "UPDATE product_images SET deleted_at = CURRENT_TIMESTAMP WHERE product_id = :productId", nativeQuery = true)
    void deleteByProductId(@Param("productId") Long productId);

    @Query(value = "SELECT COUNT(*) FROM product_images WHERE product_id = :productId AND deleted_at IS NULL", nativeQuery = true)
    long countByProductId(@Param("productId") Long productId);

    @Query(value = "SELECT MAX(sort_order) FROM product_images WHERE product_id = :productId AND deleted_at IS NULL", nativeQuery = true)
    Integer findMaxSortOrderByProductId(@Param("productId") Long productId);
}
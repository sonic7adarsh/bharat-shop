package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    List<Category> findByTenantIdAndIsActiveAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId, Boolean isActive);

    Optional<Category> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Category> findBySlugAndTenantIdAndDeletedAtIsNull(String slug, UUID tenantId);

    List<Category> findByTenantIdAndParentIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId, Long parentId);

    List<Category> findByTenantIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.parentId = :parentId AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findChildCategories(@Param("tenantId") UUID tenantId, @Param("parentId") Long parentId);

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.parentId IS NULL AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findRootCategories(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.tenantId = :tenantId AND c.parentId = :parentId AND c.deletedAt IS NULL")
    long countChildCategories(@Param("tenantId") UUID tenantId, @Param("parentId") Long parentId);

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL AND " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Category> searchByTenantIdAndKeyword(@Param("tenantId") UUID tenantId, @Param("search") String search);

    boolean existsBySlugAndTenantIdAndDeletedAtIsNull(String slug, UUID tenantId);

    boolean existsBySlugAndTenantIdAndIdNotAndDeletedAtIsNull(String slug, UUID tenantId, UUID id);
}
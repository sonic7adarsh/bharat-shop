package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(Long tenantId);

    List<Category> findByTenantIdAndIsActiveAndDeletedAtIsNullOrderBySortOrderAsc(Long tenantId, Boolean isActive);

    Optional<Category> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    Optional<Category> findBySlugAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);

    List<Category> findByTenantIdAndParentIdAndDeletedAtIsNullOrderBySortOrderAsc(Long tenantId, Long parentId);

    List<Category> findByTenantIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrderAsc(Long tenantId);

    @Query(value = "SELECT * FROM categories WHERE tenant_id = :tenantId AND parent_id = :parentId AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Category> findChildCategories(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    @Query(value = "SELECT * FROM categories WHERE tenant_id = :tenantId AND parent_id IS NULL AND deleted_at IS NULL ORDER BY sort_order ASC", nativeQuery = true)
    List<Category> findRootCategories(@Param("tenantId") Long tenantId);

    @Query(value = "SELECT COUNT(id) FROM categories WHERE tenant_id = :tenantId AND parent_id = :parentId AND deleted_at IS NULL", nativeQuery = true)
    long countChildCategories(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    // Simplified method name-based query to avoid HQL validation issues
    List<Category> findByTenantIdAndDeletedAtIsNull(Long tenantId);

    boolean existsBySlugAndTenantIdAndDeletedAtIsNull(String slug, Long tenantId);

    boolean existsBySlugAndTenantIdAndIdNotAndDeletedAtIsNull(String slug, Long tenantId, Long id);
    
    // SEO-related methods for sitemap generation
    List<Category> findByTenantIdAndFeaturedInSitemapTrueAndDeletedAtIsNullOrderBySortOrderAsc(Long tenantId);
    
    List<Category> findByTenantIdAndIsActiveAndFeaturedInSitemapTrueAndDeletedAtIsNullOrderBySortOrderAsc(Long tenantId, Boolean isActive);
    
    long countByTenantIdAndFeaturedInSitemapTrueAndDeletedAtIsNull(Long tenantId);
}
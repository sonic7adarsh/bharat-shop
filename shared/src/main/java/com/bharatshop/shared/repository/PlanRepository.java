package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    // Find active plans
    @Query("SELECT p FROM Plan p WHERE p.active = true AND p.deletedAt IS NULL ORDER BY p.displayOrder ASC, p.price ASC")
    List<Plan> findAllActive();

    // Find plan by name
    @Query("SELECT p FROM Plan p WHERE p.name = :name AND p.active = true AND p.deletedAt IS NULL")
    Optional<Plan> findActiveByName(@Param("name") String name);

    // Find plans by price range
    @Query("SELECT p FROM Plan p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.active = true AND p.deletedAt IS NULL ORDER BY p.price ASC")
    List<Plan> findActiveByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // Find plans by duration
    @Query("SELECT p FROM Plan p WHERE p.durationDays = :durationDays AND p.active = true AND p.deletedAt IS NULL ORDER BY p.price ASC")
    List<Plan> findActiveByDuration(@Param("durationDays") Integer durationDays);

    // Find popular plans
    @Query("SELECT p FROM Plan p WHERE p.isPopular = true AND p.active = true AND p.deletedAt IS NULL ORDER BY p.displayOrder ASC")
    List<Plan> findPopularPlans();

    // Find plans with specific feature
    @Query(value = "SELECT * FROM plans p WHERE p.active = true AND p.deleted_at IS NULL AND JSON_EXTRACT(p.features, :featurePath) IS NOT NULL ORDER BY p.display_order ASC", nativeQuery = true)
    List<Plan> findActiveWithFeature(@Param("featurePath") String featurePath);

    // Find plans with minimum product limit
    @Query(value = "SELECT * FROM plans p WHERE p.active = true AND p.deleted_at IS NULL AND JSON_EXTRACT(p.features, '$.maxProducts') >= :minProducts ORDER BY p.price ASC", nativeQuery = true)
    List<Plan> findActiveWithMinProducts(@Param("minProducts") Integer minProducts);

    // Find plans with minimum storage limit
    @Query(value = "SELECT * FROM plans p WHERE p.active = true AND p.deleted_at IS NULL AND JSON_EXTRACT(p.features, '$.storageLimit') >= :minStorage ORDER BY p.price ASC", nativeQuery = true)
    List<Plan> findActiveWithMinStorage(@Param("minStorage") Long minStorage);

    // Check if plan name exists (excluding specific plan)
    @Query("SELECT COUNT(p) > 0 FROM Plan p WHERE p.name = :name AND p.id != :excludeId AND p.deletedAt IS NULL")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("excludeId") UUID excludeId);

    // Check if plan name exists
    @Query("SELECT COUNT(p) > 0 FROM Plan p WHERE p.name = :name AND p.deletedAt IS NULL")
    boolean existsByName(@Param("name") String name);

    // Find active plan by ID
    @Query("SELECT p FROM Plan p WHERE p.id = :id AND p.active = true AND p.deletedAt IS NULL")
    Optional<Plan> findActiveById(@Param("id") UUID id);

    // Count active plans
    @Query("SELECT COUNT(p) FROM Plan p WHERE p.active = true AND p.deletedAt IS NULL")
    long countActive();

    // Find cheapest active plan
    @Query("SELECT p FROM Plan p WHERE p.active = true AND p.deletedAt IS NULL ORDER BY p.price ASC LIMIT 1")
    Optional<Plan> findCheapestActivePlan();

    // Find most expensive active plan
    @Query("SELECT p FROM Plan p WHERE p.active = true AND p.deletedAt IS NULL ORDER BY p.price DESC LIMIT 1")
    Optional<Plan> findMostExpensiveActivePlan();
}
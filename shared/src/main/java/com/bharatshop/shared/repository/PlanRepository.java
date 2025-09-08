package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    // Find active plans
    List<Plan> findByActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAscPriceAsc();

    // Find plan by name
    Optional<Plan> findByNameAndActiveTrueAndDeletedAtIsNull(String name);

    // Find plans by price range
    List<Plan> findByPriceBetweenAndActiveTrueAndDeletedAtIsNullOrderByPriceAsc(BigDecimal minPrice, BigDecimal maxPrice);

    // Find plans by duration
    List<Plan> findByDurationDaysAndActiveTrueAndDeletedAtIsNullOrderByPriceAsc(Integer durationDays);

    // Find popular plans
    List<Plan> findByIsPopularTrueAndActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAsc();

    // Find plans with specific feature - using JPA method instead of JSON_EXTRACT
    // Note: These methods will be implemented in the service layer using Plan.hasFeature() helper method
    // to avoid H2 database JSON_EXTRACT compatibility issues
    
    // Find plans with minimum product limit - replaced with service layer implementation
    // @Query removed to avoid H2 JSON_EXTRACT issues
    
    // Find plans with minimum storage limit - replaced with service layer implementation  
    // @Query removed to avoid H2 JSON_EXTRACT issues

    // Check if plan name exists (excluding specific plan)
    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, Long excludeId);

    // Check if plan name exists
    boolean existsByNameAndDeletedAtIsNull(String name);

    // Find active plan by ID
    Optional<Plan> findByIdAndActiveTrueAndDeletedAtIsNull(Long id);

    // Count active plans
    long countByActiveTrueAndDeletedAtIsNull();

    // Find cheapest active plan
    @Query(value = "SELECT * FROM plans WHERE active = true AND deleted_at IS NULL ORDER BY price ASC LIMIT 1", nativeQuery = true)
    Optional<Plan> findCheapestActivePlan();

    // Find most expensive active plan
    @Query(value = "SELECT * FROM plans WHERE active = true AND deleted_at IS NULL ORDER BY price DESC LIMIT 1", nativeQuery = true)
    Optional<Plan> findMostExpensiveActivePlan();
    
    // Alias methods for service compatibility
    default Optional<Plan> findActiveById(Long id) {
        return findByIdAndActiveTrueAndDeletedAtIsNull(id);
    }
    
    default long countActive() {
        return countByActiveTrueAndDeletedAtIsNull();
    }
    
    default List<Plan> findActiveByDuration(Integer durationDays) {
        return findByDurationDaysAndActiveTrueAndDeletedAtIsNullOrderByPriceAsc(durationDays);
    }
    
    default boolean existsByName(String name) {
        return existsByNameAndDeletedAtIsNull(name);
    }
    
    default boolean existsByNameExcludingId(String name, Long excludeId) {
        return existsByNameAndIdNotAndDeletedAtIsNull(name, excludeId);
    }
    
    default Optional<Plan> findActiveByName(String name) {
        return findByNameAndActiveTrueAndDeletedAtIsNull(name);
    }
    
    default List<Plan> findPopularPlans() {
        return findByIsPopularTrueAndActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAsc();
    }
    
    default List<Plan> findActiveByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return findByPriceBetweenAndActiveTrueAndDeletedAtIsNullOrderByPriceAsc(minPrice, maxPrice);
    }
    
    default List<Plan> findAllActive() {
        return findByActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAscPriceAsc();
    }
}
package com.bharatshop.shared.service;

import com.bharatshop.shared.dto.PlanResponseDto;
import com.bharatshop.shared.entity.Plan;
import com.bharatshop.shared.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    
    private final PlanRepository planRepository;

    /**
     * Get all active plans
     */
    @Transactional(readOnly = true)
    public List<Plan> getAllActivePlans() {
        log.debug("Fetching all active plans");
        return planRepository.findAllActive();
    }

    /**
     * Get plan by ID
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getPlanById(UUID id) {
        log.debug("Fetching plan by ID: {}", id);
        return planRepository.findActiveById(id);
    }

    /**
     * Get plan by name
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getPlanByName(String name) {
        log.debug("Fetching plan by name: {}", name);
        return planRepository.findActiveByName(name);
    }

    /**
     * Get popular plans
     */
    @Transactional(readOnly = true)
    public List<Plan> getPopularPlans() {
        log.debug("Fetching popular plans");
        return planRepository.findPopularPlans();
    }

    /**
     * Get plans by price range
     */
    @Transactional(readOnly = true)
    public List<Plan> getPlansByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Fetching plans by price range: {} - {}", minPrice, maxPrice);
        return planRepository.findActiveByPriceRange(minPrice, maxPrice);
    }

    /**
     * Get plans by duration
     */
    @Transactional(readOnly = true)
    public List<Plan> getPlansByDuration(Integer durationDays) {
        log.debug("Fetching plans by duration: {} days", durationDays);
        return planRepository.findActiveByDuration(durationDays);
    }

    /**
     * Get plans with minimum product limit
     */
    @Transactional(readOnly = true)
    public List<Plan> getPlansWithMinProducts(Integer minProducts) {
        log.debug("Fetching plans with minimum products: {}", minProducts);
        return planRepository.findActiveWithMinProducts(minProducts);
    }

    /**
     * Get plans with minimum storage limit
     */
    @Transactional(readOnly = true)
    public List<Plan> getPlansWithMinStorage(Long minStorage) {
        log.debug("Fetching plans with minimum storage: {} bytes", minStorage);
        return planRepository.findActiveWithMinStorage(minStorage);
    }

    /**
     * Create a new plan
     */
    public Plan createPlan(Plan plan) {
        log.info("Creating new plan: {}", plan.getName());
        
        // Validate plan name uniqueness
        if (planRepository.existsByName(plan.getName())) {
            throw new IllegalArgumentException("Plan with name '" + plan.getName() + "' already exists");
        }
        
        // Set default values
        if (plan.getActive() == null) {
            plan.setActive(true);
        }
        if (plan.getIsPopular() == null) {
            plan.setIsPopular(false);
        }
        
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        
        Plan savedPlan = planRepository.save(plan);
        log.info("Plan created successfully with ID: {}", savedPlan.getId());
        return savedPlan;
    }

    /**
     * Update an existing plan
     */
    public Plan updatePlan(UUID id, Plan planUpdates) {
        log.info("Updating plan with ID: {}", id);
        
        Plan existingPlan = planRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));
        
        // Validate name uniqueness if name is being changed
        if (!existingPlan.getName().equals(planUpdates.getName()) && 
            planRepository.existsByNameExcludingId(planUpdates.getName(), id)) {
            throw new IllegalArgumentException("Plan with name '" + planUpdates.getName() + "' already exists");
        }
        
        // Update fields
        existingPlan.setName(planUpdates.getName());
        existingPlan.setPrice(planUpdates.getPrice());
        existingPlan.setFeatures(planUpdates.getFeatures());
        existingPlan.setDurationDays(planUpdates.getDurationDays());
        existingPlan.setDescription(planUpdates.getDescription());
        existingPlan.setDisplayOrder(planUpdates.getDisplayOrder());
        existingPlan.setIsPopular(planUpdates.getIsPopular());
        existingPlan.setActive(planUpdates.getActive());
        existingPlan.setUpdatedAt(LocalDateTime.now());
        
        Plan savedPlan = planRepository.save(existingPlan);
        log.info("Plan updated successfully: {}", savedPlan.getId());
        return savedPlan;
    }

    /**
     * Deactivate a plan (soft delete)
     */
    public void deactivatePlan(UUID id) {
        log.info("Deactivating plan with ID: {}", id);
        
        Plan plan = planRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));
        
        plan.setActive(false);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        
        log.info("Plan deactivated successfully: {}", id);
    }

    /**
     * Delete a plan (hard delete)
     */
    public void deletePlan(UUID id) {
        log.info("Deleting plan with ID: {}", id);
        
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + id));
        
        plan.setDeletedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        
        log.info("Plan deleted successfully: {}", id);
    }

    /**
     * Get cheapest active plan
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getCheapestPlan() {
        log.debug("Fetching cheapest active plan");
        return planRepository.findCheapestActivePlan();
    }

    /**
     * Get most expensive active plan
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getMostExpensivePlan() {
        log.debug("Fetching most expensive active plan");
        return planRepository.findMostExpensiveActivePlan();
    }

    /**
     * Validate if plan supports required features
     */
    @Transactional(readOnly = true)
    public boolean validatePlanFeatures(UUID planId, Integer requiredProducts, Long requiredStorage) {
        log.debug("Validating plan features for plan ID: {}", planId);
        
        Optional<Plan> planOpt = planRepository.findActiveById(planId);
        if (planOpt.isEmpty()) {
            return false;
        }
        
        Plan plan = planOpt.get();
        
        // Check product limit
        if (requiredProducts != null && plan.getMaxProducts() < requiredProducts) {
            log.debug("Plan {} does not support required products: {} (max: {})", 
                    planId, requiredProducts, plan.getMaxProducts());
            return false;
        }
        
        // Check storage limit
        if (requiredStorage != null && plan.getStorageLimit() < requiredStorage) {
            log.debug("Plan {} does not support required storage: {} (max: {})", 
                    planId, requiredStorage, plan.getStorageLimit());
            return false;
        }
        
        return true;
    }

    /**
     * Check if plan has specific feature
     */
    @Transactional(readOnly = true)
    public boolean planHasFeature(UUID planId, String featureName) {
        log.debug("Checking if plan {} has feature: {}", planId, featureName);
        
        Optional<Plan> planOpt = planRepository.findActiveById(planId);
        if (planOpt.isEmpty()) {
            return false;
        }
        
        return planOpt.get().hasFeature(featureName);
    }

    /**
     * Get total active plans count
     */
    @Transactional(readOnly = true)
    public long getActivePlansCount() {
        return planRepository.countActive();
    }

    /**
     * Convert Plan entity to PlanResponseDto
     */
    public PlanResponseDto convertToResponseDto(Plan plan) {
        return PlanResponseDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .maxProducts(plan.getMaxProducts())
                .storageLimit(plan.getStorageLimit())
                .features(plan.getFeatures())
                .active(plan.getActive())
                .isPopular(plan.getIsPopular())
                .displayOrder(plan.getDisplayOrder())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
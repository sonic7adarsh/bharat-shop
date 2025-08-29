package com.bharatshop.modules.auth.repository;

import com.bharatshop.modules.auth.entity.StorefrontUser;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StorefrontUser entity with tenant-aware operations.
 */
@Repository
public interface StorefrontUserRepository extends TenantAwareRepository<StorefrontUser> {
    
    @Query("SELECT u FROM StorefrontUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM StorefrontUser u WHERE u.phone = :phone AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByPhone(@Param("phone") String phone);

    @Query("SELECT u FROM StorefrontUser u WHERE (u.email = :emailOrPhone OR u.phone = :emailOrPhone) AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    @Query("SELECT COUNT(u) > 0 FROM StorefrontUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM StorefrontUser u WHERE u.phone = :phone AND u.deletedAt IS NULL")
    boolean existsByPhone(@Param("phone") String phone);
}
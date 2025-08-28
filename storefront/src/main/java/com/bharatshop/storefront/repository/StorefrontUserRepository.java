package com.bharatshop.storefront.repository;

import com.bharatshop.storefront.entity.StorefrontUser;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontUserRepository extends TenantAwareRepository<StorefrontUser> {

    @Query("SELECT u FROM StorefrontUser u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM StorefrontUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM StorefrontUser u WHERE u.phone = :phone AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM StorefrontUser u WHERE u.phone = :phone AND u.deletedAt IS NULL")
    Optional<StorefrontUser> findByPhone(@Param("phone") String phone);

    @Query("SELECT COUNT(u) > 0 FROM StorefrontUser u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    boolean existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) > 0 FROM StorefrontUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM StorefrontUser u WHERE u.phone = :phone AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    boolean existsByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) > 0 FROM StorefrontUser u WHERE u.phone = :phone AND u.deletedAt IS NULL")
    boolean existsByPhone(@Param("phone") String phone);
}
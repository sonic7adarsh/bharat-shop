package com.bharatshop.modules.auth.repository;

import com.bharatshop.modules.auth.entity.PlatformUser;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformUserRepository extends TenantAwareRepository<PlatformUser> {

    @Query("SELECT u FROM PlatformUser u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Optional<PlatformUser> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM PlatformUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<PlatformUser> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM PlatformUser u WHERE u.refreshToken = :refreshToken AND u.deletedAt IS NULL")
    Optional<PlatformUser> findByRefreshToken(@Param("refreshToken") String refreshToken);

    @Query("SELECT COUNT(u) > 0 FROM PlatformUser u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    boolean existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) > 0 FROM PlatformUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);
}
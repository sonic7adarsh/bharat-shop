package com.bharatshop.modules.auth.repository;

import com.bharatshop.shared.entity.User;
import com.bharatshop.shared.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformUserRepository extends TenantAwareRepository<User> {

    @Query(value = "SELECT * FROM users WHERE email = :email AND tenant_id = :tenantId AND user_type = 'PLATFORM' AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

    @Query(value = "SELECT * FROM users WHERE email = :email AND user_type = 'PLATFORM' AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE refresh_token = :refreshToken AND user_type = 'PLATFORM' AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByRefreshToken(@Param("refreshToken") String refreshToken);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email AND tenant_id = :tenantId AND user_type = 'PLATFORM' AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email AND user_type = 'PLATFORM' AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);
}
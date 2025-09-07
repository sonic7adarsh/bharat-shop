package com.bharatshop.shared.repository;

import com.bharatshop.shared.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedStorefrontUserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users WHERE email = :email AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE phone = :phone AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByPhone(@Param("phone") String phone);

    @Query(value = "SELECT * FROM users WHERE email = :email AND user_type = 'CUSTOMER' AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE phone = :phone AND user_type = 'CUSTOMER' AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByPhoneAndDeletedAtIsNull(@Param("phone") String phone);

    @Query(value = "SELECT * FROM users WHERE email = :email AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

    @Query(value = "SELECT * FROM users WHERE phone = :phone AND tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    Optional<User> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") Long tenantId);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByEmailAndTenantIdAndDeletedAtIsNull(String email, Long tenantId);

    boolean existsByPhoneAndTenantIdAndDeletedAtIsNull(String phone, Long tenantId);
}
package com.bharatshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for BharatShop.
 * 
 * This application follows clean architecture principles with:
 * - Domain-driven design in modules package
 * - Shared infrastructure in base packages
 * - Constructor injection only
 * - Immutable DTOs using records
 */
@SpringBootApplication(scanBasePackages = {
    "com.bharatshop",
    "com.bharatshop.shared"
})
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
    "com.bharatshop.modules.auth.repository",
    "com.bharatshop.modules.users.repository",
    "com.bharatshop.shared.repository"
})
@EntityScan(basePackages = {
    "com.bharatshop.modules.auth.entity",
    "com.bharatshop.modules.users.entity",
    "com.bharatshop.shared.entity"
})
public class BharatShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BharatShopApplication.class, args);
    }
}
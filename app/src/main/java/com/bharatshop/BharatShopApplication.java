package com.bharatshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
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
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.bharatshop.app",
        "com.bharatshop.config",
        "com.bharatshop.modules",
        "com.bharatshop.shared",
        "com.bharatshop.storefront",
        "com.bharatshop.platform"
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bharatshop\\.platform\\.PlatformApplication"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bharatshop\\.storefront\\.StorefrontApplication")
    }
)
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.bharatshop.shared.repository",
    "com.bharatshop.storefront.repository",
    "com.bharatshop.platform.repository"
})
@EntityScan(basePackages = {
    "com.bharatshop.shared.entity"
})
public class BharatShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BharatShopApplication.class, args);
    }
}
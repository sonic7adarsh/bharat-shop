package com.bharatshop.storefront;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.bharatshop.storefront", "com.bharatshop.shared"})
@EnableTransactionManagement
@EnableCaching
@EnableJpaRepositories(basePackages = {"com.bharatshop.storefront.repository", "com.bharatshop.shared.repository"})
@EntityScan(basePackages = {
    "com.bharatshop.shared.entity",
    "com.bharatshop.storefront.entity"
})
public class StorefrontApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorefrontApplication.class, args);
    }
}
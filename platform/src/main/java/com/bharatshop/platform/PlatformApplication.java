package com.bharatshop.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "com.bharatshop.platform.repository",
    "com.bharatshop.shared.repository"
})
@EntityScan(basePackages = {
    "com.bharatshop.platform.entity",
    "com.bharatshop.platform.model",
    "com.bharatshop.shared.entity"
})
@ComponentScan(basePackages = {
    "com.bharatshop.platform",
    "com.bharatshop.shared"
})
@EnableTransactionManagement
@EnableCaching
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
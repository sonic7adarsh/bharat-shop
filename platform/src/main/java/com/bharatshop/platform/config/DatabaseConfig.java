package com.bharatshop.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA will handle database schema creation automatically
    // Flyway is disabled in application.yml
}
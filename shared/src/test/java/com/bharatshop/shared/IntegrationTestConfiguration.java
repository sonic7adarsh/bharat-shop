package com.bharatshop.shared;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Integration test configuration for Spring Boot tests in the shared module.
 * This provides the necessary @SpringBootConfiguration for integration tests with real database.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.bharatshop.shared")
@EnableJpaRepositories(basePackages = "com.bharatshop.shared.repository")
@EntityScan(basePackages = "com.bharatshop.shared.entity")
@EnableTransactionManagement
public class IntegrationTestConfiguration {
}
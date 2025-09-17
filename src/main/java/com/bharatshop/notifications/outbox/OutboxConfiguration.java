package com.bharatshop.notifications.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for outbox pattern implementation.
 * Sets up necessary beans and async processing configuration.
 */
@Configuration
@EnableScheduling
@EnableAsync
@ConditionalOnProperty(name = "bharatshop.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxConfiguration {
    
    /**
     * Task executor for async outbox event processing
     */
    @Bean("outboxTaskExecutor")
    public Executor outboxTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * ObjectMapper for JSON serialization/deserialization
     */
    @Bean
    @ConditionalOnProperty(name = "bharatshop.outbox.custom-object-mapper", havingValue = "true")
    public ObjectMapper outboxObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}
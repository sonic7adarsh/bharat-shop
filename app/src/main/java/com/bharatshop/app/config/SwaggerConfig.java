package com.bharatshop.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BharatShop API")
                        .description("Comprehensive e-commerce platform API for BharatShop - supporting multi-tenancy, product management, order processing, and customer management")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("BharatShop Development Team")
                                .email("dev@bharatshop.com")
                                .url("https://bharatshop.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Development Server"),
                        new Server().url("https://api.bharatshop.com").description("Production Server"),
                        new Server().url("https://staging-api.bharatshop.com").description("Staging Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token for authentication"))
                        .addSecuritySchemes("tenantHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Tenant-ID")
                                .description("Tenant identifier for multi-tenancy")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth")
                        .addList("tenantHeader"));
    }
}
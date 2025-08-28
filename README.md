# BharatShop - Multi-Module Spring Boot Application

A comprehensive Spring Boot 3.x multi-module project with Java 17 and Maven, designed for SaaS multi-tenant e-commerce platform.

## Project Structure

```
bharatshop/
├── pom.xml                 # Parent POM with dependency management
├── platform/               # SaaS multi-tenant APIs module
│   ├── pom.xml
│   └── src/main/java/com/bharatshop/platform/
│       ├── PlatformApplication.java
│       ├── config/
│       │   ├── DatabaseConfig.java
│       │   ├── CacheConfig.java
│       │   └── SecurityConfig.java
│       ├── controller/
│       │   └── TenantController.java
│       ├── service/
│       │   └── TenantService.java
│       ├── repository/
│       │   └── TenantRepository.java
│       ├── model/
│       │   └── Tenant.java
│       ├── dto/
│       │   ├── TenantCreateDto.java
│       │   └── TenantResponseDto.java
│       ├── security/
│       │   └── SecurityConfig.java
│       ├── tenant/
│       │   ├── TenantContext.java
│       │   └── TenantInterceptor.java
│       └── shared/
│           └── ApiResponse.java
└── storefront/             # Customer-facing APIs module
    ├── pom.xml
    └── src/main/java/com/bharatshop/storefront/
        ├── StorefrontApplication.java
        ├── controller/
        │   └── ProductController.java
        ├── service/
        │   └── ProductService.java
        ├── repository/
        │   └── ProductRepository.java
        ├── model/
        │   └── Product.java
        ├── dto/
        │   └── ProductResponseDto.java
        └── shared/
            └── ApiResponse.java
```

## Technology Stack

### Core Framework
- **Spring Boot 3.x** - Latest Spring Boot framework
- **Java 17** - Modern Java LTS version
- **Maven** - Build and dependency management

### Dependencies Included

#### Web & API
- `spring-boot-starter-web` - REST API development
- `springdoc-openapi-starter-webmvc-ui` - API documentation (Swagger)

#### Security
- `spring-boot-starter-security` - Authentication and authorization
- `spring-boot-starter-validation` - Input validation

#### Database
- `spring-boot-starter-data-jpa` - JPA/Hibernate ORM
- `postgresql` - PostgreSQL database driver
- `mysql-connector-j` - MySQL database driver
- `flyway-core` - Database migration
- `h2` - In-memory database for testing

#### Caching
- `spring-boot-starter-cache` - Caching abstraction
- `spring-boot-starter-data-redis` - Redis integration
- `jedis` - Redis Java client

#### Cloud Storage
- `aws-java-sdk-s3` - AWS S3 integration
- `minio` - MinIO object storage

#### Code Generation
- `lombok` - Reduce boilerplate code
- `mapstruct` - Bean mapping

#### Monitoring
- `spring-boot-starter-actuator` - Application monitoring
- `micrometer-registry-prometheus` - Prometheus metrics

#### Payment
- `okhttp` - HTTP client for payment gateway integration
- `razorpay-java` - Razorpay payment gateway

#### Testing
- `spring-boot-starter-test` - Testing framework
- `spring-security-test` - Security testing
- `testcontainers-junit-jupiter` - Integration testing
- `testcontainers-postgresql` - PostgreSQL test containers
- `testcontainers-mysql` - MySQL test containers

## Module Details

### Platform Module (Port: 8080)
SaaS multi-tenant APIs for:
- Tenant management
- Multi-tenant data isolation
- Admin operations
- Platform configuration

**Key Features:**
- Multi-tenant architecture with `TenantContext` and `TenantInterceptor`
- Comprehensive security configuration
- Database and cache configuration
- RESTful APIs with Swagger documentation

### Storefront Module (Port: 8081)
Customer-facing APIs for:
- Product catalog browsing
- Product search and filtering
- Customer operations
- E-commerce functionality

**Key Features:**
- Product management with advanced search
- Caching for performance optimization
- Clean architecture with layered design
- Comprehensive product data model

## Configuration

### Database Configuration
- **Development**: H2 in-memory database
- **Production**: PostgreSQL/MySQL with connection pooling
- **Migration**: Flyway for database versioning

### Caching
- Redis-based caching with configurable TTL
- Method-level caching for performance optimization

### Security
- JWT-based authentication
- Role-based access control
- CORS configuration
- Security headers (HSTS, Content-Type Options)

### Monitoring
- Actuator endpoints for health checks
- Prometheus metrics integration
- Structured logging with different profiles

## Getting Started

### Coding Standards

- **Java 17** with Spring Boot 3.x
- **Records** for immutable DTOs
- **MapStruct** for type-safe mapping
- **Lombok** only for entities (getters/setters) and builders
- **Constructor injection** only (no field injection)
- **Response envelope**: `{ success, data, error, traceId }`
- **SLF4J logging** with Logback JSON encoder in production

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+ (for production)
- Git

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd bharatshop
```

### 2. Build the Project
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package
```

### 3. Run the Application

#### Development Mode (H2 Database)
```bash
# Run with dev profile (default)
mvn spring-boot:run -pl app

# Or with explicit profile
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=dev
```

#### Production Mode (MySQL Database)
```bash
# Set environment variables
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password

# Run with production profile
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### 4. Access the Application

- **Application**: http://localhost:8080
- **H2 Console** (dev only): http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/actuator/health

## Development Setup

### Database Setup

#### Development (H2)
No setup required. H2 runs in-memory and is configured automatically.

#### Production (MySQL)
```sql
CREATE DATABASE bharatshop;
CREATE USER 'bharatshop'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON bharatshop.* TO 'bharatshop'@'localhost';
FLUSH PRIVILEGES;
```

### IDE Configuration

#### IntelliJ IDEA
1. Install Lombok plugin
2. Enable annotation processing: Settings → Build → Compiler → Annotation Processors
3. Import the project as Maven project

#### VS Code
1. Install Java Extension Pack
2. Install Lombok Annotations Support
3. Open the project folder

## Project Structure

```
bharatshop/
├── pom.xml                 # Parent POM
├── README.md              # This file
└── app/                   # Main application module
    ├── pom.xml           # App module POM
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/bharatshop/
        │   │       ├── BharatShopApplication.java
        │   │       ├── config/
        │   │       ├── security/
        │   │       ├── tenant/
        │   │       ├── common/
        │   │       └── modules/
        │   └── resources/
        │       ├── application.yml
        │       └── logback-spring.xml
        └── test/
```

## API Response Format

All API responses follow a consistent envelope format:

```json
{
  "success": true,
  "data": {
    // Response data here
  },
  "error": null,
  "traceId": "uuid-trace-id"
}
```

Error responses:
```json
{
  "success": false,
  "data": null,
  "error": {
    "message": "Error description",
    "code": "ERROR_CODE"
  },
  "traceId": "uuid-trace-id"
}
```

## Logging

### Development
- Console output with readable format
- Debug level for application packages

### Production
- JSON format for structured logging
- File rotation (100MB, 30 days retention)
- Includes trace IDs for request correlation

## Testing

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run integration tests
mvn verify
```

## Building for Production

```bash
# Create production JAR
mvn clean package -Pprod

# Run the JAR
java -jar app/target/bharatshop-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## Docker Support

```dockerfile
# Dockerfile example
FROM openjdk:17-jre-slim
COPY app/target/bharatshop-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|----------|
| `DB_USERNAME` | Database username | bharatshop |
| `DB_PASSWORD` | Database password | password |
| `PORT` | Server port | 8080 |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | dev |

## Contributing

1. Follow the established coding standards
2. Write tests for new features
3. Use conventional commit messages
4. Ensure all tests pass before submitting PR

## License

This project is licensed under the MIT License.
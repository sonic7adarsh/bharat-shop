# Multi-stage build for BharatShop application
# Stage 1: Build stage
FROM maven:3.9.5-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom files for dependency resolution
COPY pom.xml .
COPY shared/pom.xml shared/
COPY platform/pom.xml platform/
COPY storefront/pom.xml storefront/
COPY app/pom.xml app/

# Download dependencies (this layer will be cached if pom files don't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY shared/src shared/src
COPY platform/src platform/src
COPY storefront/src storefront/src
COPY app/src app/src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S bharatshop && \
    adduser -S bharatshop -u 1001 -G bharatshop

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/app/target/bharatshop-app-*.jar app.jar

# Change ownership to non-root user
RUN chown -R bharatshop:bharatshop /app

# Switch to non-root user
USER bharatshop

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
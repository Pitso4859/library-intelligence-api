# =============================================
# Pitso Book System - Multi-stage Dockerfile
# =============================================

# Stage 1: Build with Maven
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Cache dependencies layer — copy pom first, download deps, then copy source
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build the application (skip tests — run them in CI separately)
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime (minimal JRE only)
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup -S pitso && adduser -S pitso -G pitso
USER pitso

# Copy the fat JAR from builder stage
COPY --from=builder /app/target/pitso-book-system-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

# =============================================
# Pitso Book System - Multi-stage Dockerfile
# =============================================

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Build application
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Runtime (minimal JRE)
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup -S pitso && adduser -S pitso -G pitso
USER pitso

# Copy the fat JAR from builder
COPY --from=builder /app/target/pitso-book-system-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

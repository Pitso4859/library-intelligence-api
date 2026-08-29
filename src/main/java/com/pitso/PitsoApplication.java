package com.pitso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pitso Library Intelligence API
 *
 * Entry point for the Spring Boot application.
 *
 * Run modes:
 *   Dev (H2):  mvn spring-boot:run
 *   Prod (PG): mvn spring-boot:run -Dspring-boot.run.profiles=prod
 *
 * API Docs: http://localhost:8080/swagger-ui.html
 * H2 Console (dev): http://localhost:8080/h2-console
 */
@SpringBootApplication
public class PitsoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PitsoApplication.class, args);
    }
}

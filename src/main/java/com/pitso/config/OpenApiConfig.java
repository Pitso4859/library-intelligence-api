package com.pitso.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pitsoOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Pitso Library Intelligence API")
                .description("""
                    Production-style Java REST API for a polymorphic book catalog.

                    Engineering features include transactional bulk writes, explainable
                    recommendations, catalog analytics, request correlation IDs,
                    pagination, validation, central error handling and PostgreSQL
                    migrations.

                    ISBN rules:
                    - exactly 10 characters
                    - starts with 0 for EBook or 1 for PrintBook
                    - first 9 characters are numeric
                    - last character is a digit, B or b
                    """)
                .version("v1.1.0")
                .contact(new Contact()
                    .name("Pitso Nkotolane")
                    .url("https://github.com/Pitso4859"))
                .license(new License().name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Development")
            ));
    }
}

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
                .title("Pitso Book Management System API")
                .description("""
                    RESTful API for managing a book inventory containing EBooks and PrintBooks.
                    
                    **ISBN Rules:**
                    - Must be exactly 10 characters
                    - Must start with `0` (EBook) or `1` (PrintBook)
                    - First 9 characters must be numeric digits
                    - Last character must be a digit, `B`, or `b`
                    """)
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Pitso Team")
                    .email("api@pitso.com"))
                .license(new License().name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Development"),
                new Server().url("https://api.pitso.com").description("Production")
            ));
    }
}

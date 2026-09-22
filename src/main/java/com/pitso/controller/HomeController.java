package com.pitso.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight API information endpoint that does not require a database connection.
 * The web interface is served at "/" by LibraryWebController.
 */
@RestController
@Hidden
public class HomeController {

    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Library Intelligence API");
        body.put("status", "UP");
        body.put("version", "1.1.0");
        body.put("web", "/");
        body.put("documentation", "/swagger-ui.html");
        body.put("openApi", "/api-docs");
        body.put("health", "/actuator/health");
        body.put("books", "/api/v1/books");
        body.put("catalog", "/api/v1/catalog");
        return ResponseEntity.ok(body);
    }
}

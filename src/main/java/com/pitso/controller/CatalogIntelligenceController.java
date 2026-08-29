package com.pitso.controller;

import com.pitso.model.CatalogDtos.CatalogInsights;
import com.pitso.model.CatalogDtos.RecommendationResponse;
import com.pitso.service.CatalogIntelligenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recruiter-facing showcase feature: explainable recommendations and catalog
 * analytics implemented entirely in the Java service layer.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog Intelligence", description = "Explainable recommendations and inventory analytics")
public class CatalogIntelligenceController {

    private final CatalogIntelligenceService intelligenceService;

    public CatalogIntelligenceController(CatalogIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/recommendations")
    @Operation(
        summary = "Get explainable book recommendations",
        description = "Ranks up to 200 catalog candidates using deterministic Java scoring and returns reasons for every recommendation."
    )
    public ResponseEntity<List<RecommendationResponse>> recommendations(
            @Parameter(description = "Optional title/author search text")
            @RequestParam(defaultValue = "") String q,
            @Parameter(description = "ANY, EBOOK or PRINTBOOK")
            @RequestParam(defaultValue = "ANY") String preferredType,
            @Parameter(description = "Number of recommendations, 1-20")
            @RequestParam(defaultValue = "5") int limit) {

        return ResponseEntity.ok(intelligenceService.recommend(q, preferredType, limit));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get catalog analytics and operational insights")
    public ResponseEntity<CatalogInsights> insights() {
        return ResponseEntity.ok(intelligenceService.getInsights());
    }
}

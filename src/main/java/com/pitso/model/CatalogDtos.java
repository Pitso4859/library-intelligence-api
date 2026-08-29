package com.pitso.model;

import java.util.List;

import com.pitso.model.BookDtos.BookResponse;

/**
 * Read-only DTOs used by the catalog intelligence API.
 *
 * The recommendation engine is intentionally explainable: each recommendation
 * includes a score and human-readable reasons instead of returning an opaque
 * ranking. This makes the feature easier to test, debug and evolve.
 */
public final class CatalogDtos {

    private CatalogDtos() {}

    public record RecommendationResponse(
        Long id,
        String title,
        String author,
        String isbnNo,
        String bookType,
        int score,
        List<String> reasons
    ) {
        public RecommendationResponse {
            reasons = List.copyOf(reasons);
        }
    }

    public record AuthorInsight(
        String author,
        long bookCount
    ) {}

    public record CatalogInsights(
        long totalBooks,
        long totalEBooks,
        long totalPrintBooks,
        long uniqueAuthors,
        double averageEBookSizeKb,
        double averagePrintPages,
        double averagePrintWeightGrams,
        List<AuthorInsight> topAuthors,
        List<BookResponse> newestBooks
    ) {
        public CatalogInsights {
            topAuthors = List.copyOf(topAuthors);
            newestBooks = List.copyOf(newestBooks);
        }
    }
}

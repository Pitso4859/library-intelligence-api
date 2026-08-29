package com.pitso.service;

import com.pitso.model.Book;
import com.pitso.model.EBook;
import com.pitso.model.PrintBook;
import com.pitso.model.CatalogDtos.AuthorInsight;
import com.pitso.model.CatalogDtos.CatalogInsights;
import com.pitso.model.CatalogDtos.RecommendationResponse;
import com.pitso.repository.AuthorCountProjection;
import com.pitso.repository.BookRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.pitso.model.BookDtos.BookResponse;

/**
 * Read-only catalog intelligence service.
 *
 * This feature deliberately uses an explainable deterministic ranking model
 * rather than a black-box dependency. Recruiters can inspect the scoring rules,
 * tests can assert them, and future versions can replace the algorithm without
 * changing the API contract.
 */
@Service
@Transactional(readOnly = true)
public class CatalogIntelligenceService {

    private static final int MAX_CANDIDATES = 200;
    private static final int MAX_RECOMMENDATIONS = 20;

    private final BookRepository bookRepository;

    public CatalogIntelligenceService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<RecommendationResponse> recommend(String query, String preferredType, int limit) {
        if (limit < 1 || limit > MAX_RECOMMENDATIONS) {
            throw new IllegalArgumentException("limit must be between 1 and 20");
        }

        String normalizedType = normalizeType(preferredType);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> terms = normalizedQuery.isBlank()
            ? List.of()
            : Arrays.stream(normalizedQuery.split("\\s+"))
                .filter(term -> !term.isBlank())
                .distinct()
                .toList();

        List<Book> candidates = bookRepository.findAll(
            PageRequest.of(0, MAX_CANDIDATES, Sort.by("createdAt").descending())
        ).getContent();

        return candidates.stream()
            .filter(book -> "ANY".equals(normalizedType) || book.getBookType().equals(normalizedType))
            .map(book -> score(book, normalizedQuery, terms, normalizedType))
            .filter(scored -> terms.isEmpty() || scored.textMatched())
            .sorted(Comparator
                .comparingInt(ScoredRecommendation::score).reversed()
                .thenComparing(scored -> scored.book().getTitle(), String.CASE_INSENSITIVE_ORDER))
            .limit(limit)
            .map(this::toResponse)
            .toList();
    }

    public CatalogInsights getInsights() {
        List<AuthorInsight> topAuthors = bookRepository
            .findTopAuthors(PageRequest.of(0, 5))
            .stream()
            .map(this::toAuthorInsight)
            .toList();

        List<BookResponse> newestBooks = bookRepository.findAll(
            PageRequest.of(0, 5, Sort.by("createdAt").descending())
        ).stream().map(BookResponse::from).toList();

        return new CatalogInsights(
            bookRepository.count(),
            bookRepository.countEBooks(),
            bookRepository.countPrintBooks(),
            bookRepository.countDistinctAuthors(),
            round(bookRepository.averageEBookSizeKb()),
            round(bookRepository.averagePrintPages()),
            round(bookRepository.averagePrintWeightGrams()),
            topAuthors,
            newestBooks
        );
    }

    private ScoredRecommendation score(
            Book book,
            String fullQuery,
            List<String> terms,
            String preferredType) {

        int score = 10;
        boolean textMatched = terms.isEmpty();
        List<String> reasons = new ArrayList<>();

        String title = book.getTitle().toLowerCase(Locale.ROOT);
        String author = book.getAuthor().toLowerCase(Locale.ROOT);

        if (!fullQuery.isBlank() && title.equals(fullQuery)) {
            score += 45;
            textMatched = true;
            reasons.add("Exact title match");
        }

        for (String term : terms) {
            if (title.contains(term)) {
                score += 18;
                textMatched = true;
                reasons.add("Title matches '" + term + "'");
            }
            if (author.contains(term)) {
                score += 12;
                textMatched = true;
                reasons.add("Author matches '" + term + "'");
            }
        }

        if (!"ANY".equals(preferredType)) {
            score += 20;
            reasons.add("Matches preferred format " + preferredType);
        }

        if (book instanceof EBook ebook && ebook.getFileSizeKb() <= 4096) {
            score += 5;
            reasons.add("Compact digital edition");
        } else if (book instanceof PrintBook printBook && printBook.getNoOfPages() <= 400) {
            score += 5;
            reasons.add("Concise print edition");
        }

        LocalDateTime createdAt = book.getCreatedAt();
        if (createdAt != null && createdAt.isAfter(LocalDateTime.now().minusDays(90))) {
            score += 5;
            reasons.add("Recently added to the catalog");
        }

        if (reasons.isEmpty()) {
            reasons.add("Strong general catalog match");
        }

        return new ScoredRecommendation(book, score, textMatched, reasons);
    }

    private RecommendationResponse toResponse(ScoredRecommendation scored) {
        Book book = scored.book();
        return new RecommendationResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getIsbnNo(),
            book.getBookType(),
            scored.score(),
            scored.reasons()
        );
    }

    private AuthorInsight toAuthorInsight(AuthorCountProjection projection) {
        return new AuthorInsight(projection.getAuthor(), projection.getBookCount());
    }

    private String normalizeType(String preferredType) {
        if (preferredType == null || preferredType.isBlank()) {
            return "ANY";
        }

        String normalized = preferredType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ANY", "EBOOK", "PRINTBOOK").contains(normalized)) {
            throw new IllegalArgumentException(
                "preferredType must be one of ANY, EBOOK or PRINTBOOK");
        }
        return normalized;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ScoredRecommendation(
        Book book,
        int score,
        boolean textMatched,
        List<String> reasons
    ) {
        private ScoredRecommendation {
            reasons = List.copyOf(reasons);
        }
    }
}

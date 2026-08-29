package com.pitso.service;

import com.pitso.model.Book;
import com.pitso.model.EBook;
import com.pitso.model.PrintBook;
import com.pitso.model.CatalogDtos.RecommendationResponse;
import com.pitso.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogIntelligenceService unit tests")
class CatalogIntelligenceServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CatalogIntelligenceService intelligenceService;

    @Test
    @DisplayName("recommend: ranks a title match ahead of unrelated books")
    void recommend_titleMatchRanksFirst() {
        Book cleanCode = new EBook("Clean Code", "Robert C. Martin", "032156840b", 3500);
        Book refactoring = new PrintBook("Refactoring", "Martin Fowler", "119873456B", 448, 680.5f);

        when(bookRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(refactoring, cleanCode)));

        List<RecommendationResponse> result = intelligenceService.recommend("clean", "ANY", 5);

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).title());
        assertTrue(result.get(0).score() > 10);
        assertTrue(result.get(0).reasons().stream().anyMatch(reason -> reason.contains("Title matches")));
    }

    @Test
    @DisplayName("recommend: respects preferred format")
    void recommend_filtersByPreferredType() {
        Book ebook = new EBook("Java Fundamentals", "A. Developer", "012345678B", 2000);
        Book printBook = new PrintBook("Java in Practice", "B. Engineer", "112345678B", 300, 400f);

        when(bookRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(ebook, printBook)));

        List<RecommendationResponse> result = intelligenceService.recommend("java", "EBOOK", 5);

        assertEquals(1, result.size());
        assertEquals("EBOOK", result.get(0).bookType());
        assertTrue(result.get(0).reasons().contains("Matches preferred format EBOOK"));
    }

    @Test
    @DisplayName("recommend: rejects unsupported preferred type")
    void recommend_rejectsInvalidType() {
        assertThrows(IllegalArgumentException.class,
            () -> intelligenceService.recommend("java", "AUDIOBOOK", 5));
    }

    @Test
    @DisplayName("recommend: enforces bounded result limits")
    void recommend_rejectsOversizedLimit() {
        assertThrows(IllegalArgumentException.class,
            () -> intelligenceService.recommend("", "ANY", 21));
    }
}

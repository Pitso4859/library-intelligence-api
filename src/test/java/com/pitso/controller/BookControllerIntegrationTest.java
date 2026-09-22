package com.pitso.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitso.model.BookDtos.CreateBookRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests — uses full Spring context + H2 in-memory DB.
 * Each test runs in a transaction that's rolled back after the test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Book API Integration Tests")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/books";

    @Test
    @DisplayName("GET /: returns API status")
    void root_returnsApiStatus() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Library Intelligence API"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.books").value("/api/v1/books"));
    }

    @Test
    @DisplayName("GET /api: returns API status")
    void apiInfo_returnsApiStatus() throws Exception {
        mockMvc.perform(get("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Library Intelligence API"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.documentation").value("/swagger-ui.html"));
    }

    @Test
    @DisplayName("Unknown route: returns 404 instead of being converted to 500")
    void unknownRoute_returns404() throws Exception {
        mockMvc.perform(get("/this-route-does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    // ── POST /api/v1/books ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /books: creates EBook successfully")
    void createEBook_returns201() throws Exception {
        CreateBookRequest req = new CreateBookRequest();
        req.setTitle("Clean Code");
        req.setAuthor("Robert C. Martin");
        req.setIsbnNo("032156840b");
        req.setFileSizeKb(3500);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bookType").value("EBOOK"))
            .andExpect(jsonPath("$.title").value("Clean Code"))
            .andExpect(jsonPath("$.fileSizeKb").value(3500))
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("POST /books: creates PrintBook successfully")
    void createPrintBook_returns201() throws Exception {
        CreateBookRequest req = new CreateBookRequest();
        req.setTitle("Refactoring");
        req.setAuthor("Martin Fowler");
        req.setIsbnNo("119873456B");
        req.setNoOfPages(448);
        req.setWeightGrams(680.5f);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bookType").value("PRINTBOOK"))
            .andExpect(jsonPath("$.noOfPages").value(448));
    }

    @Test
    @DisplayName("POST /books: returns 400 for invalid ISBN (too short)")
    void createBook_invalidIsbn_returns400() throws Exception {
        CreateBookRequest req = new CreateBookRequest();
        req.setTitle("Bad Book");
        req.setAuthor("Bad Author");
        req.setIsbnNo("192156844"); // 9 chars - invalid
        req.setFileSizeKb(100);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /books: returns 400 for missing required fields")
    void createBook_missingTitle_returns400() throws Exception {
        CreateBookRequest req = new CreateBookRequest();
        req.setAuthor("Someone");
        req.setIsbnNo("032156840b");

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    // ── GET /api/v1/books ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /books: returns paginated list")
    void getAllBooks_returnsPaginatedList() throws Exception {
        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /books/{id}: returns 404 for unknown id")
    void getBookById_notFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Book not found with id: 999999"));
    }

    // ── GET /stats ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /books/stats: returns inventory stats")
    void getStats_returnsStats() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBooks").isNumber())
            .andExpect(jsonPath("$.totalEBooks").isNumber())
            .andExpect(jsonPath("$.totalPrintBooks").isNumber());
    }


    @Test
    @DisplayName("POST /books/bulk: creates multiple books atomically")
    void createBooksBulk_returns201() throws Exception {
        String payload = """
            {
              \"books\": [
                {
                  \"title\": \"Effective Java\",
                  \"author\": \"Joshua Bloch\",
                  \"isbnNo\": \"012345670B\",
                  \"fileSizeKb\": 2400
                },
                {
                  \"title\": \"Java Concurrency in Practice\",
                  \"author\": \"Brian Goetz\",
                  \"isbnNo\": \"112345670B\",
                  \"noOfPages\": 424,
                  \"weightGrams\": 620.0
                }
              ]
            }
            """;

        mockMvc.perform(post(BASE_URL + "/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requested").value(2))
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.books[0].bookType").value("EBOOK"))
            .andExpect(jsonPath("$.books[1].bookType").value("PRINTBOOK"));
    }

    @Test
    @DisplayName("GET /catalog/recommendations: returns explainable Java recommendations")
    void recommendations_returnsExplainableRanking() throws Exception {
        CreateBookRequest req = new CreateBookRequest();
        req.setTitle("Effective Java");
        req.setAuthor("Joshua Bloch");
        req.setIsbnNo("012345671B");
        req.setFileSizeKb(2200);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/catalog/recommendations")
                .param("q", "java")
                .param("preferredType", "EBOOK")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Effective Java"))
            .andExpect(jsonPath("$[0].score").isNumber())
            .andExpect(jsonPath("$[0].reasons").isArray());
    }

    @Test
    @DisplayName("Every response includes X-Request-ID for traceability")
    void requestIdHeader_isReturned() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    @DisplayName("GET /books: rejects unsafe sort fields")
    void getAllBooks_invalidSort_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL).param("sortBy", "doesNotExist"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.requestId").exists());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /books/{id}: returns 404 when book does not exist")
    void deleteBook_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/999999"))
            .andExpect(status().isNotFound());
    }
}

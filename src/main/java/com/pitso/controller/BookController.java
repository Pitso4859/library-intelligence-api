package com.pitso.controller;

import com.pitso.model.BookDtos.*;
import com.pitso.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Set;

/** REST API for book inventory management. */
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Book inventory management API")
public class BookController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "title", "author", "isbnNo", "createdAt", "updatedAt"
    );

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Create a book",
        description = "Creates an EBook or PrintBook from the ISBN prefix."
    )
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }

    @PostMapping("/bulk")
    @Operation(
        summary = "Create multiple books atomically",
        description = "Creates up to 100 books in a single transaction. If one item fails validation, none are persisted."
    )
    public ResponseEntity<BulkCreateBookResponse> createBooksBulk(
            @Valid @RequestBody BulkCreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBooksBulk(request));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all books (paginated)")
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String direction) {

        return ResponseEntity.ok(bookService.getAllBooks(buildPageable(page, size, sortBy, direction)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a book by ID")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/isbn/{isbnNo}")
    @Operation(summary = "Get a book by ISBN")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbnNo) {
        return ResponseEntity.ok(bookService.getBookByIsbn(isbnNo));
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title or author")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @Parameter(description = "Search query") @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("Search query q must not be blank");
        }

        Pageable pageable = buildPageable(page, size, "title", "asc");
        return ResponseEntity.ok(bookService.searchBooks(q, pageable));
    }

    @GetMapping("/ebooks")
    @Operation(summary = "List EBooks (paginated)")
    public ResponseEntity<Page<BookResponse>> getAllEBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getAllEBooks(buildPageable(page, size, "title", "asc")));
    }

    @GetMapping("/printbooks")
    @Operation(summary = "List PrintBooks (paginated)")
    public ResponseEntity<Page<BookResponse>> getAllPrintBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getAllPrintBooks(buildPageable(page, size, "title", "asc")));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get inventory statistics")
    public ResponseEntity<InventoryStats> getStats() {
        return ResponseEntity.ok(bookService.getInventoryStats());
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a book")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book by ID")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                "sortBy must be one of: " + String.join(", ", ALLOWED_SORT_FIELDS));
        }

        String normalizedDirection = direction.toLowerCase(Locale.ROOT);
        if (!normalizedDirection.equals("asc") && !normalizedDirection.equals("desc")) {
            throw new IllegalArgumentException("direction must be asc or desc");
        }

        Sort sort = normalizedDirection.equals("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        return PageRequest.of(page, size, sort);
    }
}

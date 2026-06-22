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

import java.util.List;

/**
 * REST API for the Pitso Book Management System.
 *
 * Base path: /api/v1/books
 *
 * Versioning strategy: URI prefix (/v1/) allows a future /v2/ to coexist.
 *
 * Endpoints:
 *  POST   /api/v1/books              → create any book type
 *  GET    /api/v1/books              → list all (paginated)
 *  GET    /api/v1/books/{id}         → get by ID
 *  GET    /api/v1/books/isbn/{isbn}  → get by ISBN
 *  GET    /api/v1/books/search       → search by title/author
 *  GET    /api/v1/books/ebooks       → list all EBooks
 *  GET    /api/v1/books/printbooks   → list all PrintBooks
 *  GET    /api/v1/books/stats        → inventory stats
 *  PATCH  /api/v1/books/{id}         → partial update
 *  DELETE /api/v1/books/{id}         → delete
 */
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Book inventory management API")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a book",
        description = """
            Creates an EBook or PrintBook determined by the ISBN prefix:
            - ISBN starting with '0' → EBook (requires fileSizeKb)
            - ISBN starting with '1' → PrintBook (requires noOfPages + weightGrams)
            """
    )
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all books (paginated)")
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
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

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("title"));
        return ResponseEntity.ok(bookService.searchBooks(q, pageable));
    }

    @GetMapping("/ebooks")
    @Operation(summary = "List all EBooks")
    public ResponseEntity<List<BookResponse>> getAllEBooks() {
        return ResponseEntity.ok(bookService.getAllEBooks());
    }

    @GetMapping("/printbooks")
    @Operation(summary = "List all PrintBooks with weight details")
    public ResponseEntity<List<BookResponse>> getAllPrintBooks() {
        return ResponseEntity.ok(bookService.getAllPrintBooks());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get inventory statistics")
    public ResponseEntity<InventoryStats> getStats() {
        return ResponseEntity.ok(bookService.getInventoryStats());
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a book (title, author, type-specific fields)")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a book by ID")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}

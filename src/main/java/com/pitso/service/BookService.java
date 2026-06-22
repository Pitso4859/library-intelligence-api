package com.pitso.service;

import com.pitso.exception.BookExceptions.*;
import com.pitso.model.*;
import com.pitso.model.BookDtos.*;
import com.pitso.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic layer for the Book system.
 *
 * Scalability notes:
 * - All reads use @Transactional(readOnly=true) for Hibernate optimisations.
 * - ISBN uniqueness is checked before persist (avoids DB roundtrip on conflict).
 * - Page<Book> API supports pagination from day one — no "return all" endpoints.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        // Guard: duplicate ISBN
        if (bookRepository.existsByIsbnNo(request.getIsbnNo())) {
            throw new DuplicateIsbnException(request.getIsbnNo());
        }

        Book book = buildBook(request);
        Book saved = bookRepository.save(book);
        return BookResponse.from(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public Page<BookResponse> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(BookResponse::from);
    }

    public BookResponse getBookById(Long id) {
        return bookRepository.findById(id)
            .map(BookResponse::from)
            .orElseThrow(() -> new BookNotFoundException(id));
    }

    public BookResponse getBookByIsbn(String isbnNo) {
        return bookRepository.findByIsbnNo(isbnNo)
            .map(BookResponse::from)
            .orElseThrow(() -> new BookNotFoundException(isbnNo));
    }

    public Page<BookResponse> searchBooks(String query, Pageable pageable) {
        return bookRepository.searchBooks(query, pageable).map(BookResponse::from);
    }

    public List<BookResponse> getAllEBooks() {
        return bookRepository.findAllEBooks().stream()
            .map(BookResponse::from)
            .toList();
    }

    public List<BookResponse> getAllPrintBooks() {
        return bookRepository.findAllPrintBooks().stream()
            .map(BookResponse::from)
            .toList();
    }

    public InventoryStats getInventoryStats() {
        return new InventoryStats(
            bookRepository.countEBooks(),
            bookRepository.countPrintBooks()
        );
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public BookResponse updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));

        if (request.getTitle() != null) book.setTitle(request.getTitle());
        if (request.getAuthor() != null) book.setAuthor(request.getAuthor());

        if (book instanceof EBook eb) {
            if (request.getFileSizeKb() != null) eb.setFileSizeKb(request.getFileSizeKb());
        } else if (book instanceof PrintBook pb) {
            if (request.getNoOfPages() != null) pb.setNoOfPages(request.getNoOfPages());
            if (request.getWeightGrams() != null) pb.setWeightGrams(request.getWeightGrams());
        }

        return BookResponse.from(bookRepository.save(book));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Determines the concrete type from ISBN prefix and constructs the entity.
     * ISBN starting with '0' → EBook
     * ISBN starting with '1' → PrintBook
     * All other values are rejected by the Book.setIsbnNo() validation before reaching here.
     */
    private Book buildBook(CreateBookRequest req) {
        String isbn = req.getIsbnNo();

        if (isbn.startsWith("0")) {
            if (req.getFileSizeKb() == null || req.getFileSizeKb() < 1) {
                throw new InvalidBookTypeException(
                    "EBook (ISBN starting with '0') requires a valid fileSizeKb (>= 1)");
            }
            return new EBook(req.getTitle(), req.getAuthor(), isbn, req.getFileSizeKb());

        } else if (isbn.startsWith("1")) {
            if (req.getNoOfPages() == null || req.getNoOfPages() < 1) {
                throw new InvalidBookTypeException(
                    "PrintBook (ISBN starting with '1') requires a valid noOfPages (>= 1)");
            }
            if (req.getWeightGrams() == null || req.getWeightGrams() <= 0) {
                throw new InvalidBookTypeException(
                    "PrintBook (ISBN starting with '1') requires a valid weightGrams (> 0)");
            }
            return new PrintBook(req.getTitle(), req.getAuthor(), isbn,
                req.getNoOfPages(), req.getWeightGrams());

        } else {
            // This branch is a safety net; Book.setIsbnNo() will already throw
            throw new InvalidIsbnException("ISBN must start with '0' (EBook) or '1' (PrintBook)");
        }
    }
}

package com.pitso.service;

import com.pitso.exception.BookExceptions.*;
import com.pitso.model.Book;
import com.pitso.model.EBook;
import com.pitso.model.PrintBook;
import com.pitso.model.BookDtos.*;
import com.pitso.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Business logic layer for the Book system.
 *
 * Engineering goals:
 * - controller stays thin and focused on HTTP concerns
 * - write operations are transactional
 * - duplicate ISBN checks are case-insensitive
 * - bulk writes are all-or-nothing
 * - entity construction is centralised in one domain factory method
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
        assertIsbnAvailable(request.getIsbnNo());

        Book saved = bookRepository.save(buildBook(request));
        return BookResponse.from(saved);
    }

    /**
     * Creates up to 100 books in one transaction.
     *
     * The method validates the full payload before writing anything. If a
     * duplicate ISBN or invalid book is found, the transaction fails and the
     * database remains unchanged.
     */
    @Transactional
    public BulkCreateBookResponse createBooksBulk(BulkCreateBookRequest request) {
        List<CreateBookRequest> requests = request.getBooks();
        Set<String> payloadIsbns = new HashSet<>();
        List<Book> entities = new ArrayList<>(requests.size());

        for (CreateBookRequest item : requests) {
            String isbnKey = item.getIsbnNo().toUpperCase(Locale.ROOT);

            if (!payloadIsbns.add(isbnKey)) {
                throw new DuplicateIsbnException(item.getIsbnNo());
            }

            assertIsbnAvailable(item.getIsbnNo());
            entities.add(buildBook(item));
        }

        List<BookResponse> created = bookRepository.saveAll(entities).stream()
            .map(BookResponse::from)
            .toList();

        return new BulkCreateBookResponse(requests.size(), created);
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
        return bookRepository.findByIsbnNoIgnoreCase(isbnNo)
            .map(BookResponse::from)
            .orElseThrow(() -> new BookNotFoundException(isbnNo));
    }

    public Page<BookResponse> searchBooks(String query, Pageable pageable) {
        return bookRepository.searchBooks(query.trim(), pageable).map(BookResponse::from);
    }

    public Page<BookResponse> getAllEBooks(Pageable pageable) {
        return bookRepository.findAllEBooks(pageable).map(BookResponse::from);
    }

    public Page<BookResponse> getAllPrintBooks(Pageable pageable) {
        return bookRepository.findAllPrintBooks(pageable).map(BookResponse::from);
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

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("title must not be blank");
            }
            book.setTitle(title);
        }
        if (request.getAuthor() != null) {
            String author = request.getAuthor().trim();
            if (author.isEmpty()) {
                throw new IllegalArgumentException("author must not be blank");
            }
            book.setAuthor(author);
        }

        if (book instanceof EBook eb) {
            if (request.getFileSizeKb() != null) {
                if (request.getFileSizeKb() < 1) {
                    throw new InvalidBookTypeException("fileSizeKb must be at least 1");
                }
                eb.setFileSizeKb(request.getFileSizeKb());
            }
        } else if (book instanceof PrintBook pb) {
            if (request.getNoOfPages() != null) {
                if (request.getNoOfPages() < 1) {
                    throw new InvalidBookTypeException("noOfPages must be at least 1");
                }
                pb.setNoOfPages(request.getNoOfPages());
            }
            if (request.getWeightGrams() != null) {
                if (request.getWeightGrams() <= 0) {
                    throw new InvalidBookTypeException("weightGrams must be greater than 0");
                }
                pb.setWeightGrams(request.getWeightGrams());
            }
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

    private void assertIsbnAvailable(String isbnNo) {
        if (bookRepository.existsByIsbnNoIgnoreCase(isbnNo)) {
            throw new DuplicateIsbnException(isbnNo);
        }
    }

    /**
     * Determines the concrete type from ISBN prefix and constructs the entity.
     * ISBN starting with '0' → EBook
     * ISBN starting with '1' → PrintBook
     */
    private Book buildBook(CreateBookRequest req) {
        String isbn = req.getIsbnNo();
        String title = req.getTitle().trim();
        String author = req.getAuthor().trim();

        if (isbn.startsWith("0")) {
            if (req.getFileSizeKb() == null || req.getFileSizeKb() < 1) {
                throw new InvalidBookTypeException(
                    "EBook (ISBN starting with '0') requires a valid fileSizeKb (>= 1)");
            }
            return new EBook(title, author, isbn, req.getFileSizeKb());

        } else if (isbn.startsWith("1")) {
            if (req.getNoOfPages() == null || req.getNoOfPages() < 1) {
                throw new InvalidBookTypeException(
                    "PrintBook (ISBN starting with '1') requires a valid noOfPages (>= 1)");
            }
            if (req.getWeightGrams() == null || req.getWeightGrams() <= 0) {
                throw new InvalidBookTypeException(
                    "PrintBook (ISBN starting with '1') requires a valid weightGrams (> 0)");
            }
            return new PrintBook(title, author, isbn,
                req.getNoOfPages(), req.getWeightGrams());
        }

        throw new InvalidIsbnException("ISBN must start with '0' (EBook) or '1' (PrintBook)");
    }
}

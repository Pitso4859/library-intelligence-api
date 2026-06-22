package com.pitso.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Abstract base entity for all book types.
 * Uses JOINED table inheritance strategy for clean schema separation
 * and scalability — each subtype has its own table, no nullable columns.
 *
 * ISBN validation rules (preserved from original):
 *  - Exactly 10 characters
 *  - Starts with '0' (EBook) or '1' (PrintBook)
 *  - First 9 chars must be digits
 *  - Last char is digit, 'B', or 'b'
 */
@Entity
@Table(
    name = "books",
    indexes = {
        @Index(name = "idx_books_isbn", columnList = "isbn_no", unique = true),
        @Index(name = "idx_books_author", columnList = "author"),
        @Index(name = "idx_books_book_type", columnList = "book_type")
    }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "book_type", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
    @SequenceGenerator(name = "book_seq", sequenceName = "books_id_seq", allocationSize = 50)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author cannot exceed 255 characters")
    @Column(name = "author", nullable = false)
    private String author;

    @NotBlank(message = "ISBN is required")
    @Column(name = "isbn_no", nullable = false, unique = true, length = 10)
    private String isbnNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Required by JPA
    protected Book() {}

    protected Book(String title, String author, String isbnNo) {
        this.title = title;
        this.author = author;
        setIsbnNo(isbnNo);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── ISBN Validation (original business rules preserved) ──────────────────

    public void setIsbnNo(String isbnNo) {
        if (isbnNo == null || isbnNo.length() != 10) {
            throw new IllegalArgumentException("The ISBN number must be exactly 10 characters long");
        }
        if (!isbnNo.startsWith("0") && !isbnNo.startsWith("1")) {
            throw new IllegalArgumentException("The ISBN number must start with 0 or 1");
        }
        String first9 = isbnNo.substring(0, 9);
        for (int i = 0; i < first9.length(); i++) {
            if (!Character.isDigit(first9.charAt(i))) {
                throw new IllegalArgumentException("The first nine digits must always be a numerical digit");
            }
        }
        char last = isbnNo.charAt(9);
        if (!Character.isDigit(last) && last != 'b' && last != 'B') {
            throw new IllegalArgumentException("The last character must be a numerical value or either B or b");
        }
        this.isbnNo = isbnNo;
    }

    // ── Abstract contract ────────────────────────────────────────────────────

    /**
     * Returns a human-readable size description.
     * EBook: "EBook: Title, 1024 KB"
     * PrintBook: "Printbook: Title, 320 pages, 450.0g"
     */
    public abstract String getSizeDetails();

    /**
     * Returns the discriminator value used to identify this book type.
     */
    public abstract String getBookType();

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbnNo() { return isbnNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return String.format("Book{id=%d, title='%s', author='%s', isbn='%s', type='%s'}",
            id, title, author, isbnNo, getBookType());
    }
}

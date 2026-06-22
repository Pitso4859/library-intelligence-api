package com.pitso.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

/**
 * DTOs (Data Transfer Objects) for the Book API.
 * Kept in one file for compactness; split into separate files if they grow.
 */
public final class BookDtos {

    private BookDtos() {}

    // ── Request DTOs ─────────────────────────────────────────────────────────

    /**
     * Common fields for creating any book.
     * The ISBN prefix determines the type:
     *   '0' → EBook (requires fileSizeKb)
     *   '1' → PrintBook (requires noOfPages + weightGrams)
     */
    public static class CreateBookRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        private String title;

        @NotBlank(message = "Author is required")
        @Size(max = 255)
        private String author;

        @NotBlank(message = "ISBN is required")
        private String isbnNo;

        // EBook fields (required if ISBN starts with '0')
        private Integer fileSizeKb;

        // PrintBook fields (required if ISBN starts with '1')
        private Integer noOfPages;
        private Float weightGrams;

        // Getters & Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getIsbnNo() { return isbnNo; }
        public void setIsbnNo(String isbnNo) { this.isbnNo = isbnNo; }

        public Integer getFileSizeKb() { return fileSizeKb; }
        public void setFileSizeKb(Integer fileSizeKb) { this.fileSizeKb = fileSizeKb; }

        public Integer getNoOfPages() { return noOfPages; }
        public void setNoOfPages(Integer noOfPages) { this.noOfPages = noOfPages; }

        public Float getWeightGrams() { return weightGrams; }
        public void setWeightGrams(Float weightGrams) { this.weightGrams = weightGrams; }
    }

    public static class UpdateBookRequest {
        @Size(max = 255)
        private String title;

        @Size(max = 255)
        private String author;

        // Type-specific updatable fields
        private Integer fileSizeKb;
        private Integer noOfPages;
        private Float weightGrams;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public Integer getFileSizeKb() { return fileSizeKb; }
        public void setFileSizeKb(Integer fileSizeKb) { this.fileSizeKb = fileSizeKb; }

        public Integer getNoOfPages() { return noOfPages; }
        public void setNoOfPages(Integer noOfPages) { this.noOfPages = noOfPages; }

        public Float getWeightGrams() { return weightGrams; }
        public void setWeightGrams(Float weightGrams) { this.weightGrams = weightGrams; }
    }

    // ── Response DTOs ────────────────────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookResponse {
        private Long id;
        private String bookType;
        private String title;
        private String author;
        private String isbnNo;
        private String sizeDetails;
        private String createdAt;
        private String updatedAt;

        // EBook-only
        private Integer fileSizeKb;

        // PrintBook-only
        private Integer noOfPages;
        private Float weightGrams;

        public static BookResponse from(Book book) {
            BookResponse r = new BookResponse();
            r.id = book.getId();
            r.bookType = book.getBookType();
            r.title = book.getTitle();
            r.author = book.getAuthor();
            r.isbnNo = book.getIsbnNo();
            r.sizeDetails = book.getSizeDetails();
            r.createdAt = book.getCreatedAt() != null ? book.getCreatedAt().toString() : null;
            r.updatedAt = book.getUpdatedAt() != null ? book.getUpdatedAt().toString() : null;

            if (book instanceof EBook eb) {
                r.fileSizeKb = eb.getFileSizeKb();
            } else if (book instanceof PrintBook pb) {
                r.noOfPages = pb.getNoOfPages();
                r.weightGrams = pb.getWeightGrams();
            }
            return r;
        }

        // Getters
        public Long getId() { return id; }
        public String getBookType() { return bookType; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getIsbnNo() { return isbnNo; }
        public String getSizeDetails() { return sizeDetails; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public Integer getFileSizeKb() { return fileSizeKb; }
        public Integer getNoOfPages() { return noOfPages; }
        public Float getWeightGrams() { return weightGrams; }
    }

    /** Inventory statistics response */
    public static class InventoryStats {
        private long totalBooks;
        private long totalEBooks;
        private long totalPrintBooks;

        public InventoryStats(long totalEBooks, long totalPrintBooks) {
            this.totalEBooks = totalEBooks;
            this.totalPrintBooks = totalPrintBooks;
            this.totalBooks = totalEBooks + totalPrintBooks;
        }

        public long getTotalBooks() { return totalBooks; }
        public long getTotalEBooks() { return totalEBooks; }
        public long getTotalPrintBooks() { return totalPrintBooks; }
    }
}

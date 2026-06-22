package com.pitso.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

/**
 * EBook — identified by ISBN starting with '0'.
 * Table: ebooks (joined with books)
 *
 * Scalability note: fileSize stored as INT (KB).
 * For files >2GB, upgrade to BIGINT (fileSizeKb long).
 */
@Entity
@Table(name = "ebooks")
@DiscriminatorValue("EBOOK")
public class EBook extends Book {

    @Min(value = 1, message = "File size must be at least 1 KB")
    @Column(name = "file_size_kb", nullable = false)
    private int fileSizeKb;

    // Required by JPA
    protected EBook() {}

    public EBook(String title, String author, String isbnNo, int fileSizeKb) {
        super(title, author, isbnNo);
        this.fileSizeKb = fileSizeKb;
    }

    @Override
    public String getSizeDetails() {
        return "EBook : " + getTitle() + ", " + fileSizeKb + " KB";
    }

    @Override
    public String getBookType() {
        return "EBOOK";
    }

    public int getFileSizeKb() { return fileSizeKb; }
    public void setFileSizeKb(int fileSizeKb) { this.fileSizeKb = fileSizeKb; }
}

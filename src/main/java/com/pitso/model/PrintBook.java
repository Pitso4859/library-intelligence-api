package com.pitso.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/**
 * PrintBook — identified by ISBN starting with '1'.
 * Table: print_books (joined with books)
 */
@Entity
@Table(name = "print_books")
@DiscriminatorValue("PRINTBOOK")
public class PrintBook extends Book {

    @Min(value = 1, message = "Number of pages must be at least 1")
    @Column(name = "no_of_pages", nullable = false)
    private int noOfPages;

    @DecimalMin(value = "0.1", message = "Weight must be greater than 0")
    @Column(name = "weight_grams", nullable = false)
    private float weightGrams;

    // Required by JPA
    protected PrintBook() {}

    public PrintBook(String title, String author, String isbnNo, int noOfPages, float weightGrams) {
        super(title, author, isbnNo);
        this.noOfPages = noOfPages;
        this.weightGrams = weightGrams;
    }

    @Override
    public String getSizeDetails() {
        return "Printbook : " + getTitle() + ", " + noOfPages + ", " + weightGrams + "g";
    }

    @Override
    public String getBookType() {
        return "PRINTBOOK";
    }

    public int getNoOfPages() { return noOfPages; }
    public void setNoOfPages(int noOfPages) { this.noOfPages = noOfPages; }

    public float getWeightGrams() { return weightGrams; }
    public void setWeightGrams(float weightGrams) { this.weightGrams = weightGrams; }
}

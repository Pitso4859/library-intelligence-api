package com.pitso.ui.model;

public record Book(
        long id,
        String bookType,
        String title,
        String author,
        String isbnNo,
        String sizeDetails,
        String createdAt,
        String updatedAt,
        Integer fileSizeKb,
        Integer noOfPages,
        Double weightGrams) {

    public boolean isEBook() {
        return "EBOOK".equalsIgnoreCase(bookType);
    }

    public boolean isPrintBook() {
        return "PRINTBOOK".equalsIgnoreCase(bookType);
    }
}

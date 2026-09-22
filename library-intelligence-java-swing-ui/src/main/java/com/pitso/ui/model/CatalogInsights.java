package com.pitso.ui.model;

import java.util.List;

public record CatalogInsights(
        long totalBooks,
        long totalEBooks,
        long totalPrintBooks,
        long uniqueAuthors,
        double averageEBookSizeKb,
        double averagePrintPages,
        double averagePrintWeightGrams,
        List<AuthorInsight> topAuthors,
        List<Book> newestBooks) {

    public record AuthorInsight(String author, long bookCount) {
    }
}

package com.pitso.ui.model;

import java.util.List;

public record Recommendation(
        long id,
        String title,
        String author,
        String isbnNo,
        String bookType,
        int score,
        List<String> reasons) {
}

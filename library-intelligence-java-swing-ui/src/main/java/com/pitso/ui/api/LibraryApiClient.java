package com.pitso.ui.api;

import com.pitso.ui.model.Book;
import com.pitso.ui.model.CatalogInsights;
import com.pitso.ui.model.InventoryStats;
import com.pitso.ui.model.Recommendation;
import com.pitso.ui.util.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LibraryApiClient {
    private final HttpClient client;
    private volatile String baseUrl;

    public LibraryApiClient(String baseUrl) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        setBaseUrl(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public final void setBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException("API URL cannot be blank");
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        this.baseUrl = normalized;
    }

    public InventoryStats getStats() {
        Map<String, Object> json = asObject(request("GET", "/api/v1/books/stats", null));
        return new InventoryStats(
                longValue(json.get("totalBooks")),
                longValue(json.get("totalEBooks")),
                longValue(json.get("totalPrintBooks")));
    }

    public List<Book> getBooks() {
        return parseBooksPage(request("GET", "/api/v1/books?page=0&size=100&sortBy=title&direction=asc", null));
    }

    public List<Book> searchBooks(String query) {
        if (query == null || query.isBlank()) return getBooks();
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        return parseBooksPage(request("GET", "/api/v1/books/search?q=" + encoded + "&page=0&size=100", null));
    }

    public Book createBook(Map<String, Object> payload) {
        return parseBook(asObject(request("POST", "/api/v1/books", SimpleJson.stringify(payload))));
    }

    public Book updateBook(long id, Map<String, Object> payload) {
        return parseBook(asObject(request("PATCH", "/api/v1/books/" + id, SimpleJson.stringify(payload))));
    }

    public void deleteBook(long id) {
        request("DELETE", "/api/v1/books/" + id, null);
    }

    public List<Recommendation> getRecommendations(String query, String preferredType, int limit) {
        String q = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8);
        String type = URLEncoder.encode(preferredType == null ? "ANY" : preferredType, StandardCharsets.UTF_8);
        Object parsed = request("GET", "/api/v1/catalog/recommendations?q=" + q + "&preferredType=" + type + "&limit=" + limit, null);
        List<Object> list = asArray(parsed);
        List<Recommendation> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> row = asObject(item);
            List<String> reasons = new ArrayList<>();
            Object reasonsRaw = row.get("reasons");
            if (reasonsRaw instanceof List<?> values) {
                for (Object reason : values) reasons.add(stringValue(reason));
            }
            result.add(new Recommendation(
                    longValue(row.get("id")),
                    stringValue(row.get("title")),
                    stringValue(row.get("author")),
                    stringValue(row.get("isbnNo")),
                    stringValue(row.get("bookType")),
                    intValue(row.get("score")),
                    reasons));
        }
        return result;
    }

    public CatalogInsights getInsights() {
        Map<String, Object> json = asObject(request("GET", "/api/v1/catalog/insights", null));
        List<CatalogInsights.AuthorInsight> authors = new ArrayList<>();
        if (json.get("topAuthors") instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> row = asObject(item);
                authors.add(new CatalogInsights.AuthorInsight(
                        stringValue(row.get("author")), longValue(row.get("bookCount"))));
            }
        }
        List<Book> newest = new ArrayList<>();
        if (json.get("newestBooks") instanceof List<?> list) {
            for (Object item : list) newest.add(parseBook(asObject(item)));
        }
        return new CatalogInsights(
                longValue(json.get("totalBooks")),
                longValue(json.get("totalEBooks")),
                longValue(json.get("totalPrintBooks")),
                longValue(json.get("uniqueAuthors")),
                doubleValue(json.get("averageEBookSizeKb")),
                doubleValue(json.get("averagePrintPages")),
                doubleValue(json.get("averagePrintWeightGrams")),
                authors,
                newest);
    }

    private Object request(String method, String path, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json");

            switch (method) {
                case "POST" -> builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "{}" : jsonBody));
                case "PATCH" -> builder.header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody == null ? "{}" : jsonBody));
                case "DELETE" -> builder.DELETE();
                default -> builder.GET();
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            String body = response.body() == null ? "" : response.body().trim();
            if (code < 200 || code >= 300) {
                throw new ApiException(code, extractApiMessage(body, "Request failed with HTTP " + code));
            }
            if (body.isEmpty()) return Collections.emptyMap();
            try {
                return SimpleJson.parse(body);
            } catch (RuntimeException ex) {
                throw new ApiException(code, "The server returned data that is not valid JSON.");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException(0, "Could not connect to " + baseUrl + ". Check that the API is running and your internet connection is available.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "The API request was interrupted.");
        } catch (IllegalArgumentException ex) {
            throw new ApiException(0, "Invalid API URL: " + baseUrl);
        }
    }

    private String extractApiMessage(String body, String fallback) {
        if (body == null || body.isBlank()) return fallback;
        try {
            Object parsed = SimpleJson.parse(body);
            if (parsed instanceof Map<?, ?> map) {
                Object message = map.get("message");
                if (message != null && !String.valueOf(message).isBlank()) return String.valueOf(message);
                Object error = map.get("error");
                if (error != null && !String.valueOf(error).isBlank()) return String.valueOf(error);
            }
        } catch (RuntimeException ignored) {
        }
        return body.length() > 350 ? body.substring(0, 350) + "..." : body;
    }

    private List<Book> parseBooksPage(Object raw) {
        Map<String, Object> page = asObject(raw);
        Object contentRaw = page.get("content");
        if (!(contentRaw instanceof List<?> content)) return List.of();
        List<Book> books = new ArrayList<>();
        for (Object item : content) books.add(parseBook(asObject(item)));
        return books;
    }

    private Book parseBook(Map<String, Object> json) {
        return new Book(
                longValue(json.get("id")),
                stringValue(json.get("bookType")),
                stringValue(json.get("title")),
                stringValue(json.get("author")),
                stringValue(json.get("isbnNo")),
                stringValue(json.get("sizeDetails")),
                nullableString(json.get("createdAt")),
                nullableString(json.get("updatedAt")),
                nullableInt(json.get("fileSizeKb")),
                nullableInt(json.get("noOfPages")),
                nullableDouble(json.get("weightGrams")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new ApiException(0, "Unexpected API response format.");
    }

    @SuppressWarnings("unchecked")
    private List<Object> asArray(Object value) {
        if (value instanceof List<?> list) return (List<Object>) list;
        throw new ApiException(0, "Unexpected API response format.");
    }

    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value); }
    private String nullableString(Object value) { return value == null ? null : String.valueOf(value); }
    private long longValue(Object value) { return value instanceof Number n ? n.longValue() : 0L; }
    private int intValue(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    private double doubleValue(Object value) { return value instanceof Number n ? n.doubleValue() : 0.0; }
    private Integer nullableInt(Object value) { return value instanceof Number n ? n.intValue() : null; }
    private Double nullableDouble(Object value) { return value instanceof Number n ? n.doubleValue() : null; }
}

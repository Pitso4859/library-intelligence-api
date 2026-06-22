package com.pitso.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Domain-specific exceptions.
 * Each maps to an HTTP status code via @ResponseStatus,
 * handled centrally by GlobalExceptionHandler.
 */
public final class BookExceptions {

    private BookExceptions() {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class BookNotFoundException extends RuntimeException {
        public BookNotFoundException(Long id) {
            super("Book not found with id: " + id);
        }
        public BookNotFoundException(String isbnNo) {
            super("Book not found with ISBN: " + isbnNo);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateIsbnException extends RuntimeException {
        public DuplicateIsbnException(String isbnNo) {
            super("A book with ISBN '" + isbnNo + "' already exists");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidBookTypeException extends RuntimeException {
        public InvalidBookTypeException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidIsbnException extends RuntimeException {
        public InvalidIsbnException(String message) {
            super(message);
        }
    }
}

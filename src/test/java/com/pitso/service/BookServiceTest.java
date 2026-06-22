package com.pitso.service;

import com.pitso.exception.BookExceptions.*;
import com.pitso.model.*;
import com.pitso.model.BookDtos.*;
import com.pitso.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService unit tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private CreateBookRequest ebookRequest;
    private CreateBookRequest printBookRequest;

    @BeforeEach
    void setUp() {
        ebookRequest = new CreateBookRequest();
        ebookRequest.setTitle("Clean Code");
        ebookRequest.setAuthor("Robert C. Martin");
        ebookRequest.setIsbnNo("032156840b");
        ebookRequest.setFileSizeKb(3500);

        printBookRequest = new CreateBookRequest();
        printBookRequest.setTitle("Refactoring");
        printBookRequest.setAuthor("Martin Fowler");
        printBookRequest.setIsbnNo("1198734561B");
        printBookRequest.setNoOfPages(448);
        printBookRequest.setWeightGrams(680.5f);
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBook: EBook is created when ISBN starts with '0'")
    void createEBook_success() {
        when(bookRepository.existsByIsbnNo("032156840b")).thenReturn(false);
        EBook saved = new EBook("Clean Code", "Robert C. Martin", "032156840b", 3500);
        when(bookRepository.save(any(EBook.class))).thenReturn(saved);

        BookResponse result = bookService.createBook(ebookRequest);

        assertNotNull(result);
        assertEquals("EBOOK", result.getBookType());
        assertEquals(3500, result.getFileSizeKb());
        verify(bookRepository).save(any(EBook.class));
    }

    @Test
    @DisplayName("createBook: PrintBook is created when ISBN starts with '1'")
    void createPrintBook_success() {
        when(bookRepository.existsByIsbnNo("1198734561B")).thenReturn(false);
        PrintBook saved = new PrintBook("Refactoring", "Martin Fowler", "1198734561B", 448, 680.5f);
        when(bookRepository.save(any(PrintBook.class))).thenReturn(saved);

        BookResponse result = bookService.createBook(printBookRequest);

        assertEquals("PRINTBOOK", result.getBookType());
        assertEquals(448, result.getNoOfPages());
    }

    @Test
    @DisplayName("createBook: throws DuplicateIsbnException for existing ISBN")
    void createBook_duplicateIsbn() {
        when(bookRepository.existsByIsbnNo("032156840b")).thenReturn(true);
        assertThrows(DuplicateIsbnException.class, () -> bookService.createBook(ebookRequest));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBook: EBook without fileSizeKb throws InvalidBookTypeException")
    void createEBook_missingFileSize() {
        when(bookRepository.existsByIsbnNo("032156840b")).thenReturn(false);
        ebookRequest.setFileSizeKb(null);
        assertThrows(InvalidBookTypeException.class, () -> bookService.createBook(ebookRequest));
    }

    @Test
    @DisplayName("createBook: PrintBook without noOfPages throws InvalidBookTypeException")
    void createPrintBook_missingPages() {
        when(bookRepository.existsByIsbnNo("1198734561B")).thenReturn(false);
        printBookRequest.setNoOfPages(null);
        assertThrows(InvalidBookTypeException.class, () -> bookService.createBook(printBookRequest));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBookById: throws BookNotFoundException for unknown id")
    void getBookById_notFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(99L));
    }

    @Test
    @DisplayName("getBookByIsbn: returns book when ISBN exists")
    void getBookByIsbn_found() {
        EBook book = new EBook("Clean Code", "Robert C. Martin", "032156840b", 3500);
        when(bookRepository.findByIsbnNo("032156840b")).thenReturn(Optional.of(book));

        BookResponse response = bookService.getBookByIsbn("032156840b");
        assertEquals("Clean Code", response.getTitle());
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBook: throws BookNotFoundException when book does not exist")
    void deleteBook_notFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);
        assertThrows(BookNotFoundException.class, () -> bookService.deleteBook(99L));
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteBook: succeeds when book exists")
    void deleteBook_success() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> bookService.deleteBook(1L));
        verify(bookRepository).deleteById(1L);
    }
}

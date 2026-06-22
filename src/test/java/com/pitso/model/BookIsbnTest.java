package com.pitso.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Book ISBN validation.
 * Mirrors the original IsbnTestClass exactly — all original test cases preserved.
 */
@DisplayName("Book ISBN Validation")
class BookIsbnTest {

    // Use EBook as concrete instance (same validation path for all Book subclasses)
    private final Book book = new EBook("The Picture of Dorian Gray", "Oscar Wilde", "1111111111", 500);

    // ── Valid ISBNs (original test cases) ────────────────────────────────────

    @Test
    @DisplayName("ISBN '067001617B' is valid (EBook, ends with B)")
    void testSetIsbn_067001617B() {
        book.setIsbnNo("067001617B");
        assertEquals("067001617B", book.getIsbnNo());
    }

    @Test
    @DisplayName("ISBN '1367823245' is valid (PrintBook, all digits)")
    void testSetIsbn_1367823245() {
        book.setIsbnNo("1367823245");
        assertEquals("1367823245", book.getIsbnNo());
    }

    @Test
    @DisplayName("ISBN '198734561B' is valid (PrintBook, ends with B)")
    void testSetIsbn_198734561B() {
        book.setIsbnNo("198734561B");
        assertEquals("198734561B", book.getIsbnNo());
    }

    @Test
    @DisplayName("ISBN '032156840b' is valid (EBook, ends with lowercase b)")
    void testSetIsbn_032156840b() {
        book.setIsbnNo("032156840b");
        assertEquals("032156840b", book.getIsbnNo());
    }

    // ── Invalid ISBNs (original test cases) ──────────────────────────────────

    @Test
    @DisplayName("ISBN '192156844' is invalid — only 9 digits")
    void testSetIsbn_tooShort() {
        assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo("192156844"));
    }

    @Test
    @DisplayName("ISBN '032156B840' is invalid — B in position 7 (not digit)")
    void testSetIsbn_nonDigitInFirst9() {
        assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo("032156B840"));
    }

    @Test
    @DisplayName("ISBN '198734561K' is invalid — last char is K")
    void testSetIsbn_invalidLastChar() {
        assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo("198734561K"));
    }

    // ── Additional edge cases ────────────────────────────────────────────────

    @Nested
    @DisplayName("ISBN length validation")
    class LengthValidation {

        @Test
        void tooLong_isRejected() {
            assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo("01234567890"));
        }

        @Test
        void empty_isRejected() {
            assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo(""));
        }

        @Test
        void nullIsbn_isRejected() {
            assertThrows(Exception.class, () -> book.setIsbnNo(null));
        }
    }

    @Nested
    @DisplayName("ISBN prefix validation")
    class PrefixValidation {

        @Test
        void startsWith2_isRejected() {
            assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo("2123456789"));
        }

        @Test
        void startsWith9_isRejected() {
            assertThrows(IllegalArgumentException.class, () -> book.setIsbnNo("9123456789"));
        }
    }
}

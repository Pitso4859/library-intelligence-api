-- =============================================
-- V1__Initial_Schema.sql
-- Pitso Book Management System - Initial Schema
-- =============================================

-- Sequence for book IDs (PostgreSQL)
CREATE SEQUENCE IF NOT EXISTS books_id_seq
    START WITH 1
    INCREMENT BY 50
    NO MAXVALUE
    CACHE 1;

-- Base books table (shared columns for all book types)
CREATE TABLE IF NOT EXISTS books (
    id          BIGINT PRIMARY KEY DEFAULT nextval('books_id_seq'),
    book_type   VARCHAR(10)  NOT NULL,      -- discriminator: EBOOK | PRINTBOOK
    title       VARCHAR(255) NOT NULL,
    author      VARCHAR(255) NOT NULL,
    isbn_no     VARCHAR(10)  NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- EBook-specific columns (joined table)
CREATE TABLE IF NOT EXISTS ebooks (
    id           BIGINT PRIMARY KEY REFERENCES books(id) ON DELETE CASCADE,
    file_size_kb INT NOT NULL CHECK (file_size_kb >= 1)
);

-- PrintBook-specific columns (joined table)
CREATE TABLE IF NOT EXISTS print_books (
    id           BIGINT PRIMARY KEY REFERENCES books(id) ON DELETE CASCADE,
    no_of_pages  INT   NOT NULL CHECK (no_of_pages >= 1),
    weight_grams FLOAT NOT NULL CHECK (weight_grams > 0)
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_books_isbn     ON books(isbn_no);
CREATE INDEX IF NOT EXISTS idx_books_author   ON books(author);
CREATE INDEX IF NOT EXISTS idx_books_book_type ON books(book_type);
CREATE INDEX IF NOT EXISTS idx_books_title    ON books(title);

-- Auto-update updated_at on row change (PostgreSQL trigger)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
